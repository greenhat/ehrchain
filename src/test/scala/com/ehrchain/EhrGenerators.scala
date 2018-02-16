package com.ehrchain

import com.ehrchain.block.{EhrBlock, EhrBlockCompanion}
import com.ehrchain.core.{RecordType, TimeStamp}
import com.ehrchain.history.EhrBlockStream._
import com.ehrchain.history.{EhrBlockStream, EhrBlockStreamElement, EhrHistoryStorage}
import com.ehrchain.mining.EhrMiningSettings
import com.ehrchain.transaction.{EhrTransaction, EhrTransactionCompanion}
import commons.ExamplesCommonGenerators
import org.scalacheck.{Arbitrary, Gen}
import scorex.core.block.Block.BlockId
import scorex.testkit.generators.CoreGenerators


@SuppressWarnings(Array("org.wartremover.warts.OptionPartial", "org.wartremover.warts.TryPartial", "org.wartremover.warts.Recursion"))
trait EhrGenerators extends CoreGenerators
with ExamplesCommonGenerators {

  val MaxTransactionQtyInBlock: Int = 20

  val MiningDifficulty: Int = 0

  def genRecord(minSize: Int, maxSize: Int): Gen[RecordType] = {
    Gen.choose(minSize, maxSize) flatMap { sz =>
      Gen.listOfN(sz, Arbitrary.arbitrary[Byte]).map(RecordType @@  _.toArray)
    }
  }

  lazy val timestampGen: Gen[TimeStamp] = Gen.choose(1, Long.MaxValue).map(TimeStamp @@ _)

  lazy val ehrTransactionGen: Gen[EhrTransaction] = for {
    timestamp <- timestampGen
    providerKeys <- key25519Gen
    patientPK <- propositionGen
    record <- genRecord(1, EhrTransaction.MaxRecordSize)
  } yield EhrTransactionCompanion.generate(patientPK, providerKeys, record, timestamp)

  def ehrTransactionsGen(min: Int, max: Int): Gen[List[EhrTransaction]] = for {
    txs <- Gen.choose(min, max).flatMap(i => Gen.listOfN(i, ehrTransactionGen))
  } yield txs

  lazy val emptyRecordEhrTransactionGen: Gen[EhrTransaction] = for {
    timestamp <- timestampGen
    providerKeys <- key25519Gen
    patientPK <- propositionGen
    record <- genRecord(0, 0)
  } yield EhrTransactionCompanion.generate(patientPK, providerKeys, record, timestamp)

  lazy val ehrBlockGen: Gen[EhrBlock] = for {
    timestamp <- timestampGen
    generatorKeys <- key25519Gen
    transactions <- ehrTransactionsGen(1, MaxTransactionQtyInBlock)
    parentId <- modifierIdGen
  } yield EhrBlockCompanion.generate(parentId, timestamp,transactions, generatorKeys, MiningDifficulty)

  lazy val zeroTxsEhrBlockGen: Gen[EhrBlock] = for {
    timestamp <- timestampGen
    generatorKeys <- key25519Gen
    transactions <- ehrTransactionsGen(0, 0)
    parentId <- modifierIdGen
  } yield EhrBlockCompanion.generate(parentId, timestamp, transactions, generatorKeys, MiningDifficulty)

  def generateGenesisBlock: EhrBlock = {
    EhrBlockCompanion.generate(
      EhrBlockStream.GenesisParentId,
      timestampGen.sample.get,
      ehrTransactionsGen(1, MaxTransactionQtyInBlock).sample.get,
      key25519Gen.sample.get, MiningDifficulty)
  }

  def generateBlock(parentId: BlockId): EhrBlock =
    EhrBlockCompanion.generate(
      parentId,
      timestampGen.sample.get,
      ehrTransactionsGen(1, MaxTransactionQtyInBlock).sample.get,
      key25519Gen.sample.get, MiningDifficulty)

  def generateBlockStream(height: Int): EhrBlockStream = {
    val storage = new EhrHistoryStorage(new EhrMiningSettings())
    def blockList(element: EhrBlockStreamElement, elements: List[EhrBlockStreamElement]): List[EhrBlockStreamElement] = {
      if (elements.lengthCompare(height) < 0)
        blockList(EhrBlockStreamElement(generateBlock(element.block.id), element.blockHeight + 1), elements :+ element)
      else
        elements
    }
    val reversedBlockList = blockList(EhrBlockStreamElement(generateGenesisBlock, 1), List())
    blockStreamFromElements(reversedBlockList.reverse)(storage)
  }

  @SuppressWarnings(Array("org.wartremover.warts.ImplicitParameter"))
  def blockStreamFromElements(elements: List[EhrBlockStreamElement])(implicit storage: EhrHistoryStorage): EhrBlockStream = {
    def loop(rest: List[EhrBlockStreamElement]): EhrBlockStream = rest match {
      case h :: t => cons(h, loop(t))
      case _ => empty
    }
    loop(elements)
  }
}
