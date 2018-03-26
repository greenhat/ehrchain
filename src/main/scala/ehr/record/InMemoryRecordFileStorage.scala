package ehr.record

import ehr.core.DigestSha256

@SuppressWarnings(Array("org.wartremover.warts.DefaultArguments"))
final class InMemoryRecordFileStorage(store: Map[DigestSha256, FileSource] =
                                      Map[DigestSha256, FileSource]()
                                     ) extends RecordFileStorage {

  override def get(recordFile: FileHash): Option[FileSource] =
    store.get(recordFile.hash)

  override def put(recordFile: FileHash, source: FileSource): RecordFileStorage =
    new InMemoryRecordFileStorage(
      store + (recordFile.hash -> source))
}

@SuppressWarnings(Array("org.wartremover.warts.TryPartial"))
object InMemoryRecordFileStorageMock {

  private val bytes = "mock file".getBytes
  val recordFile: FileHash = FileHash.generate(bytes).get

  val storage: RecordFileStorage = new InMemoryRecordFileStorage().put(recordFile, bytes)
}

