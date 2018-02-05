package com.ehrchain

import scorex.core.utils.ScorexLogging

class EhrchainApp(val settingsFilename: String) extends ScorexLogging {

  def run(): Unit = log.info("Starting...")
}

object EhrchainApp extends App {
  val settingsFilename: String = args.headOption.getOrElse("settings.conf")
  new EhrchainApp(settingsFilename).run()
}

