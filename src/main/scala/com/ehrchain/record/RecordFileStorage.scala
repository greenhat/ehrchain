package com.ehrchain.record

trait RecordFileStorage {

  def get(recordFile: RecordFile): Option[FileSource]
  def put(recordFile: RecordFile, source: FileSource): RecordFileStorage
}

