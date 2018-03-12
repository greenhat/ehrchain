package ehr.contract

import java.time.Instant

import ehr.serialization._
import scorex.core.serialization.{BytesSerializable, Serializer}
import scorex.core.transaction.box.proposition.PublicKey25519Proposition

trait ContractTerm extends Serializable

@SerialVersionUID(0L)
case object Unlimited extends ContractTerm
@SerialVersionUID(0L)
final case class ValidUntil(date: Instant) extends ContractTerm

trait Contract extends BytesSerializable {

  val timestamp: Instant
  val term: ContractTerm

  def validity: Either[Throwable, Boolean] = term match {
    case Unlimited => Right[Throwable, Boolean](true)
    case ValidUntil(date) if timestamp.compareTo(date) < 0 => Right[Throwable, Boolean](true)
    case ValidUntil(date) if timestamp.compareTo(date) >= 0 =>
      Left[Throwable, Boolean](new Exception("contract ValidUntil date is less than contract's timestamp"))
  }
}

@SerialVersionUID(0L)
final case class AppendContract(patientPK: PublicKey25519Proposition,
                                providerPK: PublicKey25519Proposition,
                                timestamp: Instant,
                                term: ContractTerm) extends Contract {
  override type M = AppendContract

  override def serializer: Serializer[M] = byteSerializer[M]
}
