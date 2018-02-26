package com.ehrchain.transaction

import java.time.Instant

import com.ehrchain.contract.{EhrAppendContract, EhrContractStorage, Unlimited, ValidUntil}

class EhrRecordTransactionContractValidator(contractStorage: EhrContractStorage) {

  def validity(tx: EhrRecordTransaction): Boolean =
    contractStorage.contractsForPatient(tx.subject).exists {
      case appendContract: EhrAppendContract => appendContract.term match {
        case Unlimited => true
        case ValidUntil(date) => date.compareTo(Instant.ofEpochSecond(tx.timestamp)) >= 0
      }
      case _ => false
    }
}
