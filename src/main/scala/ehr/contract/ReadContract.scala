package ehr.contract

import java.time.Instant

import ehr.core.EncryptedAes256Keys
import ehr.serialization._
import scorex.core.serialization.Serializer
import scorex.core.transaction.box.proposition.PublicKey25519Proposition

import scala.util.Try

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

