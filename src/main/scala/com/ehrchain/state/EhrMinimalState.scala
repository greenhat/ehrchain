package com.ehrchain.state

import com.ehrchain.block.EhrBlock
import com.ehrchain.contract.{EhrContract, EhrContractStorage}
import com.ehrchain.record.{InMemoryRecordFileStorage, RecordFileStorage}
import com.ehrchain.transaction._
import scorex.core.VersionTag
import scorex.core.transaction.box.proposition.PublicKey25519Proposition
import scorex.core.transaction.state.{MinimalState, ModifierValidation, TransactionValidation}
import scorex.core.utils.ScorexLogging

import scala.util.{Failure, Try}

final case class EhrMinimalState(override val version: VersionTag,
                                 contractStorage: EhrContractStorage,
                                 recordFileStorage: RecordFileStorage)
  extends MinimalState[EhrBlock, EhrMinimalState]
  with TransactionValidation[PublicKey25519Proposition, EhrTransaction]
  with ModifierValidation[EhrBlock]
  with ScorexLogging {
  self: EhrMinimalState =>

  override type NVCT = this.type

  private val ehrRecordTxContractValidator = new EhrRecordTransactionContractValidator(contractStorage)
  private val ehrRecordTxFileValidator = new RecordTransactionFileValidator(recordFileStorage)

  override def applyModifier(mod: EhrBlock): Try[EhrMinimalState] =
    validate(mod).map { _ =>
      EhrMinimalState(VersionTag @@ mod.id, contractStorage.add(
        mod.transactions.flatMap {
          case contractTx: EhrContractTransaction => Seq(contractTx.contract)
          case _ => Seq[EhrContract]()
        }
      ), recordFileStorage)
    }

  override def rollbackTo(version: VersionTag): Try[EhrMinimalState] = ???

  override def validate(tx: EhrTransaction): Try[Unit] = Try {
    require(tx.semanticValidity)
    require(
      tx match {
        case recordTx: EhrRecordTransaction =>
          ehrRecordTxContractValidator.validity(recordTx) && ehrRecordTxFileValidator.validity(recordTx)
      }
    )
  }

  override def validate(mod: EhrBlock): Try[Unit] = Try {
    // todo validate each tx in the block
    require(mod.parentId sameElements version)
  }.recoverWith{case t => log.warn(s"Not a valid modifier ${mod.encodedId}", t); Failure[Unit](t)}

  override def maxRollbackDepth: Int = ???
}
