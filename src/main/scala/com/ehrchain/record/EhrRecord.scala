package com.ehrchain.record

import com.ehrchain.serialization._
import io.circe.Json
import io.circe.syntax._
import scorex.core.serialization.{BytesSerializable, JsonSerializable, Serializer}
import scorex.crypto.encode.Base58
import scorex.crypto.hash.{Blake2b256, Digest32}

@SerialVersionUID(0L)
final case class RecordFile(size: Long,
                            hash: Digest32) extends JsonSerializable {
  override lazy val json: Json = Map(
    "size" -> size.asJson,
    "hash" -> Base58.encode(hash).asJson
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

  def generate(bytes: Array[Byte]): RecordFile = RecordFile(bytes.length, Blake2b256(bytes))
}
