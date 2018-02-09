package com.ehrchain.history

import com.ehrchain.mining.EhrMiningSettings
import org.scalatest.{FlatSpec, Matchers}

class EhrHistoryStorageSpec extends FlatSpec
  with Matchers {

  private def newStorage: EhrHistoryStorage = new EhrHistoryStorage(new EhrMiningSettings())

  "new storage" should "be empty" in {
    val storage = newStorage
    storage.height shouldEqual 0
  }

}
