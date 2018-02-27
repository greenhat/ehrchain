package com.ehrchain.transaction

import com.ehrchain.record.RecordFileStorage

class RecordTransactionFileValidator(recordFileStorage: RecordFileStorage) {

  def validity(tx: EhrRecordTransaction): Boolean = tx.record.files.forall { recordFile =>
    // todo check if file has the same hash
    recordFileStorage.get(recordFile).isDefined
  }
}
