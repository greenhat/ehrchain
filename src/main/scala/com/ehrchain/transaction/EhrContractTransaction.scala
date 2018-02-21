package com.ehrchain.transaction

import com.ehrchain.core.TimeStamp
import com.google.common.primitives.{Bytes, Longs}
import io.circe.Json
import scorex.core.serialization.Serializer
import scorex.core.transaction.box.proposition.PublicKey25519Proposition
import scorex.core.transaction.proof.Signature25519

final case class EhrContractTransaction(generator: PublicKey25519Proposition,
                                        signature: Signature25519,
                                        contract: EhrContract,
                                        timestamp: TimeStamp) extends EhrTransaction {
  override type M = this.type

  override def serializer: Serializer[M] = ???

  override def json: Json = ???

  override def validity: Boolean =
    super.validity && contract.validity.exists(_ == true)

  override val messageToSign: Array[Byte] =
    EhrContractTransaction.generateMessageToSign(timestamp, generator, contract)
}

object EhrContractTransaction {

  def generateMessageToSign(timestamp: TimeStamp,
                            generator: PublicKey25519Proposition,
                            contract: EhrContract
                            ): Array[Byte] =
    Bytes.concat(
      Longs.toByteArray(timestamp),
      generator.bytes,
      contract.bytes
    )
}
