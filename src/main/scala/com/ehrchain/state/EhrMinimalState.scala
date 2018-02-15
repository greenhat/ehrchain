package com.ehrchain.state

import com.ehrchain.block.EhrBlock
import com.ehrchain.transaction.EhrTransaction
import scorex.core.VersionTag
import scorex.core.transaction.box.proposition.PublicKey25519Proposition
import scorex.core.transaction.state.{MinimalState, ModifierValidation, TransactionValidation}

import scala.util.Try

final case class EhrMinimalState() extends MinimalState[EhrBlock, EhrMinimalState]
  with TransactionValidation[PublicKey25519Proposition, EhrTransaction]
  with ModifierValidation[EhrBlock] {
  self: EhrMinimalState =>

  override type NVCT = this.type

  override def applyModifier(mod: EhrBlock): Try[EhrMinimalState] = ???

  override def rollbackTo(version: VersionTag): Try[EhrMinimalState] = ???

  override def validate(tx: EhrTransaction): Try[Unit] = ???

  override def validate(mod: EhrBlock): Try[Unit] = ???

  override def version: VersionTag = ???

  override def maxRollbackDepth: Int = ???

}
