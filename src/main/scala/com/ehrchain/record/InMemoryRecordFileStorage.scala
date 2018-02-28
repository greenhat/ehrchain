package com.ehrchain.record

import java.io.InputStream

import com.sun.xml.internal.messaging.saaj.util.ByteInputStream
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

object InMemoryRecordFileStorageMock {

  private val bytes = "mock file".getBytes
  val inputStream: InputStream = new ByteInputStream(bytes, bytes.length)
  val recordFile: RecordFile = RecordFile.generate(inputStream)

  val storage: RecordFileStorage = new InMemoryRecordFileStorage().put(recordFile, inputStream)
}

