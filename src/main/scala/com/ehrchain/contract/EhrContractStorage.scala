package com.ehrchain.contract

import scorex.core.transaction.box.proposition.PublicKey25519Proposition

import scala.util.{Failure, Success, Try}

trait EhrContractStorage {

  def add(contract: EhrContract): Try[EhrContractStorage]
  def contractsForPatient(patientPK: PublicKey25519Proposition): Seq[EhrContract]
}

@SuppressWarnings(Array("org.wartremover.warts.MutableDataStructures", "org.wartremover.warts.Var"))
class EhrInMemoryContractStorage extends EhrContractStorage {

  private val store: scala.collection.mutable.Map[String, EhrContract] = scala.collection.mutable.Map()

  override def add(contract: EhrContract): Try[EhrContractStorage] = contract match {
    case append: EhrAppendContract =>
      store(append.patientPK.address) = append
      Success(this)
    case _ => Failure[EhrContractStorage](new Error("unknown contract type"))
  }

  override def contractsForPatient(patientPK: PublicKey25519Proposition): Seq[EhrContract] =
    store.get(patientPK.address).map(Seq(_)).getOrElse(Seq[EhrContract]())
}
