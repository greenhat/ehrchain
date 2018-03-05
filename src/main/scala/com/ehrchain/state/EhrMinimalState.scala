package com.ehrchain.state

import com.ehrchain.block.EhrBlock
import com.ehrchain.contract.{EhrContract, EhrContractStorage}
import com.ehrchain.record.{InMemoryRecordFileStorage, RecordFileStorage}
import com.ehrchain.transaction._
import scorex.core.VersionTag
import scorex.core.transaction.box.proposition.PublicKey25519Proposition
import scorex.core.transaction.state.{MinimalState, ModifierValidation, TransactionValidation}
import scorex.core.utils.ScorexLogging

import scala.util.{Failure, Success, Try}

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
      EhrMinimalState(VersionTag @@ mod.id,
        contractStorage.add(gatherContracts(mod.transactions)),
        recordFileStorage)
    }

  private def gatherContracts(txs: Seq[EhrTransaction]): Seq[EhrContract] =
    txs.flatMap {
      case contractTx: EhrContractTransaction => Seq(contractTx.contract)
      case _ => Seq[EhrContract]()
    }

  override def rollbackTo(version: VersionTag): Try[EhrMinimalState] = ???

  override def validate(tx: EhrTransaction): Try[Unit] = Try {
    require(tx.semanticValidity, "tx semantic validation failed")
    tx match {
      case recordTx: EhrRecordTransaction => {
        require(ehrRecordTxContractValidator.validity(recordTx), s"contract validation failed for: $recordTx")
        require(ehrRecordTxFileValidator.validity(recordTx), s"file validation failed for: $recordTx")
      }
      case _ => ()
    }
  }

  override def validate(mod: EhrBlock): Try[Unit] =
    Try {
    require(mod.parentId sameElements version, "modifier's parentId != version")
    }.map { _ =>
      mod.transactions.map(tx => validate(tx)).find(_.isFailure).getOrElse(Success())
    }.flatten
//    require(mod.transactions.forall(tx => validate(tx).isSuccess), "transactions validation failed")

  override def maxRollbackDepth: Int = ???
}
