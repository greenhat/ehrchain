package ehr.record

import java.net.{InetSocketAddress, URL}

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors.{same, stopped}
import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import akka.actor.{ActorRef, typed}
import akka.pattern.ask
import akka.util.Timeout
import com.google.common.io.ByteStreams
import ehr.api.http.FileApiRoute
import ehr.record.DownloadFileEffect.downloadFileEffect
import ehr.record.RecordFileDownloader.DownloadEffect
import ehr.record.RecordFileDownloaderSupervisor.{DownloadErrors, DownloadFailed, DownloadSucceeded, NoPeers}
import scorex.core.network.peer.PeerManager.ReceivableMessages.KnownPeers
import scorex.core.utils.ScorexLogging

import scala.annotation.tailrec
import scala.concurrent.duration._
import scala.language.postfixOps
import scala.util.{Failure, Success, Try}

object RecordFileDownloader extends ScorexLogging {

  type ReplyToActor = typed.ActorRef[RecordFileDownloaderSupervisor.Command]

  sealed trait Command
  final case class DownloadFile(hash: FileHash, replyTo: ReplyToActor) extends Command
  private final case class AskPeers(peers: Seq[InetSocketAddress], hash: FileHash,
                            replyTo: ReplyToActor) extends Command

  type DownloadEffect = (InetSocketAddress, FileHash, RecordFileStorage) => Try[Unit]

  @SuppressWarnings(Array("org.wartremover.warts.DefaultArguments"))
  def behavior(fileStorage: RecordFileStorage, peerManager: ActorRef)
              (implicit downloadEffect: DownloadEffect = downloadFileEffect): Behavior[Command] =
    Behaviors.receive[Command] { (ctx, msg) =>
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
                  case Failure(e) => loop(t, e :: errors)
                  case Success(()) =>
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
    (peerManager ? KnownPeers)
    .mapTo[Seq[InetSocketAddress]]
    .foreach(f)(actorContext.executionContext)

}

object DownloadFileEffect extends ScorexLogging {

  val downloadFileEffect: DownloadEffect = { (addr, fileHash, fileStorage) =>
    downloadFile(fileUrl(addr, fileHash), fileStorage, fileHash)
  }

  def fileUrl(address: InetSocketAddress, fileHash: FileHash): URL =
    new URL("http",
      address.getHostString,
      address.getPort + 1, // rest API port
      s"/${FileApiRoute.pathPrefix}/${FileApiRoute.requestPath}/$fileHash")

  private def downloadFile(url: URL,
                           fileStorage: RecordFileStorage,
                           fileHash: FileHash): Try[Unit] =
    for {
      _ <- Try { log.debug(s"loading file: $url") }
      bytes <- Try[Array[Byte]] { ByteStreams.toByteArray(url.openStream()) }
      fileSource = FileSource.fromByteArray(bytes)
      computedHash <- FileHash.generate(fileSource)
      _ <- Try[Unit] { if (computedHash != fileHash) new RuntimeException("invalid hash") }
    } yield fileStorage.put(fileHash, fileSource)

}
