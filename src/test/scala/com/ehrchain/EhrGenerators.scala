package com.ehrchain

import com.ehrchain.core.RecordType
import com.ehrchain.transaction.{EhrTransaction, EhrTransactionCompanion}
import org.scalacheck.{Arbitrary, Gen}
import scorex.testkit.generators.CoreGenerators

trait EhrGenerators extends CoreGenerators {

  // fixme random size
  lazy val recordTypeGen: Gen[RecordType] = Gen.listOfN(10, Arbitrary.arbitrary[Byte])
    .map(bytes => RecordType @@ bytes.toArray)

  lazy val ehrTransactionGen: Gen[EhrTransaction] = for {
    timestamp <- positiveLongGen
    providerKeys <- key25519Gen
    patientPK <- propositionGen
    record <- recordTypeGen
  } yield EhrTransactionCompanion.generate(patientPK, providerKeys, record, timestamp)
}
