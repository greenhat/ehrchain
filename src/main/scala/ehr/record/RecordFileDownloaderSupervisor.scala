package ehr.record

import akka.actor.ActorRef
import akka.typed.scaladsl.Actor
import akka.typed.{Behavior, Terminated}
import ehr.block.EhrBlock
import ehr.record.RecordFileDownloader.DownloadFile
import ehr.transaction.RecordTransaction
import scorex.core.utils.ScorexLogging

@SuppressWarnings(Array("org.wartremover.warts.Nothing"))
object RecordFileDownloaderSupervisor extends ScorexLogging {

  final case class DownloadMissingFiles(block: EhrBlock)

  def behavior(fileStorage: RecordFileStorage,
               peerManager: ActorRef): Behavior[DownloadMissingFiles] =
    akka.typed.scaladsl.Actor.immutable[DownloadMissingFiles] { (ctx, msg) =>
      msg match {
        case DownloadMissingFiles(block) =>
          missingFiles(block, fileStorage).foreach { hash =>
            val downloader = ctx.spawn(RecordFileDownloader.behavior(fileStorage, peerManager),
              "RecordFileDownloader")
            ctx.watch(downloader)
            downloader ! DownloadFile(hash)
          }
          if (ctx.children.isEmpty) Actor.stopped else Actor.same
      }
    } onSignal {
      case (ctx, Terminated(ref)) =>
        log.info(s"${ctx.self}: $ref is terminated")
        if (ctx.children.isEmpty) Actor.stopped else Actor.same
    }

  def missingFiles(block: EhrBlock, fileStorage: RecordFileStorage): Seq[FileHash] = {
    block.transactions.collect { case recTx: RecordTransaction => recTx }
      .flatMap(_.record.files.filter(fileStorage.get(_).isEmpty))
  }
}
