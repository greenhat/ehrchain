package com.ehrchain.transaction

import com.ehrchain.core.TimeStamp
import com.ehrchain.record.Record
import com.ehrchain.serialization._
import com.google.common.primitives.{Bytes, Longs}
import io.circe.Json
import io.circe.syntax._
import scorex.core.serialization.Serializer
import scorex.core.transaction.box.proposition.PublicKey25519Proposition
import scorex.core.transaction.proof.Signature25519
import scorex.core.transaction.state.{PrivateKey25519, PrivateKey25519Companion}
import scorex.crypto.encode.Base58
import scorex.crypto.signatures.{Curve25519, PublicKey, Signature}

import scala.util.Try

final case class EhrRecordTransaction(generator: PublicKey25519Proposition,
                                      subject: PublicKey25519Proposition,
                                      record: Record,
                                      signature: Signature25519,
                                      timestamp: TimeStamp) extends EhrTransaction {

  override type M = EhrRecordTransaction

  override def serializer: Serializer[M] = byteSerializer[M]

  override lazy val json: Json = Map(
    "id" -> Base58.encode(id).asJson,
    "timestamp" -> timestamp.toLong.asJson,
    "generator" -> Base58.encode(generator.bytes).asJson,
    "subject" -> Base58.encode(subject.bytes).asJson,
    "record" -> record.json,
    "signature" -> Base58.encode(signature.bytes).asJson,
  ).asJson

  override lazy val messageToSign: Array[Byte] =
    EhrRecordTransaction.generateMessageToSign(timestamp, subject, generator, record)

  override def semanticValidity: Boolean =
    super.semanticValidity && record.bytes.length <= EhrRecordTransaction.MaxRecordSize
}

object EhrRecordTransaction {

  val MaxRecordSize: Int = 1024

  def generateMessageToSign(timestamp: TimeStamp,
                            patient: PublicKey25519Proposition,
                            provider: PublicKey25519Proposition,
                            record: Record): Array[Byte] =
    Bytes.concat(
      Longs.toByteArray(timestamp),
      patient.bytes,
      provider.bytes,
      record.bytes
    )
}

object EhrRecordTransactionCompanion {

  // todo move to test (generators)?
  def generate(patientPK: PublicKey25519Proposition,
               providerKeys: (PrivateKey25519, PublicKey25519Proposition),
               record: Record,
               timestamp: TimeStamp): EhrRecordTransaction = {
    val providerPK = providerKeys._2
    val providerSK = providerKeys._1
    val messageToSign = EhrRecordTransaction.generateMessageToSign(timestamp, patientPK, providerPK, record)
    val signature = PrivateKey25519Companion.sign(providerSK, messageToSign)
    EhrRecordTransaction(providerPK, patientPK, record, signature, timestamp)
  }
}

