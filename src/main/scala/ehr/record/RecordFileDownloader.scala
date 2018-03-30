package ehr.record

import java.net.{InetSocketAddress, URL}

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors.{same, stopped}
import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import akka.actor.{ActorRef, typed}
import akka.pattern.ask
import akka.util.Timeout
import ehr.record.DownloadFileEffect.downloadFileEffect
import ehr.record.RecordFileDownloaderSupervisor.{DownloadFailed, DownloadSucceeded, DownloadErrors, NoPeers}
import scorex.core.network.Handshake
import scorex.core.network.peer.PeerManager.ReceivableMessages.GetConnectedPeers
import scorex.core.utils.ScorexLogging

import scala.annotation.tailrec
import scala.concurrent.duration._
import scala.language.postfixOps

object RecordFileDownloader extends ScorexLogging {

  type ReplyToActor = typed.ActorRef[RecordFileDownloaderSupervisor.Command]

  sealed trait Command
  final case class DownloadFile(hash: FileHash, replyTo: ReplyToActor) extends Command
  private final case class AskPeers(peers: Seq[InetSocketAddress], hash: FileHash,
                            replyTo: ReplyToActor) extends Command

  type DownloadEffect = (InetSocketAddress, FileHash, RecordFileStorage) => Either[Throwable, Unit]

  @SuppressWarnings(Array("org.wartremover.warts.DefaultArguments"))
  def behavior(fileStorage: RecordFileStorage, peerManager: ActorRef)
              (implicit downloadEffect: DownloadEffect = downloadFileEffect): Behavior[Command] =
    Behaviors.immutable[Command] { (ctx, msg) =>
      msg match {
        case DownloadFile(fileHash, replyTo) =>
          withPeers(peerManager) { peers =>
            ctx.self ! AskPeers(peers, fileHash, replyTo)
          }(ctx)
          same
        case AskPeers(peers, fileHash, replyTo) if peers.isEmpty =>
          replyTo ! DownloadFailed(fileHash, NoPeers)
          stopped
        case AskPeers(peers, fileHash, replyTo) =>
          @tailrec
          def loop(rest: List[InetSocketAddress], errors: List[Throwable]): Behavior[Command] =
            rest match {
              case h::t =>
                downloadEffect(h, fileHash, fileStorage) match {
                  case Left(e) => loop(t, e :: errors)
                  case Right(()) =>
                    replyTo ! DownloadSucceeded(fileHash)
                    stopped
                }
            case Nil =>
              replyTo ! DownloadFailed(fileHash, DownloadErrors(errors))
              stopped
          }
          loop(peers.toList, List())
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

object DownloadFileEffect {

  def downloadFileEffect(address: InetSocketAddress,
                         fileHash: FileHash,
                         fileStorage: RecordFileStorage): Either[Throwable, Unit] =
    downloadFile(fileUrl(address, fileHash), fileStorage)

  def fileUrl(address: InetSocketAddress, fileHash: FileHash): URL = ???

  private def downloadFile(url: URL, fileStorage: RecordFileStorage): Either[Throwable, Unit] = ???
}
