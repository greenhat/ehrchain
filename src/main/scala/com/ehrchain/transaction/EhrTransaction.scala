package com.ehrchain.transaction

import java.time.Instant

import scorex.core.serialization.BytesSerializable
import scorex.core.transaction.Transaction
import scorex.core.transaction.box.proposition.PublicKey25519Proposition
import scorex.core.transaction.proof.Signature25519

@SuppressWarnings(Array("org.wartremover.warts.LeakingSealed"))
trait EhrTransaction extends Transaction[PublicKey25519Proposition] with BytesSerializable {

  val generator: PublicKey25519Proposition
  val signature: Signature25519
  val timestamp: Instant

  def semanticValidity: Boolean =
    signature.isValid(generator, messageToSign)
}
