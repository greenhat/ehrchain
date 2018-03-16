package ehr.transaction

import ehr.contract._

class RecordTransactionContractValidator(contractStorage: ContractStorage) {

  def validity(tx: RecordTransaction): Boolean =
    activeAppendContractsFor(tx).exists(!isRevoked(_))

  private def activeAppendContractsFor(tx: RecordTransaction): Seq[AppendContract] =
    contractStorage.contractsForPatient[AppendContract](tx.patient, tx.generator)
      .filter { appendContract =>
        appendContract.term match {
          case Unlimited => true
          case ValidUntil(date) => date.compareTo(tx.timestamp) >= 0
        }
      }

  private def isRevoked(appendContract: AppendContract): Boolean =
    contractStorage.contractsForPatient[RevokeAppendContract](appendContract.patientPK, appendContract.providerPK)
    .exists(revoke => revoke.startDate.isAfter(appendContract.timestamp)
      || revoke.timestamp.isAfter(appendContract.timestamp))

}
