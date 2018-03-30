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
  final case class Error(errors: Seq[Throwable]) extends FailureReason
  final case class DownloadFailed(fileHash: FileHash, reason: FailureReason) extends Command

  def behavior(fileStorage: RecordFileStorage,
               peerManager: ActorRef): Behavior[Command] =
    Behaviors.immutable[Command] { (ctx, msg) =>
      msg match {
        case DownloadFiles(files) =>
          files.foreach { hash =>
            val downloader = ctx.spawn(RecordFileDownloader.behavior(fileStorage, peerManager),
              "RecordFileDownloader")
            ctx.watch(downloader)
            downloader ! DownloadFile(hash, ctx.self)
          }
          if (ctx.children.isEmpty) stopped else same
      }
    } onSignal {
      case (ctx, Terminated(ref)) =>
        log.info(s"${ctx.self}: $ref is terminated")
        if (ctx.children.isEmpty) stopped else same
    }
}
