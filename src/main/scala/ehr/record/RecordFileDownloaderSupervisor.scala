package ehr.record

import akka.actor.ActorRef
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.scaladsl.Behaviors.{same, stopped}
import akka.actor.typed.{Behavior, Terminated}
import ehr.record.RecordFileDownloader.DownloadFile
import scorex.core.utils.ScorexLogging

@SuppressWarnings(Array("org.wartremover.warts.Nothing"))
object RecordFileDownloaderSupervisor extends ScorexLogging {

  sealed trait Command
  final case class DownloadFiles(files: Seq[FileHash]) extends Command

  final case class DownloadSucceeded(fileHash: FileHash) extends Command

  sealed trait FailureReason
  final case object NoPeers extends FailureReason
  final case class DownloadErrors(errors: Seq[Throwable]) extends FailureReason
  final case class DownloadFailed(fileHash: FileHash, reason: FailureReason) extends Command

  def behavior(fileStorage: RecordFileStorage,
               peerManager: ActorRef): Behavior[Command] =
    Behaviors.receive[Command] { (ctx, msg) =>
      msg match {
        case DownloadFiles(files) =>
          files.foreach { fileHash =>
            val downloader = ctx.spawn(RecordFileDownloader.behavior(fileStorage, peerManager),
              "RecordFileDownloader")
            ctx.watch(downloader)
            log.info(s"request to download file $fileHash")
            downloader ! DownloadFile(fileHash, ctx.self)
          }
          if (ctx.children.isEmpty) stopped else same
        case DownloadFailed(fileHash, reason) =>
          log.info(s"failed to download file $fileHash: $reason")
          same
        case DownloadSucceeded(fileHash) =>
          log.info(s"downloaded file $fileHash")
          same
      }
    } receiveSignal {
      case (ctx, Terminated(ref)) =>
        log.info(s"${ctx.self}: $ref is terminated")
        if (ctx.children.isEmpty) stopped else same
    }
}
