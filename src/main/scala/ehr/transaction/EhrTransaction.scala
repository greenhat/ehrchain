package ehr.transaction

import java.time.Instant

import io.circe.Encoder
import io.circe.syntax._
import scorex.core.serialization.BytesSerializable
import scorex.core.transaction.Transaction
import scorex.core.transaction.box.proposition.PublicKey25519Proposition
import scorex.core.transaction.proof.Signature25519

@SuppressWarnings(Array("org.wartremover.warts.LeakingSealed"))
trait EhrTransaction extends Transaction[PublicKey25519Proposition] with BytesSerializable {

  val generator: PublicKey25519Proposition
  val signature: Signature25519
  val timestamp: Instant

  def semanticValidity: Boolean =
    signature.isValid(generator, messageToSign)

  override def toString: String = s"EhrTransaction(${this.asJson}})"

}

object EhrTransaction {
  implicit val jsonEncoder: Encoder[EhrTransaction] = {
    case contract: ContractTransaction => contract.asJson
    case record: RecordTransaction => record.asJson
  }
}
