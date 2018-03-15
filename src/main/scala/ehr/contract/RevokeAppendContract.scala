package ehr.contract

import java.time.Instant

import ehr.serialization._
import scorex.core.serialization.Serializer
import scorex.core.transaction.box.proposition.PublicKey25519Proposition

import scala.util.Try

@SerialVersionUID(0L)
final case class RevokeAppendContract(patientPK: PublicKey25519Proposition,
                                      providerPK: PublicKey25519Proposition,
                                      timestamp: Instant,
                                      startDate: Instant) extends Contract {
  override type M = RevokeAppendContract

  override def serializer: Serializer[M] = byteSerializer[M]

  override def semanticValidity: Try[Unit] = Try {
    require(timestamp.compareTo(startDate) <= 0)
  }
}

