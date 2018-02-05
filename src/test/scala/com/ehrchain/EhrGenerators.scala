package com.ehrchain

import com.ehrchain.block.{EhrBlock, EhrBlockCompanion}
import com.ehrchain.core.{RecordType, TimeStamp}
import com.ehrchain.transaction.{EhrTransaction, EhrTransactionCompanion}
import commons.ExamplesCommonGenerators
import org.scalacheck.{Arbitrary, Gen}
import scorex.testkit.generators.CoreGenerators


trait EhrGenerators extends CoreGenerators
with ExamplesCommonGenerators {

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

  lazy val ehrTransactionsGen: Gen[List[EhrTransaction]] = for {
    txs <- smallInt.flatMap(i => Gen.listOfN(i, ehrTransactionGen))
  } yield txs

  lazy val invalidEhrTransactionGen: Gen[EhrTransaction] = for {
    timestamp <- Gen.choose[Long](0, 0).map(TimeStamp @@ _)
    providerKeys <- key25519Gen
    patientPK <- propositionGen
    record <- genRecord(0, EhrTransaction.MaxRecordSize * 2)
  } yield EhrTransactionCompanion.generate(patientPK, providerKeys, record, timestamp)

  lazy val ehrBlockGen: Gen[EhrBlock] = for {
    timestamp <- timestampGen
    generatorKeys <- key25519Gen
    transactions <- ehrTransactionsGen
    nonce <- nonceGen
    parentId <- modifierIdGen
  } yield EhrBlockCompanion.generate(parentId, timestamp, nonce, transactions, generatorKeys)
}
