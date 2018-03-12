package com.ehrchain

import java.io.{ByteArrayInputStream, InputStream}
import java.time.Instant

import com.ehrchain.block.EhrBlock
import com.ehrchain.contract.{EhrAppendContract, Unlimited}
import com.ehrchain.core.TimeStamp
import com.ehrchain.crypto.Curve25519KeyPair
import com.ehrchain.history.EhrBlockStream._
import com.ehrchain.history.{EhrBlockStream, EhrHistoryStorage}
import com.ehrchain.record._
import com.ehrchain.transaction._
import commons.ExamplesCommonGenerators
import org.scalacheck.{Arbitrary, Gen}
import scorex.core.block.Block.BlockId
import scorex.core.transaction.state.PrivateKey25519Companion
import scorex.crypto.signatures.Curve25519
import scorex.testkit.generators.CoreGenerators


@SuppressWarnings(Array("org.wartremover.warts.OptionPartial", "org.wartremover.warts.TryPartial", "org.wartremover.warts.Recursion"))
trait EhrGenerators extends CoreGenerators
with ExamplesCommonGenerators {

  val MaxTransactionQtyInBlock: Int = 20

  val MiningDifficulty: Int = 0

  val mockRecord: Record = Record(Seq(InMemoryRecordFileStorageMock.recordFile))

  def genRecordFile(minSize: Int, maxSize: Int): Gen[(RecordFile, FileSource)] =
    Gen.choose(minSize, maxSize) flatMap { sz =>
      Gen.listOfN(sz, Arbitrary.arbitrary[Byte]).map { b =>
        val recordFileSource = ByteArrayFileSource(b.toArray)
        (RecordFile.generate(recordFileSource).get, recordFileSource)
      }
    }

  def genBytes(minSize: Int, maxSize: Int): Gen[Array[Byte]] =
    Gen.choose(minSize, maxSize) flatMap { sz =>
      Gen.listOfN(sz, Arbitrary.arbitrary[Byte]).map(_.toArray)
    }

  def ehrAppendContractUnlimitedGen: Gen[EhrAppendContract] = for {
    patientPK <- propositionGen
    providerPK <- propositionGen
    timestamp <- instantGen
  } yield EhrAppendContract(patientPK, providerPK, timestamp, Unlimited)

  lazy val timestampGen: Gen[TimeStamp] =
    Gen.choose(Instant.EPOCH.getEpochSecond, Instant.now.getEpochSecond).map(TimeStamp @@ _)

  lazy val instantGen: Gen[Instant] =
    Gen.choose(Instant.EPOCH.getEpochSecond, Instant.now.getEpochSecond)
      .map(Instant.ofEpochSecond)

  def ehrTransactionPairGen: Gen[List[EhrTransaction]] = for {
    timestamp <- timestampGen
    providerKeys <- key25519Gen
    patientKeys <- key25519Gen
  } yield List[EhrTransaction](
    EhrContractTransaction.generate(
      patientKeys,
      EhrAppendContract(patientKeys._2, providerKeys._2, Instant.ofEpochSecond(timestamp), Unlimited),
      timestamp),
    EhrRecordTransactionCompanion.generate(patientKeys._2, providerKeys, mockRecord, timestamp)
  )

  lazy val ehrRecordTransactionGen: Gen[EhrRecordTransaction] = for {
    timestamp <- timestampGen
    providerKeys <- key25519Gen
    patientPK <- propositionGen
  } yield EhrRecordTransactionCompanion.generate(patientPK, providerKeys, mockRecord, timestamp)

  lazy val ehrAppendContractTransactionGen: Gen[EhrContractTransaction] = for {
    timestamp <- timestampGen
    generatorKeys <- key25519Gen
    appendContract <- ehrAppendContractUnlimitedGen
  } yield EhrContractTransaction.generate(generatorKeys, appendContract, timestamp)

  def ehrTransactionsGen(min: Int, max: Int): Gen[List[EhrTransaction]] = for {
    txs <- Gen.choose(min, max).flatMap(i => Gen.listOfN(i / 2, ehrTransactionPairGen))
  } yield txs.flatten

  lazy val ehrBlockGen: Gen[EhrBlock] = for {
    timestamp <- timestampGen
    generatorKeys <- key25519Gen
    transactions <- ehrTransactionsGen(2, MaxTransactionQtyInBlock)
    parentId <- modifierIdGen
  } yield EhrBlock.generate(parentId, timestamp,transactions, generatorKeys, MiningDifficulty)

  lazy val key25519PairGen: Gen[Curve25519KeyPair] = genBytesList(Curve25519.KeyLength)
    .map(s => PrivateKey25519Companion.generateKeys(s))

  def generateGenesisBlock: EhrBlock =
    EhrBlock.generate(
      EhrBlockStream.GenesisParentId,
      timestampGen.sample.get,
      Seq(ehrAppendContractTransactionGen.sample.get),
      key25519Gen.sample.get, MiningDifficulty)

  def generateBlock(parentId: BlockId): EhrBlock =
    EhrBlock.generate(
      parentId,
      timestampGen.sample.get,
      ehrTransactionsGen(2, MaxTransactionQtyInBlock).sample.get,
      key25519Gen.sample.get, MiningDifficulty)

  def generateBlockStream(height: Int): EhrBlockStream = {
    require(height > 0)
    def blockList(block: EhrBlock, elements: List[EhrBlock]): List[EhrBlock] = {
      if (elements.lengthCompare(height) < 0)
        blockList(generateBlock(block.id), elements :+ block)
      else
        elements
    }
    blockStreamFromElements(blockList(generateGenesisBlock, List()))(new EhrHistoryStorage())
  }

  @SuppressWarnings(Array("org.wartremover.warts.ImplicitParameter"))
  def blockStreamFromElements(blocks: List[EhrBlock])(implicit storage: EhrHistoryStorage): EhrBlockStream = {
    def loop(stream: EhrBlockStream, rest: List[EhrBlock]): EhrBlockStream = rest match {
      case h :: t => loop(stream.append(h).get._1, t)
      case Nil => stream
    }
    loop(empty, blocks)
  }

  def currentTimestamp: TimeStamp = TimeStamp @@ Instant.now.toEpochMilli

  def validBlockstream: EhrBlockStream = {
    val genesisBlock = generateGenesisBlock
    val patientKeyPair: Curve25519KeyPair = key25519Gen.sample.get
    val providerKeyPair: Curve25519KeyPair = key25519Gen.sample.get
    val blockGeneratorKeyPair: Curve25519KeyPair = key25519Gen.sample.get
    val appendContract = EhrAppendContract(patientKeyPair.publicKey,
      providerKeyPair.publicKey,
      Instant.now,
      Unlimited)
    val block1TXs = Seq(EhrContractTransaction.generate(patientKeyPair, appendContract, currentTimestamp))
    val block1 = EhrBlock.generate(genesisBlock.id,
      currentTimestamp,
      block1TXs,
      blockGeneratorKeyPair,
      0)

    val block2TXs = Seq(EhrRecordTransactionCompanion.generate(
      patientKeyPair.publicKey,
      providerKeyPair,
      mockRecord,
      currentTimestamp))
    val block2 = EhrBlock.generate(block1.id,
      currentTimestamp,
      block2TXs,
      blockGeneratorKeyPair,
      0)
    blockStreamFromElements(List(genesisBlock, block1, block2))(new EhrHistoryStorage())
  }
}
