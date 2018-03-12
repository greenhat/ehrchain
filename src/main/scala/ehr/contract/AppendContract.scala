package ehr.contract

import java.time.Instant

import ehr.serialization._
import scorex.core.serialization.Serializer
import scorex.core.transaction.box.proposition.PublicKey25519Proposition

import scala.util.{Failure, Success, Try}


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

