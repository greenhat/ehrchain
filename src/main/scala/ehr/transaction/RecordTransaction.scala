package ehr.transaction

import java.time.Instant

import com.google.common.primitives.Bytes
import ehr.record.Record
import io.circe.{Encoder, Json}
import io.circe.syntax._
import scorex.core.serialization.Serializer
import scorex.core.transaction.box.proposition.PublicKey25519Proposition
import scorex.core.transaction.proof.Signature25519
import scorex.core.transaction.state.{PrivateKey25519, PrivateKey25519Companion}
import ehr.serialization._
import scorex.crypto.encode.Base58

  final case class RecordTransaction(generator: PublicKey25519Proposition,
                                     patient: PublicKey25519Proposition,
                                     record: Record,
                                     signature: Signature25519,
                                     timestamp: Instant) extends EhrTransaction {

    override type M = RecordTransaction

    override def serializer: Serializer[M] = byteSerializer[M]

    override lazy val messageToSign: Array[Byte] =
      RecordTransaction.generateMessageToSign(timestamp, patient, generator, record)

  override def semanticValidity: Boolean =
    super.semanticValidity && record.bytes.length <= RecordTransaction.MaxRecordSize
}

object RecordTransaction {

  val MaxRecordSize: Int = 1024

  def generateMessageToSign(timestamp: Instant,
                            patient: PublicKey25519Proposition,
                            provider: PublicKey25519Proposition,
                            record: Record): Array[Byte] =
    Bytes.concat(
      serialize(timestamp),
      patient.bytes,
      provider.bytes,
      record.bytes
    )

  implicit val jsonEncoder: Encoder[RecordTransaction] = (tx: RecordTransaction) => {
    Map(
      "id" -> Base58.encode(tx.id).asJson,
      "timestamp" -> tx.timestamp.asJson,
      "generator" -> Base58.encode(tx.generator.bytes).asJson,
      "patient" -> Base58.encode(tx.patient.bytes).asJson,
      "record" -> tx.record.asJson,
      "signature" -> Base58.encode(tx.signature.bytes).asJson,
    ).asJson
  }
}

object EhrRecordTransactionCompanion {

  def generate(patientPK: PublicKey25519Proposition,
               providerKeys: (PrivateKey25519, PublicKey25519Proposition),
               record: Record,
               timestamp: Instant): RecordTransaction = {
    val providerPK = providerKeys._2
    val providerSK = providerKeys._1
    val messageToSign = RecordTransaction.generateMessageToSign(timestamp, patientPK, providerPK, record)
    val signature = PrivateKey25519Companion.sign(providerSK, messageToSign)
    RecordTransaction(providerPK, patientPK, record, signature, timestamp)
  }
}

