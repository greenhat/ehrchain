package ehr.transaction

import ehr.crypto.Sha256
import ehr.record.RecordFileStorage

class RecordTransactionFileValidator(recordFileStorage: RecordFileStorage) {

  def validity(tx: EhrRecordTransaction): Boolean = tx.record.files.forall { recordFile =>
    recordFileStorage.get(recordFile)
      .exists(fileSource => Sha256.digest(fileSource.inputStream)
        .map(hash => hash == recordFile.hash).getOrElse(false)
      )
  }
}
