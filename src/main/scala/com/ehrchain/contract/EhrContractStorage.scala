package com.ehrchain.contract

import scorex.core.transaction.box.proposition.PublicKey25519Proposition

trait EhrContractStorage {

  def add(contract: EhrContract): Unit
  def contractsForPatient(patientPK: PublicKey25519Proposition): Seq[EhrContract]
}

class EhrInMemoryContractStorage extends EhrContractStorage {

  override def add(contract: EhrContract): Unit = ???

  override def contractsForPatient(patientPK: PublicKey25519Proposition): Seq[EhrContract] = ???
}
