package ehr.state

import ehr.block.EhrBlock
import ehr.contract.{Contract, ContractStorage}
import ehr.record.RecordFileStorage
import ehr.transaction._
import scorex.core.VersionTag
import scorex.core.transaction.box.proposition.PublicKey25519Proposition
import scorex.core.transaction.state.{MinimalState, ModifierValidation, TransactionValidation}
import scorex.core.utils.ScorexLogging

import scala.util.{Failure, Success, Try}

final case class EhrMinimalState(override val version: VersionTag,
                                 contractStorage: ContractStorage,
                                // todo remove
                                 recordFileStorage: RecordFileStorage,
                                 recordTransactionStorage: RecordTransactionStorage)
  extends MinimalState[EhrBlock, EhrMinimalState]
  with TransactionValidation[PublicKey25519Proposition, EhrTransaction]
  with ModifierValidation[EhrBlock]
  with ScorexLogging {
  self: EhrMinimalState =>

  override type NVCT = this.type

  private val ehrRecordTxContractValidator = new RecordTransactionContractValidator(contractStorage)

  override def applyModifier(mod: EhrBlock): Try[EhrMinimalState] =
    validate(mod).map { _ =>
      EhrMinimalState(VersionTag @@ mod.id,
        contractStorage.add(gatherContracts(mod.transactions)),
        recordFileStorage,
        recordTransactionStorage.put(
          mod.transactions.collect { case recTx: RecordTransaction => recTx }
        ))
    }

  private def gatherContracts(txs: Seq[EhrTransaction]): Seq[Contract] =
    txs.flatMap {
      case contractTx: ContractTransaction => Seq(contractTx.contract)
      case _ => Seq[Contract]()
    }

  override def rollbackTo(version: VersionTag): Try[EhrMinimalState] = ???

  override def validate(tx: EhrTransaction): Try[Unit] = Try {
    require(tx.semanticValidity, "tx semantic validation failed")
    tx match {
      case recordTx: RecordTransaction => {
        require(ehrRecordTxContractValidator.validity(recordTx), s"contract validation failed for: $recordTx")
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

  override def maxRollbackDepth: Int = ???
}
