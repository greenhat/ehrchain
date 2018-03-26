package ehr

import akka.actor.{ActorRef, Props}
import ehr.api.http.FileApiRoute
import ehr.block.EhrBlock
import ehr.history.{BlockStream, EhrSyncInfo, EhrSyncInfoMessageSpec}
import ehr.mining.Miner
import ehr.record.InMemoryRecordFileStorage
import ehr.settings.EhrAppSettings
import ehr.transaction.EhrTransaction
import ehr.wallet.TransactionGenerator
import ehr.wallet.TransactionGenerator.StartGeneration
import scorex.core.api.http.{ApiRoute, NodeViewApiRoute, PeersApiRoute, UtilsApiRoute}
import scorex.core.app.Application
import scorex.core.network.NodeViewSynchronizer
import scorex.core.network.message.MessageSpec
import scorex.core.serialization.SerializerRegistry
import scorex.core.serialization.SerializerRegistry.SerializerRecord
import scorex.core.settings.ScorexSettings
import scorex.core.transaction.box.proposition.PublicKey25519Proposition

import scala.concurrent.duration._
import scala.io.Source

class EhrApp(val settingsFilename: String) extends Application {

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
    FileApiRoute(settings.restApi, recordFileStorage, nodeViewHolderRef)
  )

  // todo generate
  override val swaggerConfig: String = Source.fromResource("api/testApi.yaml").getLines.mkString("\n")

  val miner: ActorRef = actorSystem.actorOf(Miner.props(nodeViewHolderRef))

  override val localInterface: ActorRef =
    actorSystem.actorOf(EhrLocalInterface.props(nodeViewHolderRef, miner))

  override val nodeViewSynchronizer: ActorRef =
    actorSystem.actorOf(Props(
      new NodeViewSynchronizer[P, TX, EhrSyncInfo, EhrSyncInfoMessageSpec.type, PMOD, BlockStream, TransactionMemPool]
    (networkControllerRef, nodeViewHolderRef, localInterface, EhrSyncInfoMessageSpec, settings.network, timeProvider)))

  log.debug("Starting transactions generation")
  val transactionGenerator: ActorRef = actorSystem.actorOf(TransactionGenerator.props(nodeViewHolderRef))
  transactionGenerator ! StartGeneration(2 seconds)
}

object EhrApp extends App {
  val settingsFilename: String = args.headOption.getOrElse("settings.conf")
  new EhrApp(settingsFilename).run()
}

