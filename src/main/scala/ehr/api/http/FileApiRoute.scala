package ehr.api.http

import akka.actor.{ActorRef, ActorRefFactory}
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, StatusCodes}
import akka.http.scaladsl.server.Route
import com.google.common.io.ByteStreams
import ehr.core.DigestSha256
import ehr.record.{FileSource, FileHash, RecordFileStorage}
import scorex.core.api.http.{ApiError, ApiRoute}
import scorex.core.settings.RESTApiSettings
import scorex.crypto.encode.Base58

import scala.util.{Failure, Success}


final case class FileApiRoute(override val settings: RESTApiSettings,
                              fileStore: RecordFileStorage,
                              nodeViewHolderRef: ActorRef)
                             (implicit val context: ActorRefFactory) extends ApiRoute {

  override val route: Route = (pathPrefix("file") & withCors) {
    request
  }

  def request: Route = (get & path("hash" / Segment)) { encodedHash =>
    withFile(encodedHash) { fileSource =>
      complete(
        HttpEntity(ContentTypes.`application/octet-stream`,
          ByteStreams.toByteArray(fileSource.inputStream)))
    }
  }

  private def withFile(encodedHash: String)(fn: FileSource => Route): Route =
    Base58.decode(encodedHash) match {
      case Failure(e) => complete(ApiError(e.getLocalizedMessage, StatusCodes.NotFound))
      case Success(hash) => fileStore.get(FileHash(DigestSha256(hash)))
        .map(fn(_))
        .getOrElse(complete(ApiError("file not found", StatusCodes.NotFound)))
    }
}
