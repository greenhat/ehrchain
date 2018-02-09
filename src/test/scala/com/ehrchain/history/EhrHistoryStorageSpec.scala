package com.ehrchain.history

import com.ehrchain.EhrGenerators
import com.ehrchain.mining.EhrMiningSettings
import org.scalatest.{FlatSpec, Matchers}

class EhrHistoryStorageSpec extends FlatSpec
  with Matchers
  with EhrGenerators {

  private def newStorage: EhrHistoryStorage = new EhrHistoryStorage(new EhrMiningSettings())

  "new storage" should "be empty" in {
    val storage = newStorage
    storage.height shouldEqual 0
  }

  it should "have heightOf after genesis block" in {
    val storage = newStorage
    val genesisBlock = generateGenesisBlock
    storage.update(genesisBlock)
    storage.heightOf(genesisBlock.id) shouldEqual Some(1)
  }

  it should "have height after genesis block" in {
    val storage = newStorage
    storage.update(generateGenesisBlock)
    storage.height shouldEqual 1
  }

}
