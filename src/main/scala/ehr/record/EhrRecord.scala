package ehr.record

import ehr.serialization._
import ehr.core.DigestSha256
import ehr.crypto.Sha256
import io.circe.Json
import io.circe.syntax._
import scorex.core.serialization.{BytesSerializable, JsonSerializable, Serializer}
import scorex.crypto.encode.Base58

import scala.util.Try

@SerialVersionUID(0L)
final case class RecordFile(hash: DigestSha256) extends JsonSerializable {
  override lazy val json: Json = Map(
    "hash" -> Base58.encode(hash.bytes).asJson
  ).asJson

}

@SerialVersionUID(0L)
final case class Record(files: Seq[RecordFile]) extends BytesSerializable with JsonSerializable {

  require(files.nonEmpty)

  override type M = this.type
  override def serializer: Serializer[M] = byteSerializer[M]

  override lazy val json: Json = Map(
    "files" -> files.map(_.json).asJson
  ).asJson

}

object RecordFile {

  def generate(source: FileSource): Try[RecordFile] = {
    Sha256.digest(source.inputStream).map(RecordFile(_))
  }
}
