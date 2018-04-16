package ehr.transaction

import scorex.core.transaction.box.proposition.PublicKey25519Proposition

import scala.collection.mutable

trait RecordTransactionStorage {

  def put(txs: Seq[RecordTransaction]): Unit
  def getByPatient(patientPK: PublicKey25519Proposition): Seq[RecordTransaction]
}

@SuppressWarnings(Array("org.wartremover.warts.MutableDataStructures"))
class InMemoryRecordTransactionStorage() extends RecordTransactionStorage {

    val store: mutable.Map[PublicKey25519Proposition, Seq[RecordTransaction]] =
      mutable.Map[PublicKey25519Proposition, Seq[RecordTransaction]]()

  override def put(txs: Seq[RecordTransaction]): Unit =
    txs.foreach { tx =>
      store.update(tx.patient,
        store.getOrElse(tx.patient, Seq[RecordTransaction]()) :+ tx
      )
    }

  override def getByPatient(patientPK: PublicKey25519Proposition): Seq[RecordTransaction] =
    store.getOrElse(patientPK, Seq[RecordTransaction]())

}
