package ehr.record

import akka.actor.ActorRef
import akka.typed.Behavior
import akka.typed.scaladsl.Actor
import scorex.core.utils.ScorexLogging

object RecordFileDownloader extends ScorexLogging {

  final case class DownloadFile(hash: FileHash)

  def behavior(fileStorage: RecordFileStorage,
               peerManager: ActorRef): Behavior[DownloadFile] =
    akka.typed.scaladsl.Actor.immutable[DownloadFile] { (ctx, msg) =>
      msg match {
        case DownloadFile(hash) =>
          // todo get a peer and ask for the file
          // todo stop, if file is downloaded else ask the next peer
          Actor.same // todo or .stopped if sync download
      }
    }
}
