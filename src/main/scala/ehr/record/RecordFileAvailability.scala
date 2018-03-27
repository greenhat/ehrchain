package ehr.record

import akka.actor.ActorRef
import akka.typed.{Behavior, Terminated}
import akka.typed.scaladsl.Actor
import akka.typed.scaladsl.adapter._
import ehr.block.EhrBlock
import ehr.transaction.RecordTransaction
import scorex.core.network.peer.PeerManager.ReceivableMessages.GetAllPeers
import scorex.core.utils.ScorexLogging

object RecordFileDownloaderSupervisor extends ScorexLogging {

  final case class DownloadMissingFiles(block: EhrBlock)

  def behavior(fileStorage: RecordFileStorage,
               peerManager: ActorRef): Behavior[DownloadMissingFiles] =
    akka.typed.scaladsl.Actor.immutable[DownloadMissingFiles] { (ctx, msg) =>
      msg match {
        case DownloadMissingFiles(block) =>
          RecordFileAvailability.missingFiles(block, fileStorage)
          // todo spawn file downloader for each missing file
          Actor.same
      }

    } onSignal {
      case (ctx, Terminated(ref)) =>
        log.info(s"${ctx.self}: $ref is terminated")
        if (ctx.children.isEmpty) Actor.stopped
        else Actor.same
    }

}

object RecordFileDownloader extends ScorexLogging {

  final case class DownloadFile(hash: FileHash,
                                peerManagerRef: ActorRef,
                                fileStorage: RecordFileStorage)
}

object RecordFileAvailability {

  def missingFiles(block: EhrBlock, fileStorage: RecordFileStorage): Seq[FileHash] = {
    block.transactions.collect { case recTx: RecordTransaction => recTx }
      .flatMap(_.record.files.filter(fileStorage.get(_).isEmpty))
  }
}
