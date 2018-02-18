package com.ehrchain

import akka.actor.{ActorRef, Props}
import com.ehrchain.block.EhrBlock
import com.ehrchain.history.{EhrBlockStream, EhrSyncInfo, EhrSyncInfoMessageSpec}
import com.ehrchain.mining.EhrMiner
import com.ehrchain.settings.EhrAppSettings
import com.ehrchain.transaction.EhrTransaction
import scorex.core.api.http.{ApiRoute, NodeViewApiRoute, PeersApiRoute, UtilsApiRoute}
import scorex.core.app.Application
import scorex.core.network.NodeViewSynchronizer
import scorex.core.network.message.MessageSpec
import scorex.core.settings.ScorexSettings
import scorex.core.transaction.box.proposition.PublicKey25519Proposition
import scorex.core.utils.ScorexLogging

import scala.io.Source

class EhrApp(val settingsFilename: String) extends Application {

  override type P = PublicKey25519Proposition
  override type TX = EhrTransaction
  override type PMOD = EhrBlock
  override type NVHT = EhrNodeViewHolder

  private val ehrAppSettings = EhrAppSettings.read(Some(settingsFilename))
  implicit override val settings: ScorexSettings = ehrAppSettings.scorexSettings

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

//  if (settings.network.nodeName.startsWith("generatorNode")) {
//    log.info("Starting transactions generation")
//    val generator: ActorRef = actorSystem.actorOf(Props(new SimpleBoxTransactionGenerator(nodeViewHolderRef)))
//    generator ! StartGeneration(10 seconds)
//  }
}

object EhrApp extends App {
  val settingsFilename: String = args.headOption.getOrElse("settings.conf")
  new EhrApp(settingsFilename).run()
}
