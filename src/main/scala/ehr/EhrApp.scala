package ehr

import akka.actor.{ActorRef, Props}
import ehr.api.http.FileApiRoute
import ehr.block.EhrBlock
import ehr.demo.TypedActorWrapper.Call
import ehr.demo.{PatientTransactionGenerator, ProviderATransactionGenerator, TypedActorWrapper}
import ehr.history.{BlockStream, EhrSyncInfo, EhrSyncInfoMessageSpec}
import ehr.mempool.TransactionMemPool
import ehr.mining.Miner
import ehr.record.{InMemoryRecordFileStorage, RecordFileDownloaderSupervisor}
import ehr.settings.EhrAppSettings
import ehr.transaction.EhrTransaction
import scorex.core.api.http.{ApiRoute, NodeViewApiRoute, PeersApiRoute, UtilsApiRoute}
import scorex.core.app.Application
import scorex.core.network.NodeViewSynchronizer
import scorex.core.network.message.MessageSpec
import scorex.core.serialization.SerializerRegistry
import scorex.core.serialization.SerializerRegistry.SerializerRecord
import scorex.core.settings.ScorexSettings
import scorex.core.transaction.box.proposition.PublicKey25519Proposition

import scala.io.Source

@SuppressWarnings(Array("org.wartremover.warts.Throw"))
class EhrApp(val settingsFilename: String,
             roleName: String) extends Application {

  override type P = PublicKey25519Proposition
  override type TX = EhrTransaction
  override type PMOD = EhrBlock
  override type NVHT = EhrNodeViewHolder

  implicit override lazy val settings: ScorexSettings =
    EhrAppSettings.read(Some(settingsFilename)).scorexSettings

  log.debug(s"Starting application with settings \n$settings")

  override protected lazy val additionalMessageSpecs: Seq[MessageSpec[_]] = Seq(EhrSyncInfoMessageSpec)

  private val recordFileStorage = new InMemoryRecordFileStorage()

  override val nodeViewHolderRef: ActorRef =
    actorSystem.actorOf(EhrNodeViewHolder.props(recordFileStorage))

  implicit val serializerReg: SerializerRegistry = SerializerRegistry(Seq(SerializerRecord(EhrTransaction.jsonEncoder)))

  override val apiRoutes: Seq[ApiRoute] = Seq[ApiRoute](
    UtilsApiRoute(settings.restApi),
    NodeViewApiRoute[P, TX](settings.restApi, nodeViewHolderRef),
    PeersApiRoute(peerManagerRef, networkControllerRef, settings.restApi),
    FileApiRoute(settings.restApi, recordFileStorage)
  )

  override val swaggerConfig: String = Source.fromResource("api/testApi.yaml").getLines.mkString("\n")

  val miner: ActorRef = actorSystem.actorOf(Miner.props(nodeViewHolderRef))

  override val localInterface: ActorRef =
    actorSystem.actorOf(EhrLocalInterface.props(nodeViewHolderRef,
      miner,
      RecordFileDownloaderSupervisor.behavior(recordFileStorage, peerManagerRef),
      recordFileStorage
      ))

  override val nodeViewSynchronizer: ActorRef =
    actorSystem.actorOf(Props(
      new NodeViewSynchronizer[P, TX, EhrSyncInfo, EhrSyncInfoMessageSpec.type, PMOD, BlockStream, TransactionMemPool]
    (networkControllerRef, nodeViewHolderRef, localInterface, EhrSyncInfoMessageSpec, settings.network, timeProvider)))

  log.debug("Starting transactions generation")
  val transactionGenerator: ActorRef = roleName match {
    case "patient" => actorSystem.actorOf(TypedActorWrapper.props(nodeViewHolderRef,
        PatientTransactionGenerator.behavior(nodeViewHolderRef)) )
    case "providerA" => actorSystem.actorOf(TypedActorWrapper.props(nodeViewHolderRef,
      ProviderATransactionGenerator.behavior(nodeViewHolderRef, recordFileStorage)) )
    case "providerB" => actorSystem.actorOf(TypedActorWrapper.props(nodeViewHolderRef,
      PatientTransactionGenerator.behavior(nodeViewHolderRef)) )
    case _ => throw new IllegalArgumentException(s"unsupported role: $roleName")
  }

  transactionGenerator ! Call
}

object EhrApp extends App {
  val settingsFilename: String = args.headOption.getOrElse("settings.conf")
  val roleName: String = args.lastOption.getOrElse("patient")
  new EhrApp(settingsFilename, roleName).run()
}

