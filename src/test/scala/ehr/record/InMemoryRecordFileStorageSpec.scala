package ehr.record

import ehr.EhrGenerators
import org.scalatest.prop.{GeneratorDrivenPropertyChecks, PropertyChecks}
import org.scalatest.{Matchers, PropSpec}

class InMemoryRecordFileStorageSpec extends PropSpec
  with PropertyChecks
  with GeneratorDrivenPropertyChecks
  with Matchers
  with EhrGenerators {

    property("put and get") {
      val storage = new InMemoryRecordFileStorage()
      forAll(genRecordFile(10, 100)) { case (recordFile, inputStream) =>
        storage.put(recordFile, inputStream)
        storage.get(recordFile) shouldEqual Some(inputStream)
      }
    }

}
