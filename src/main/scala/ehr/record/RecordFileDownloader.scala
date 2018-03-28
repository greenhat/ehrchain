package ehr.record

import java.net.{InetSocketAddress, URL}

import akka.typed.scaladsl.ActorContext
import akka.actor.ActorRef
import akka.typed.Behavior
import akka.typed.scaladsl.Actor
import scorex.core.network.Handshake
import scorex.core.network.peer.PeerManager.ReceivableMessages.GetConnectedPeers
import scorex.core.utils.ScorexLogging
import akka.pattern.ask
import akka.util.Timeout
import ehr.record.FileDownloader._

import scala.annotation.tailrec
import scala.concurrent.duration._
import scala.util.Try

object RecordFileDownloader extends ScorexLogging {

  trait Command
  final case class DownloadFile(hash: FileHash) extends Command
  final case class AskPeers(peers: Seq[InetSocketAddress], hash: FileHash) extends Command

  def behavior(fileStorage: RecordFileStorage,
               peerManager: ActorRef): Behavior[Command] =
    akka.typed.scaladsl.Actor.immutable[Command] { (ctx, msg) =>
      msg match {
        case DownloadFile(fileHash) =>
          withPeers(peerManager) { peers =>
            ctx.self ! AskPeers(peers, fileHash)
          }(ctx)
          Actor.same
        case AskPeers(peers, fileHash) =>
          @tailrec
          def loop(rest: List[InetSocketAddress]): Behavior[Command] = rest match {
            case h::t if downloadFile(fileUrl(h, fileHash), fileStorage).isFailure => loop(t)
            case _ => Actor.stopped
          }
          loop(peers.toList)
      }
    }

  private implicit val timeout: Timeout = Timeout(5 seconds)

  private def withPeers(peerManager: ActorRef)(f: Seq[InetSocketAddress] => Unit)
                       (implicit actorContext: ActorContext[Command]): Unit =
    (peerManager ? GetConnectedPeers)
    .mapTo[Seq[Handshake]]
    .map(_.flatMap(_.declaredAddress.toList))(actorContext.executionContext)
    .foreach(f)(actorContext.executionContext)

}

object FileDownloader {

  def fileUrl(address: InetSocketAddress, fileHash: FileHash): URL = ???

  def downloadFile(url: URL, fileStorage: RecordFileStorage): Try[Unit] = ???
}
