package ehr.record

import ehr.serialization._
import ehr.core.DigestSha256
import ehr.crypto.Sha256
import io.circe.{Encoder, Json}
import io.circe.syntax._
import scorex.core.serialization.{BytesSerializable, Serializer}
import scorex.crypto.encode.Base58

import scala.util.Try

@SerialVersionUID(0L)
final case class RecordFile(hash: DigestSha256) {

}

@SerialVersionUID(0L)
final case class Record(files: Seq[RecordFile]) extends BytesSerializable {

  require(files.nonEmpty)

  override type M = this.type
  override def serializer: Serializer[M] = byteSerializer[M]
}

object Record {

  implicit val jsonEncoder: Encoder[Record] = (record: Record) => {
    Map(
      "files" -> record.files.map(_.asJson).asJson
    ).asJson
  }
}

object RecordFile {

  def generate(source: FileSource): Try[RecordFile] = {
    Sha256.digest(source.inputStream).map(RecordFile(_))
  }

  implicit val jsonEncoder: Encoder[RecordFile] = (file: RecordFile) => {
    Map(
      "hash" -> Base58.encode(file.hash.bytes).asJson
    ).asJson
  }
}
