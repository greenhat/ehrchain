package com.ehrchain

import akka.actor.{ActorRef, Props}
import com.ehrchain.block.EhrBlock
import com.ehrchain.history.{EhrBlockStream, EhrSyncInfo, EhrSyncInfoMessageSpec}
import com.ehrchain.transaction.EhrTransaction
import examples.hybrid.mining.HybridSettings
import scorex.core.api.http.{ApiRoute, NodeViewApiRoute, PeersApiRoute, UtilsApiRoute}
import scorex.core.app.Application
import scorex.core.network.NodeViewSynchronizer
import scorex.core.network.message.MessageSpec
import scorex.core.transaction.box.proposition.PublicKey25519Proposition
import scorex.core.utils.ScorexLogging

class EhrchainApp(val settingsFilename: String) extends Application {

  override type P = PublicKey25519Proposition
  override type TX = EhrTransaction
  override type PMOD = EhrBlock
  override type NVHT = EhrNodeViewHolder

//  private val hybridSettings = HybridSettings.read(Some(settingsFilename))
//  implicit override lazy val settings: ScorexSettings = HybridSettings.read(Some(settingsFilename)).scorexSettings

//  log.debug(s"Starting application with settings \n$settings")

  override protected lazy val additionalMessageSpecs: Seq[MessageSpec[_]] = Seq(EhrSyncInfoMessageSpec)

  // todo use props
  override val nodeViewHolderRef: ActorRef = actorSystem.actorOf(Props(new EhrNodeViewHolder()))

  override val apiRoutes: Seq[ApiRoute] = Seq(
    UtilsApiRoute(settings.restApi),
    NodeViewApiRoute[P, TX](settings.restApi, nodeViewHolderRef),
    PeersApiRoute(peerManagerRef, networkControllerRef, settings.restApi)
  )

//  override val swaggerConfig: String = Source.fromResource("api/testApi.yaml").getLines.mkString("\n")
//
//  val miner = actorSystem.actorOf(Props(new PowMiner(nodeViewHolderRef, hybridSettings.mining)))
//
  // todo use props
  override val localInterface: ActorRef = actorSystem.actorOf(Props(new EhrLocalInterface(nodeViewHolderRef, miner)))

  override val nodeViewSynchronizer: ActorRef =
    actorSystem.actorOf(Props(
      new NodeViewSynchronizer[P, TX, EhrSyncInfo, EhrSyncInfoMessageSpec.type, PMOD, EhrBlockStream, EhrTransactionMemPool]
    (networkControllerRef, nodeViewHolderRef, localInterface, EhrSyncInfoMessageSpec, settings.network, timeProvider)))

  //touching lazy vals
  miner
  localInterface
  nodeViewSynchronizer

//  if (settings.network.nodeName.startsWith("generatorNode")) {
//    log.info("Starting transactions generation")
//    val generator: ActorRef = actorSystem.actorOf(Props(new SimpleBoxTransactionGenerator(nodeViewHolderRef)))
//    generator ! StartGeneration(10 seconds)
//  }
}

object EhrchainApp extends App {
  val settingsFilename: String = args.headOption.getOrElse("settings.conf")
  new EhrchainApp(settingsFilename).run()
}

