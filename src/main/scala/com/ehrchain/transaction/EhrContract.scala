package com.ehrchain.transaction

import java.time.Instant

import scorex.core.serialization.{BytesSerializable, Serializer}
import scorex.core.transaction.box.proposition.PublicKey25519Proposition

import scala.util.Try

trait EhrContractTerm {}
case object Unlimited extends EhrContractTerm
final case class ValidUntil(date: Instant) extends EhrContractTerm

trait EhrContract extends BytesSerializable {

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
                                   term: EhrContractTerm) extends EhrContract {
  override type M = EhrAppendContract

  override def serializer: Serializer[M] = EhrAppendContractSerializer
}

object EhrAppendContractSerializer extends Serializer[EhrAppendContract] {

  override def toBytes(obj: EhrAppendContract): Array[Byte] = ???

  override def parseBytes(bytes: Array[Byte]): Try[EhrAppendContract] = ???
}
