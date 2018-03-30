package ehr.record

import ehr.core.DigestSha256

import scala.collection.mutable

@SuppressWarnings(Array("org.wartremover.warts.DefaultArguments",
  "org.wartremover.warts.MutableDataStructures"))
final class InMemoryRecordFileStorage() extends RecordFileStorage {

  val store: mutable.Map[DigestSha256, FileSource] = mutable.Map[DigestSha256, FileSource]()

  override def get(recordFile: FileHash): Option[FileSource] =
    store.get(recordFile.hash)

  override def put(recordFile: FileHash, source: FileSource): RecordFileStorage = {
    store.update(recordFile.hash, source)
    this
  }
}

@SuppressWarnings(Array("org.wartremover.warts.TryPartial"))
object InMemoryRecordFileStorageMock {

  val recordFileBytes: Array[Byte] = "mock file".getBytes
  val recordFileHash: FileHash = FileHash.generate(recordFileBytes).get

  val storage: RecordFileStorage = new InMemoryRecordFileStorage().put(recordFileHash, recordFileBytes)
}

