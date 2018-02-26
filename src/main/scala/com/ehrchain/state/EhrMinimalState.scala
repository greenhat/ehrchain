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
  // todo scan for new contract txs (put into the contract store)

  override def rollbackTo(version: VersionTag): Try[EhrMinimalState] = ???

  override def validate(tx: EhrTransaction): Try[Unit] = Try {
    // todo validate record transaction by verifying it against a valid contract
    require(tx.semanticValidity)
  }

  override def validate(mod: EhrBlock): Try[Unit] = Try {
    // todo validate each tx in the block
    require(mod.parentId sameElements version)
  }.recoverWith{case t => log.warn(s"Not a valid modifier ${mod.encodedId}", t); Failure[Unit](t)}

  override def maxRollbackDepth: Int = ???
}
