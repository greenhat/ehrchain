package ehr.settings

import com.typesafe.config.Config
import net.ceedubs.ficus.Ficus._
import net.ceedubs.ficus.readers.ArbitraryTypeReader._
import net.ceedubs.ficus.readers.ValueReader
import scorex.core.settings.{ScorexSettings, SettingsReaders}
import scorex.core.utils.ScorexLogging

final case class EhrAppSettings(scorexSettings: ScorexSettings)

object EhrAppSettings extends ScorexLogging with SettingsReaders {
  def read(userConfigPath: Option[String]): EhrAppSettings = {
    fromConfig(ScorexSettings.readConfigFromPath(userConfigPath, "scorex"))
  }

  implicit val networkSettingsValueReader: ValueReader[EhrAppSettings] =
    (cfg: Config, path: String) => fromConfig(cfg.getConfig(path))

  private def fromConfig(config: Config): EhrAppSettings = {
    log.info(s"loading app settings from config: $config")
    val scorexSettings = config.as[ScorexSettings]("scorex")
    EhrAppSettings(scorexSettings)
  }
}
