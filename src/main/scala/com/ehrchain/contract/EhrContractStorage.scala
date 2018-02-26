package com.ehrchain.contract

import scorex.core.transaction.box.proposition.PublicKey25519Proposition

trait EhrContractStorage {

  def add(contracts: Seq[EhrContract]): EhrContractStorage
  def contractsForPatient(patientPK: PublicKey25519Proposition): Seq[EhrContract]
}

@SuppressWarnings(Array("org.wartremover.warts.DefaultArguments"))
class EhrInMemoryContractStorage(store: Map[String, EhrContract] = Map[String, EhrContract]()) extends EhrContractStorage {

  override def add(contracts: Seq[EhrContract]): EhrContractStorage =
    new EhrInMemoryContractStorage(
      contracts.flatMap {
        case append: EhrAppendContract => Seq(append)
        case _ => Seq[EhrAppendContract]()
      }.foldLeft(store) { case (s, contract) =>
        s + (contract.patientPK.address -> contract)
      }
    )

  override def contractsForPatient(patientPK: PublicKey25519Proposition): Seq[EhrContract] =
    store.get(patientPK.address).map(Seq(_)).getOrElse(Seq[EhrContract]())
}
