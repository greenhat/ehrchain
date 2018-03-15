package ehr.transaction

import ehr.contract.{AppendContract, ContractStorage, Unlimited, ValidUntil}

class RecordTransactionContractValidator(contractStorage: ContractStorage) {

  def validity(tx: RecordTransaction): Boolean =
    contractStorage.contractsForPatient(tx.patient).exists {
      case appendContract: AppendContract => appendContract.term match {
        case Unlimited => true
        case ValidUntil(date) => date.compareTo(tx.timestamp) >= 0
      } // todo no revoke contract (timestamp | startDate) after appendContract.timestamp
      case _ => false
    }
}
