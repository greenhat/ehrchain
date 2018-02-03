package com.ehrchain.transaction

import com.google.common.primitives.{Bytes, Longs}
import io.circe.Json
import io.circe.syntax._
import scorex.core.serialization.Serializer
import scorex.core.transaction.Transaction
import scorex.core.transaction.box.proposition.PublicKey25519Proposition
import scorex.core.transaction.proof.Signature25519
import scorex.crypto.encode.Base58
import scorex.crypto.signatures.{Curve25519, PublicKey, Signature}

import scala.util.Try

class EhrTransaction(val provider: PublicKey25519Proposition,
                     val patient: PublicKey25519Proposition,
                     val record: Array[Byte],
                     val signature: Signature25519,
                     val timestamp: Long) extends Transaction[PublicKey25519Proposition] {
  override type M = EhrTransaction

  override def serializer: Serializer[EhrTransaction] = EhrTransactionSerializer

  override lazy val json: Json = Map(
    "id" -> Base58.encode(id).asJson,
    // TODO add the rest
  ).asJson

  override lazy val messageToSign: Array[Byte] = {
    Bytes.concat(
      Longs.toByteArray(timestamp),
      patient.bytes,
      provider.bytes,
      record
    )
  }

  lazy val isValid: Try[Unit] = Try {
    require(timestamp > 0)
    // record's signature (made with provider's SK) is valid (verified with provider's PK)
    signature.isValid(provider, messageToSign)
    // TODO how does a patient give authorization to a provider to append an EHR?
    // contract down the blockchain?
    // Akin to msg = (patientPK, providerPK, terms(duration, etc.), accessType)
    // with signature = (msg signed with patientSK)
  }
}

object EhrTransactionSerializer extends Serializer[EhrTransaction] {

  override def toBytes(obj: EhrTransaction): Array[Byte] = {
    Bytes.concat(Longs.toByteArray(obj.timestamp),
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
    val record = bytes.slice(recordStart, bytes.length - 1)
    new EhrTransaction(provider, patient, record, signature, timestamp)
  }
}

