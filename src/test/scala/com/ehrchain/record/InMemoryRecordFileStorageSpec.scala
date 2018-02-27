package com.ehrchain.record

import com.ehrchain.EhrGenerators
import org.scalatest.{Matchers, PropSpec}
import org.scalatest.prop.{GeneratorDrivenPropertyChecks, PropertyChecks}

class InMemoryRecordFileStorageSpec extends PropSpec
  with PropertyChecks
  with GeneratorDrivenPropertyChecks
  with Matchers
  with EhrGenerators {

    property("put and get") {
      val storage = new InMemoryRecordFileStorage()
      forAll(genRecordFile(10, 100)) { case (recordFile, inputStream) =>
        storage.put(recordFile, inputStream).get(recordFile) shouldEqual Some(inputStream)
      }
    }

}
