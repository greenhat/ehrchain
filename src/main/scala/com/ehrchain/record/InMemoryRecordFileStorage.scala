package com.ehrchain.record

import com.ehrchain.core.DigestSha256

@SuppressWarnings(Array("org.wartremover.warts.DefaultArguments"))
final class InMemoryRecordFileStorage(store: Map[DigestSha256, RecordFileSource] =
                                      Map[DigestSha256, RecordFileSource]()
                                     ) extends RecordFileStorage {

  override def get(recordFile: RecordFile): Option[RecordFileSource] =
    store.get(recordFile.hash)

  override def put(recordFile: RecordFile, source: RecordFileSource): RecordFileStorage =
    new InMemoryRecordFileStorage(
      store + (recordFile.hash -> source))
}

@SuppressWarnings(Array("org.wartremover.warts.TryPartial"))
object InMemoryRecordFileStorageMock {

  private val bytes = "mock file".getBytes
  val recordFile: RecordFile = RecordFile.generate(bytes).get

  val storage: RecordFileStorage = new InMemoryRecordFileStorage().put(recordFile, bytes)
}

