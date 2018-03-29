package ehr.transaction

import ehr.crypto.Sha256
import ehr.record.{FileHash, RecordFileStorage}

class RecordTransactionFileValidator(recordFileStorage: RecordFileStorage) {

  def validity(tx: RecordTransaction): Boolean = tx.record.files.forall { recordFile =>
    recordFileStorage.get(recordFile)
      .exists(fileSource => Sha256.digest(fileSource.inputStream)
        .map(hash => hash == recordFile.hash).getOrElse(false)
      )
  }

  def findMissingFiles(txs: Seq[RecordTransaction]): Seq[FileHash] =
    txs.flatMap(_.record.files.filter(recordFileStorage.get(_).isEmpty))

}
