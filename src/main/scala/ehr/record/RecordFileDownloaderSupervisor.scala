package ehr.record

import akka.actor.ActorRef
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.scaladsl.Behaviors.{same, stopped}
import akka.actor.typed.{Behavior, Terminated}
import ehr.record.RecordFileDownloader.DownloadFile
import scorex.core.utils.ScorexLogging

@SuppressWarnings(Array("org.wartremover.warts.Nothing"))
object RecordFileDownloaderSupervisor extends ScorexLogging {

  final case class DownloadFiles(files: Seq[FileHash])

  def behavior(fileStorage: RecordFileStorage,
               peerManager: ActorRef): Behavior[DownloadFiles] =
    Behaviors.immutable[DownloadFiles] { (ctx, msg) =>
      msg match {
        case DownloadFiles(files) =>
          files.foreach { hash =>
            val downloader = ctx.spawn(RecordFileDownloader.behavior(fileStorage, peerManager),
              "RecordFileDownloader")
            ctx.watch(downloader)
            downloader ! DownloadFile(hash)
          }
          if (ctx.children.isEmpty) stopped else same
      }
    } onSignal {
      case (ctx, Terminated(ref)) =>
        log.info(s"${ctx.self}: $ref is terminated")
        if (ctx.children.isEmpty) stopped else same
    }
}
