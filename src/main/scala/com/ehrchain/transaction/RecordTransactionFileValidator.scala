package com.ehrchain.transaction

import com.ehrchain.record.RecordFileStorage

class RecordTransactionFileValidator(recordFileStorage: RecordFileStorage) {

  def validity(tx: EhrRecordTransaction): Boolean = true
  // todo check if file is in storage and has the same hash
}
