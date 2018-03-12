package ehr

import java.time.Instant

import commons.ExamplesCommonGenerators
import ehr.block.EhrBlock
import ehr.contract.{AppendContract, Unlimited}
import ehr.crypto.Curve25519KeyPair
import ehr.history.BlockStream._
import ehr.history.{BlockStream, HistoryStorage}
import ehr.record._
import ehr.transaction.{ContractTransaction, RecordTransaction, EhrRecordTransactionCompanion, EhrTransaction}
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

  def ehrAppendContractUnlimitedGen: Gen[AppendContract] = for {
    patientPK <- propositionGen
    providerPK <- propositionGen
    timestamp <- instantGen
  } yield AppendContract(patientPK, providerPK, timestamp, Unlimited)

  lazy val timestampGen: Gen[Instant] = instantGen

  lazy val instantGen: Gen[Instant] =
    Gen.choose(Instant.EPOCH.getEpochSecond, Instant.now.getEpochSecond)
      .map(Instant.ofEpochSecond)

  def ehrTransactionPairGen: Gen[List[EhrTransaction]] = for {
    timestamp <- timestampGen
    providerKeys <- key25519Gen
    patientKeys <- key25519Gen
  } yield List[EhrTransaction](
    ContractTransaction.generate(
      patientKeys,
      AppendContract(patientKeys._2, providerKeys._2, timestamp, Unlimited),
      timestamp),
    EhrRecordTransactionCompanion.generate(patientKeys._2, providerKeys, mockRecord, timestamp)
  )

  lazy val ehrRecordTransactionGen: Gen[RecordTransaction] = for {
    timestamp <- timestampGen
    providerKeys <- key25519Gen
    patientPK <- propositionGen
  } yield EhrRecordTransactionCompanion.generate(patientPK, providerKeys, mockRecord, timestamp)

  lazy val ehrAppendContractTransactionGen: Gen[ContractTransaction] = for {
    timestamp <- timestampGen
    generatorKeys <- key25519Gen
    appendContract <- ehrAppendContractUnlimitedGen
  } yield ContractTransaction.generate(generatorKeys, appendContract, timestamp)

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
      BlockStream.GenesisParentId,
      timestampGen.sample.get,
      Seq(ehrAppendContractTransactionGen.sample.get),
      key25519Gen.sample.get, MiningDifficulty)

  def generateBlock(parentId: BlockId): EhrBlock =
    EhrBlock.generate(
      parentId,
      timestampGen.sample.get,
      ehrTransactionsGen(2, MaxTransactionQtyInBlock).sample.get,
      key25519Gen.sample.get, MiningDifficulty)

  def generateBlockStream(height: Int): BlockStream = {
    require(height > 0)
    def blockList(block: EhrBlock, elements: List[EhrBlock]): List[EhrBlock] = {
      if (elements.lengthCompare(height) < 0)
        blockList(generateBlock(block.id), elements :+ block)
      else
        elements
    }
    blockStreamFromElements(blockList(generateGenesisBlock, List()))(new HistoryStorage())
  }

  @SuppressWarnings(Array("org.wartremover.warts.ImplicitParameter"))
  def blockStreamFromElements(blocks: List[EhrBlock])(implicit storage: HistoryStorage): BlockStream = {
    def loop(stream: BlockStream, rest: List[EhrBlock]): BlockStream = rest match {
      case h :: t => loop(stream.append(h).get._1, t)
      case Nil => stream
    }
    loop(empty, blocks)
  }

  def currentTimestamp: Instant = Instant.now

  def validBlockstream: BlockStream = {
    val genesisBlock = generateGenesisBlock
    val patientKeyPair: Curve25519KeyPair = key25519Gen.sample.get
    val providerKeyPair: Curve25519KeyPair = key25519Gen.sample.get
    val blockGeneratorKeyPair: Curve25519KeyPair = key25519Gen.sample.get
    val appendContract = AppendContract(patientKeyPair.publicKey,
      providerKeyPair.publicKey,
      Instant.now,
      Unlimited)
    val block1TXs = Seq(ContractTransaction.generate(patientKeyPair, appendContract, currentTimestamp))
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
    blockStreamFromElements(List(genesisBlock, block1, block2))(new HistoryStorage())
  }
}
