package com.ehrchain.transaction

import com.ehrchain.crypto.Sha256
import com.ehrchain.record.RecordFileStorage

class RecordTransactionFileValidator(recordFileStorage: RecordFileStorage) {

  def validity(tx: EhrRecordTransaction): Boolean = tx.record.files.forall { recordFile =>
    recordFileStorage.get(recordFile)
      .exists(fileSource => Sha256.digest(fileSource.inputStream)
        .map(hash => hash == recordFile.hash).getOrElse(false)
      )
  }
}
