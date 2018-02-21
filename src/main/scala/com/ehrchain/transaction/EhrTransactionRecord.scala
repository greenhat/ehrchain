package com.ehrchain.transaction

import com.ehrchain.core.{RecordType, TimeStamp}
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

final case class EhrTransactionRecord(generator: PublicKey25519Proposition,
                                 subject: PublicKey25519Proposition,
                                 record: RecordType,
                                 signature: Signature25519,
                                 timestamp: TimeStamp) extends EhrTransaction {

  override type M = EhrTransactionRecord

  override def serializer: Serializer[EhrTransactionRecord] = EhrTransactionSerializer

  override lazy val json: Json = Map(
    "id" -> Base58.encode(id).asJson,
    "timestamp" -> timestamp.toLong.asJson,
    "provider" -> Base58.encode(generator.bytes).asJson,
    "patient" -> Base58.encode(subject.bytes).asJson,
    "record" -> Base58.encode(record).asJson,
    "signature" -> Base58.encode(signature.bytes).asJson,
  ).asJson

  override lazy val messageToSign: Array[Byte] =
    EhrTransactionRecord.generateMessageToSign(timestamp, subject, generator, record)

  override def validity: Boolean =
    super.validity && record.nonEmpty && record.length <= EhrTransactionRecord.MaxRecordSize
}

object EhrTransactionRecord {

  val MaxRecordSize: Int = 1024

  def generateMessageToSign(timestamp: TimeStamp,
                            patient: PublicKey25519Proposition,
                            provider: PublicKey25519Proposition,
                            record: RecordType): Array[Byte] =
    Bytes.concat(
      Longs.toByteArray(timestamp),
      patient.bytes,
      provider.bytes,
      record
    )
}

object EhrTransactionSerializer extends Serializer[EhrTransactionRecord] {

  override def toBytes(obj: EhrTransactionRecord): Array[Byte] = {
    Bytes.concat(
      Longs.toByteArray(obj.timestamp),
      obj.generator.bytes,
      obj.subject.bytes,
      obj.signature.bytes,
      obj.record
    )
  }

  override def parseBytes(bytes: Array[Byte]): Try[EhrTransactionRecord] = Try {
    val timestamp = TimeStamp @@ Longs.fromByteArray(bytes.slice(0, 8))
    val providerStart = 8
    val providerEnd = providerStart + Curve25519.KeyLength
    val providerPK = PublicKey @@ bytes.slice(providerStart, providerEnd)
    val provider = PublicKey25519Proposition(providerPK)
    val patientStart = providerEnd
    val patientEnd = patientStart + Curve25519.KeyLength
    val patientPK = PublicKey @@ bytes.slice(patientStart, patientEnd)
    val patient = PublicKey25519Proposition(patientPK)
    val signatureStart = patientEnd
    val signatureEnd = patientEnd + Curve25519.SignatureLength
    val signature = Signature25519(Signature @@ bytes.slice(signatureStart, signatureEnd))
    val recordStart = signatureEnd
    val record = RecordType @@ bytes.slice(recordStart, bytes.length)
    EhrTransactionRecord(provider, patient, record, signature, timestamp)
  }
}

object EhrTransactionCompanion {

  // todo move to test (generators)?
  def generate(patientPK: PublicKey25519Proposition,
               providerKeys: (PrivateKey25519, PublicKey25519Proposition),
               record: RecordType,
               timestamp: TimeStamp): EhrTransactionRecord = {
    val providerPK = providerKeys._2
    val providerSK = providerKeys._1
    val messageToSign = EhrTransactionRecord.generateMessageToSign(timestamp, patientPK, providerPK, record)
    val signature = PrivateKey25519Companion.sign(providerSK, messageToSign)
    EhrTransactionRecord(providerPK, patientPK, record, signature, timestamp)
  }
}

