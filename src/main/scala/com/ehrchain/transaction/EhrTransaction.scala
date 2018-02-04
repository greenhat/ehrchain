package com.ehrchain.transaction

import com.ehrchain.core.RecordType
import com.google.common.primitives.{Bytes, Longs}
import io.circe.Json
import io.circe.syntax._
import scorex.core.serialization.Serializer
import scorex.core.transaction.Transaction
import scorex.core.transaction.box.proposition.PublicKey25519Proposition
import scorex.core.transaction.proof.Signature25519
import scorex.core.transaction.state.{PrivateKey25519, PrivateKey25519Companion}
import scorex.crypto.encode.Base58
import scorex.crypto.signatures.{Curve25519, PublicKey, Signature}

import scala.util.Try

class EhrTransaction(val provider: PublicKey25519Proposition,
                     val patient: PublicKey25519Proposition,
                     val record: RecordType,
                     val signature: Signature25519,
                     val timestamp: Long) extends Transaction[PublicKey25519Proposition] {

  override type M = EhrTransaction

  override def serializer: Serializer[EhrTransaction] = EhrTransactionSerializer

  override lazy val json: Json = Map(
    "id" -> Base58.encode(id).asJson,
    // fixme add the rest
  ).asJson

  override lazy val messageToSign: Array[Byte] =
    EhrTransaction.generateMessageToSign(timestamp, patient, provider, record)

  lazy val validity: Try[Unit] = Try {
    require(timestamp > 0)
    // record's signature (made with provider's SK) is valid (verified with provider's PK)
    signature.isValid(provider, messageToSign)
  }
}

object EhrTransaction {

  def generateMessageToSign(timestamp: Long,
                            patient: PublicKey25519Proposition,
                            provider: PublicKey25519Proposition,
                            record: RecordType): Array[Byte] = {
    Bytes.concat(
      Longs.toByteArray(timestamp),
      patient.bytes,
      provider.bytes,
      record
    )
  }
}

object EhrTransactionSerializer extends Serializer[EhrTransaction] {

  override def toBytes(obj: EhrTransaction): Array[Byte] = {
    Bytes.concat(
      Longs.toByteArray(obj.timestamp),
      obj.provider.bytes,
      obj.patient.bytes,
      obj.signature.bytes,
      obj.record
    )
  }

  override def parseBytes(bytes: Array[Byte]): Try[EhrTransaction] = Try {
    val timestamp = Longs.fromByteArray(bytes.slice(0, 8))
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
    new EhrTransaction(provider, patient, record, signature, timestamp)
  }
}

object EhrTransactionCompanion {

  def generate(patientPK: PublicKey25519Proposition,
               providerKeys: (PrivateKey25519, PublicKey25519Proposition),
               record: RecordType,
               timestamp: Long): EhrTransaction = {
    val providerPK = providerKeys._2
    val providerSK = providerKeys._1
    val messageToSign = EhrTransaction.generateMessageToSign(timestamp, patientPK, providerPK, record)
    val signature = PrivateKey25519Companion.sign(providerSK, messageToSign)
    new EhrTransaction(providerPK, patientPK, record, signature, timestamp)
  }
}

