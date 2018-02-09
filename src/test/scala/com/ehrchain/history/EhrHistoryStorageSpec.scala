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
    storage.append(genesisBlock)
    storage.heightOf(genesisBlock.id) shouldEqual Some(1)
  }

  it should "have height after genesis block" in {
    val storage = newStorage
    storage.append(generateGenesisBlock)
    storage.height shouldEqual 1
  }

  it should "have heightOf after adding a block" in {
    val storage = newStorage
    val genesisBlock = generateGenesisBlock
    storage.append(genesisBlock)
    val block = generateBlock(genesisBlock.id)
    storage.append(block)
    storage.heightOf(block.id) shouldEqual Some(2)
  }

  it should "have height after adding a block" in {
    val storage = newStorage
    val genesisBlock = generateGenesisBlock
    storage.append(genesisBlock)
    val block = generateBlock(genesisBlock.id)
    storage.append(block)
    storage.height shouldEqual 2
  }

  it should "return added block" in {
    val storage = newStorage
    val genesisBlock = generateGenesisBlock
    storage.append(genesisBlock)
    val block = generateBlock(genesisBlock.id)
    storage.append(block)
    storage.modifierById(block.id).map(_.bytes) shouldEqual Some(block.bytes)
  }

  it should "have bestBlockId after added block" in {
    val storage = newStorage
    val genesisBlock = generateGenesisBlock
    storage.append(genesisBlock)
    val block = generateBlock(genesisBlock.id)
    storage.append(block)
    storage.bestBlockId.map(_.mkString) shouldEqual Some(block.id.mkString)
  }
}
