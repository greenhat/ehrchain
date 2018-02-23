package com.ehrchain.transaction

import com.ehrchain.core.{Curve25519KeyPair, TimeStamp}
import com.ehrchain.serialization._
import com.google.common.primitives.{Bytes, Longs}
import io.circe.Json
import scorex.core.serialization.Serializer
import scorex.core.transaction.box.proposition.PublicKey25519Proposition
import scorex.core.transaction.proof.Signature25519
import scorex.core.transaction.state.PrivateKey25519Companion

@SerialVersionUID(0L)
final case class EhrContractTransaction(generator: PublicKey25519Proposition,
                                        signature: Signature25519,
                                        contract: EhrContract,
                                        timestamp: TimeStamp) extends EhrTransaction {
  override type M = this.type

  override def serializer: Serializer[M] = byteSerializer[M]

  override def json: Json = ???

  override def semanticValidity: Boolean =
    super.semanticValidity && contract.validity.exists(_ == true)

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

  def generate(generatorKeys: Curve25519KeyPair,
               contract: EhrContract,
               timestamp: TimeStamp): EhrContractTransaction = {
    val messageToSign = generateMessageToSign(timestamp, generatorKeys.publicKey, contract)
    val signature = PrivateKey25519Companion.sign(generatorKeys.privateKey, messageToSign)
    EhrContractTransaction(generatorKeys.publicKey, signature, contract, timestamp)
  }
}
