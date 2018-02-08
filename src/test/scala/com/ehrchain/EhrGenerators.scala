package com.ehrchain

import com.ehrchain.block.{EhrBlock, EhrBlockCompanion}
import com.ehrchain.core.{RecordType, TimeStamp}
import com.ehrchain.history.{EhrHistory, EhrHistoryStorage}
import com.ehrchain.transaction.{EhrTransaction, EhrTransactionCompanion}
import commons.ExamplesCommonGenerators
import org.scalacheck.{Arbitrary, Gen}
import scorex.testkit.generators.CoreGenerators
import com.ehrchain.mining.EhrMiningSettings
import scorex.core.block.Block.BlockId

import scala.annotation.tailrec


@SuppressWarnings(Array("org.wartremover.warts.OptionPartial", "org.wartremover.warts.TryPartial", "org.wartremover.warts.Recursion"))
trait EhrGenerators extends CoreGenerators
with ExamplesCommonGenerators {

  val MaxTransactionQtyInBlock: Int = 20

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
  } yield EhrBlockCompanion.generate(parentId, timestamp,transactions, generatorKeys)

  lazy val zeroTxsEhrBlockGen: Gen[EhrBlock] = for {
    timestamp <- timestampGen
    generatorKeys <- key25519Gen
    transactions <- ehrTransactionsGen(0, 0)
    parentId <- modifierIdGen
  } yield EhrBlockCompanion.generate(parentId, timestamp, transactions, generatorKeys)

  def generateGenesisBlock: EhrBlock = {
    val settings = new EhrMiningSettings()
    EhrBlockCompanion.generate(
      settings.GenesisParentId,
      timestampGen.sample.get,
      ehrTransactionsGen(1, MaxTransactionQtyInBlock).sample.get,
      key25519Gen.sample.get)
  }

  def generateBlock(parentId: BlockId): EhrBlock =
    EhrBlockCompanion.generate(
      parentId,
      timestampGen.sample.get,
      ehrTransactionsGen(1, MaxTransactionQtyInBlock).sample.get,
      key25519Gen.sample.get)

  def generateHistory(height: Int): EhrHistory = {
    val settings = new EhrMiningSettings()
    val storage = new EhrHistoryStorage(settings)
    val h = new EhrHistory(storage, settings)
    def addBlock(block: EhrBlock, history: EhrHistory): EhrHistory = {
      h.append(block).map{ case (history, progressInfo) =>
        if (history.height < height) (addBlock(generateBlock(block.id), history), progressInfo)
        else (history, progressInfo)
      }.get._1
    }
    addBlock(generateGenesisBlock, h)
  }
}
