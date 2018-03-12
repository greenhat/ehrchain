package ehr

import akka.actor.{ActorRef, Props}
import ehr.block.EhrBlock
import ehr.history.{EhrBlockStream, EhrSyncInfo, EhrSyncInfoMessageSpec}
import ehr.mining.EhrMiner
import ehr.settings.EhrAppSettings
import ehr.transaction.EhrTransaction
import ehr.wallet.EhrTransactionGenerator
import ehr.wallet.EhrTransactionGenerator.StartGeneration
import scorex.core.api.http.{ApiRoute, NodeViewApiRoute, PeersApiRoute, UtilsApiRoute}
import scorex.core.app.Application
import scorex.core.network.NodeViewSynchronizer
import scorex.core.network.message.MessageSpec
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

  override val nodeViewHolderRef: ActorRef = actorSystem.actorOf(EhrNodeViewHolder.props)

  override val apiRoutes: Seq[ApiRoute] = Seq[ApiRoute](
    UtilsApiRoute(settings.restApi),
    NodeViewApiRoute[P, TX](settings.restApi, nodeViewHolderRef),
    PeersApiRoute(peerManagerRef, networkControllerRef, settings.restApi)
  )

  // todo generate
  override val swaggerConfig: String = Source.fromResource("api/testApi.yaml").getLines.mkString("\n")

  val miner: ActorRef = actorSystem.actorOf(EhrMiner.props(nodeViewHolderRef))

  override val localInterface: ActorRef =
    actorSystem.actorOf(EhrLocalInterface.props(nodeViewHolderRef, miner))

  override val nodeViewSynchronizer: ActorRef =
    actorSystem.actorOf(Props(
      new NodeViewSynchronizer[P, TX, EhrSyncInfo, EhrSyncInfoMessageSpec.type, PMOD, EhrBlockStream, EhrTransactionMemPool]
    (networkControllerRef, nodeViewHolderRef, localInterface, EhrSyncInfoMessageSpec, settings.network, timeProvider)))

  log.debug("Starting transactions generation")
  val transactionGenerator: ActorRef = actorSystem.actorOf(EhrTransactionGenerator.props(nodeViewHolderRef))
  transactionGenerator ! StartGeneration(2 seconds)
}

object EhrApp extends App {
  val settingsFilename: String = args.headOption.getOrElse("settings.conf")
  new EhrApp(settingsFilename).run()
}

