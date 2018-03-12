package ehr.transaction

import ehr.contract.{EhrAppendContract, EhrContractStorage, Unlimited, ValidUntil}

class EhrRecordTransactionContractValidator(contractStorage: EhrContractStorage) {

  def validity(tx: EhrRecordTransaction): Boolean =
    contractStorage.contractsForPatient(tx.subject).exists {
      case appendContract: EhrAppendContract => appendContract.term match {
        case Unlimited => true
        case ValidUntil(date) => date.compareTo(tx.timestamp) >= 0
      }
      case _ => false
    }
}
