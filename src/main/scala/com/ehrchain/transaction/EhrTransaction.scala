package com.ehrchain.transaction

import com.ehrchain.core.TimeStamp
import scorex.core.serialization.BytesSerializable
import scorex.core.transaction.Transaction
import scorex.core.transaction.box.proposition.PublicKey25519Proposition
import scorex.core.transaction.proof.Signature25519


//sealed trait EhrTransactionKind
//case object AppendContract extends EhrTransactionKind
//case object AppendRecord extends EhrTransactionKind
//case object ReadContract extends EhrTransactionKind
//case object RevokeAppendContract extends EhrTransactionKind


@SuppressWarnings(Array("org.wartremover.warts.LeakingSealed"))
trait EhrTransaction extends Transaction[PublicKey25519Proposition] with BytesSerializable {

  val generator: PublicKey25519Proposition
  val signature: Signature25519
  // todo switch to Instant
  val timestamp: TimeStamp

  def semanticValidity: Boolean =
    timestamp > 0 && signature.isValid(generator, messageToSign)
}
