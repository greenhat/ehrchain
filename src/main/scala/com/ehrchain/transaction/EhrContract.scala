package com.ehrchain.transaction

import java.time.Instant

import scorex.core.transaction.box.proposition.PublicKey25519Proposition

trait EhrContractTerm {}
case object Unlimited extends EhrContractTerm
final case class ValidUntil(date: Instant) extends EhrContractTerm

trait EhrContract {

  val timestamp: Instant
  val term: EhrContractTerm

  def validity: Either[Throwable, Boolean] = term match {
    case Unlimited => Right[Throwable, Boolean](true)
    case ValidUntil(date) if timestamp.compareTo(date) < 0 => Right[Throwable, Boolean](true)
    case ValidUntil(date) if timestamp.compareTo(date) >= 0 =>
      Left[Throwable, Boolean](new Exception("contract ValidUntil date is less than contract's timestamp"))
  }
}

final case class EhrAppendContract(patientPK: PublicKey25519Proposition,
                                   providerPK: PublicKey25519Proposition,
                                   timestamp: Instant,
                                   term: EhrContractTerm)
  extends EhrContract {

}
