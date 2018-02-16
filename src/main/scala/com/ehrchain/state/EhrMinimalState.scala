package com.ehrchain.state

import com.ehrchain.block.EhrBlock
import com.ehrchain.transaction.EhrTransaction
import scorex.core.VersionTag
import scorex.core.transaction.box.proposition.PublicKey25519Proposition
import scorex.core.transaction.state.{MinimalState, ModifierValidation, TransactionValidation}
import scorex.core.utils.ScorexLogging

import scala.util.{Failure, Success, Try}

final case class EhrMinimalState(override val version: VersionTag) extends MinimalState[EhrBlock, EhrMinimalState]
  with TransactionValidation[PublicKey25519Proposition, EhrTransaction]
  with ModifierValidation[EhrBlock]
  with ScorexLogging {
  self: EhrMinimalState =>

  override type NVCT = this.type

  override def applyModifier(mod: EhrBlock): Try[EhrMinimalState] =
    validate(mod).map(_ => EhrMinimalState(VersionTag @@ mod.id))

  override def rollbackTo(version: VersionTag): Try[EhrMinimalState] = ???

  override def validate(tx: EhrTransaction): Try[Unit] = Try {
    require(tx.validity)
  }

  override def validate(mod: EhrBlock): Try[Unit] = Try {
    require(mod.parentId sameElements version)
  }.recoverWith{case t => log.warn(s"Not valid modifier ${mod.encodedId}", t); Failure[Unit](t)}

  override def maxRollbackDepth: Int = ???
}
