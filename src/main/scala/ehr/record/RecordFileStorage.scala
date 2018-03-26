package ehr.record

trait RecordFileStorage {

  def get(recordFile: FileHash): Option[FileSource]
  def put(recordFile: FileHash, source: FileSource): RecordFileStorage
}

