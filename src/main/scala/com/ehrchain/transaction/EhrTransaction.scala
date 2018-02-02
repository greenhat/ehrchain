package com.ehrchain.transaction

import com.google.common.primitives.{Bytes, Longs}
import io.circe.Json
import io.circe.syntax._
import scorex.core.serialization.Serializer
import scorex.core.transaction.Transaction
import scorex.core.transaction.box.proposition.PublicKey25519Proposition
import scorex.crypto.encode.Base58

import scala.util.Try

class EhrTransaction(val timestamp: Long) extends Transaction[PublicKey25519Proposition] {
  override type M = EhrTransaction

  override def serializer: Serializer[EhrTransaction] = EhrTransactionSerializer

  override lazy val json: Json = Map(
    "id" -> Base58.encode(id).asJson,
  ).asJson

  override lazy val messageToSign: Array[Byte] =  {
    Array[Byte]()
  }
}

object EhrTransactionSerializer extends Serializer[EhrTransaction] {

  override def toBytes(obj: EhrTransaction): Array[Byte] = {
    Bytes.concat(Longs.toByteArray(obj.timestamp))
  }

  override def parseBytes(bytes: Array[Byte]): Try[EhrTransaction] = Try {
    val timestamp = Longs.fromByteArray(bytes.slice(8, 16))
    new EhrTransaction(timestamp)
  }
}

