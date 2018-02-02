package com.ehrchain

import akka.actor.{ActorRef, Props}
import scorex.core.api.http.{ApiRoute, NodeViewApiRoute, PeersApiRoute, UtilsApiRoute}
import scorex.core.app.Application
import scorex.core.network.NodeViewSynchronizer
import scorex.core.network.message.MessageSpec
import scorex.core.settings.ScorexSettings
import scorex.core.transaction.box.proposition.PublicKey25519Proposition
import scorex.core.utils.ScorexLogging

import scala.io.Source

class EhrchainApp(val settingsFilename: String) extends ScorexLogging {

  def run() = log.info("Starting...")
}

object EhrchainApp extends App {
  val settingsFilename = args.headOption.getOrElse("settings.conf")
  new EhrchainApp(settingsFilename).run()
}

