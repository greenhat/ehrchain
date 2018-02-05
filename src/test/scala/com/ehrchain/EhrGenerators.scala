package com.ehrchain

import com.ehrchain.core.RecordType
import com.ehrchain.transaction.{EhrTransaction, EhrTransactionCompanion}
import org.scalacheck.{Arbitrary, Gen}
import scorex.testkit.generators.CoreGenerators

trait EhrGenerators extends CoreGenerators {

  def genRecord(minSize: Int, maxSize: Int): Gen[RecordType] = {
    Gen.choose(minSize, maxSize) flatMap { sz =>
      Gen.listOfN(sz, Arbitrary.arbitrary[Byte]).map(RecordType @@  _.toArray)
    }
  }

  lazy val ehrTransactionGen: Gen[EhrTransaction] = for {
    timestamp <- positiveLongGen
    providerKeys <- key25519Gen
    patientPK <- propositionGen
    record <- genRecord(1, EhrTransaction.MaxRecordSize)
  } yield EhrTransactionCompanion.generate(patientPK, providerKeys, record, timestamp)

  lazy val invalidEhrTransactionGen: Gen[EhrTransaction] = for {
    timestamp <- Gen.choose[Long](0, 0)
    providerKeys <- key25519Gen
    patientPK <- propositionGen
    record <- genRecord(0, EhrTransaction.MaxRecordSize * 2)
  } yield EhrTransactionCompanion.generate(patientPK, providerKeys, record, timestamp)
}
