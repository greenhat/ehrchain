package ehr.contract

import java.time.Instant

import ehr.core.EncryptedAes256Keys
import ehr.serialization._
import scorex.core.serialization.{BytesSerializable, Serializer}
import scorex.core.transaction.box.proposition.PublicKey25519Proposition

import scala.util.{Failure, Success, Try}

trait ContractTerm extends Serializable

@SerialVersionUID(0L)
case object Unlimited extends ContractTerm
@SerialVersionUID(0L)
final case class ValidUntil(date: Instant) extends ContractTerm

trait Contract extends BytesSerializable {

  val timestamp: Instant

  def semanticValidity: Try[Unit]
}

@SerialVersionUID(0L)
final case class AppendContract(patientPK: PublicKey25519Proposition,
                                providerPK: PublicKey25519Proposition,
                                timestamp: Instant,
                                term: ContractTerm) extends Contract {
  override type M = AppendContract

  override def serializer: Serializer[M] = byteSerializer[M]

  override def semanticValidity: Try[Unit] = term match {
    case Unlimited => Success()
    case ValidUntil(date) if timestamp.compareTo(date) < 0 => Success()
    case ValidUntil(date) if timestamp.compareTo(date) >= 0 =>
      Failure[Unit](new Exception("contract ValidUntil date is less than contract's timestamp"))
  }
}

@SerialVersionUID(0L)
final case class ReadContract(patientPK: PublicKey25519Proposition,
                              providerPK: PublicKey25519Proposition,
                              timestamp: Instant,
                             // todo providerPK -> AesKey pairs
                              encryptedKeys: EncryptedAes256Keys) extends Contract {
  override type M = ReadContract

  override def serializer: Serializer[M] = byteSerializer[M]

  override def semanticValidity: Try[Unit] = ???
}
