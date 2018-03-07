package com.ehrchain.record

trait RecordFileStorage {

  def get(recordFile: RecordFile): Option[RecordFileSource]
  def put(recordFile: RecordFile, source: RecordFileSource): RecordFileStorage
}

