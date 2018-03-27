package ehr.api.http

import akka.actor.ActorRefFactory
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, StatusCodes}
import akka.http.scaladsl.server.Route
import com.google.common.io.ByteStreams
import ehr.core._
import ehr.record.{FileHash, FileSource, RecordFileStorage}
import scorex.core.api.http.{ApiError, ApiRoute}
import scorex.core.settings.RESTApiSettings
import scorex.crypto.encode.Base58


final case class FileApiRoute(override val settings: RESTApiSettings,
                              fileStore: RecordFileStorage)
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
    Base58.decode(encodedHash)
      .flatMap(DigestSha256.rawUnsafe)
      .map(FileHash(_))
      .toEither match {
        case Left(e) => complete(ApiError(e.getLocalizedMessage, StatusCodes.BadRequest))
        case Right(fileHash) => fileStore.get(fileHash)
          .map(fn(_))
          .getOrElse(complete(ApiError("file not found", StatusCodes.NotFound)))
      }
}
