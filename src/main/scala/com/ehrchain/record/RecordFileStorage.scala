package com.ehrchain.record

import java.io.InputStream

trait RecordFileStorage {

  def get(recordFile: RecordFile): Option[InputStream]
  def put(recordFile: RecordFile, inputStream: InputStream): RecordFileStorage
}

