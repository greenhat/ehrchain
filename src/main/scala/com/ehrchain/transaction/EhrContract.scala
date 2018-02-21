package com.ehrchain.transaction

import java.time.Instant

import scorex.core.transaction.box.proposition.PublicKey25519Proposition

trait EhrContract {

  def validity: Either[Throwable, Boolean]
}

trait EhrContractTerm {}
case object Unlimited extends EhrContractTerm
final case class ValidUntil(date: Instant) extends EhrContractTerm

final case class EhrAppendContract(patientPK: PublicKey25519Proposition,
                                   providerPK: PublicKey25519Proposition,
                                   timestamp: Instant,
                                   term: EhrContractTerm)
  extends EhrContract {

  override def validity: Either[Throwable, Boolean] = term match {
    case Unlimited => Right[Throwable, Boolean](true)
    case ValidUntil(date) if timestamp.compareTo(date) < 0 => Right[Throwable, Boolean](true)
    case ValidUntil(date) if timestamp.compareTo(date) >= 0 =>
      Left[Throwable, Boolean](new Exception("contract ValidUntil date is less than contract's timestamp"))
  }
}
