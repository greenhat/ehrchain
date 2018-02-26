package com.ehrchain.contract

import scorex.core.transaction.box.proposition.PublicKey25519Proposition

trait EhrContractStorage {

  def add(contract: EhrContract)
  def contractsForPatient(patientPK: PublicKey25519Proposition): Seq[EhrContract]
}
