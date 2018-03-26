package ehr.record

import ehr.core.DigestSha256
import ehr.crypto.Sha256
import ehr.serialization._
import io.circe.Encoder
import io.circe.syntax._
import scorex.core.serialization.{BytesSerializable, Serializer}
import scorex.crypto.encode.Base58

import scala.util.Try

@SerialVersionUID(0L)
final case class FileHash(hash: DigestSha256) {
  override def toString: String = hash.toString
}

@SerialVersionUID(0L)
final case class Record(files: Seq[FileHash]) extends BytesSerializable {

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

object FileHash {

  def generate(source: FileSource): Try[FileHash] = {
    Sha256.digest(source.inputStream).map(FileHash(_))
  }

  implicit val jsonEncoder: Encoder[FileHash] = (file: FileHash) => {
    Map(
      "hash" -> Base58.encode(file.hash.bytes).asJson
    ).asJson
  }
}
