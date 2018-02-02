package com.ehrchain

import scorex.core.utils.ScorexLogging

class EhrchainApp(val settingsFilename: String) extends ScorexLogging {

  def run() = log.info("Starting...")
}

object EhrchainApp extends App {
  val settingsFilename = args.headOption.getOrElse("settings.conf")
  new EhrchainApp(settingsFilename).run()
}

