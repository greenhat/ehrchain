package com.ehrchain.contract

import java.time.Instant

import com.ehrchain.serialization._
import scorex.core.serialization.{BytesSerializable, Serializer}
import scorex.core.transaction.box.proposition.PublicKey25519Proposition

trait EhrContractTerm extends Serializable

@SerialVersionUID(0L)
case object Unlimited extends EhrContractTerm
@SerialVersionUID(0L)
final case class ValidUntil(date: Instant) extends EhrContractTerm

trait EhrContract extends BytesSerializable {

  // todo every contract must have an id
  val timestamp: Instant
  val term: EhrContractTerm

  def validity: Either[Throwable, Boolean] = term match {
    case Unlimited => Right[Throwable, Boolean](true)
    case ValidUntil(date) if timestamp.compareTo(date) < 0 => Right[Throwable, Boolean](true)
    case ValidUntil(date) if timestamp.compareTo(date) >= 0 =>
      Left[Throwable, Boolean](new Exception("contract ValidUntil date is less than contract's timestamp"))
  }
}

@SerialVersionUID(0L)
final case class EhrAppendContract(patientPK: PublicKey25519Proposition,
                                   providerPK: PublicKey25519Proposition,
                                   timestamp: Instant,
                                   term: EhrContractTerm) extends EhrContract {
  override type M = EhrAppendContract

  override def serializer: Serializer[M] = byteSerializer[M]
}