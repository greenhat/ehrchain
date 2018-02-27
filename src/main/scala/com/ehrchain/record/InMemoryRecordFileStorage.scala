package com.ehrchain.record

import java.io.InputStream

import scorex.crypto.encode.Base58

@SuppressWarnings(Array("org.wartremover.warts.DefaultArguments"))
final class InMemoryRecordFileStorage(store: Map[String, InputStream] = Map[String, InputStream]()
                                     ) extends RecordFileStorage {

  override def get(recordFile: RecordFile): Option[InputStream] =
    store.get(Base58.encode(recordFile.hash))

  override def put(recordFile: RecordFile, inputStream: InputStream): RecordFileStorage =
    new InMemoryRecordFileStorage(
      store + (Base58.encode(recordFile.hash) -> inputStream))
}

