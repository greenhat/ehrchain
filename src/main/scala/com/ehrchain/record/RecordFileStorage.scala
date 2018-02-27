package com.ehrchain.record

import java.io.InputStream

trait RecordFileStorage {

  def get(recordFile: RecordFile): Option[InputStream]
  def put(recordFile: RecordFile, inputStream: InputStream): RecordFileStorage
}

final class InMemoryRecordFileStorage() extends RecordFileStorage {

  override def get(recordFile: RecordFile): Option[InputStream] = ???

  override def put(recordFile: RecordFile, inputStream: InputStream): RecordFileStorage = ???
}
