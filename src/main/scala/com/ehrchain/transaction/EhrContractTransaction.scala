package com.ehrchain.transaction

import com.ehrchain.core.TimeStamp
import io.circe.Json
import scorex.core.serialization.Serializer
import scorex.core.transaction.box.proposition.PublicKey25519Proposition
import scorex.core.transaction.proof.Signature25519

final case class EhrContractTransaction(generator: PublicKey25519Proposition,
                                        signature: Signature25519,
                                        contract: EhrContract,
                                        timestamp: TimeStamp) extends EhrTransaction {
  override type M = this.type

  override def serializer: Serializer[EhrContractTransaction.this.type] = ???

  override def json: Json = ???

  override val messageToSign: Array[Byte] = ???
}
