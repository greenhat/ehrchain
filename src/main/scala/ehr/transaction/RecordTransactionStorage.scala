package ehr.transaction

import scorex.core.transaction.box.proposition.PublicKey25519Proposition

trait RecordTransactionStorage {

  def put(txs: Seq[RecordTransaction]): RecordTransactionStorage
  def getByPatient(patientPK: PublicKey25519Proposition): Seq[RecordTransaction]
}


@SuppressWarnings(Array("org.wartremover.warts.DefaultArguments"))
class InMemoryRecordTransactionStorage(
                                        store: Map[PublicKey25519Proposition, Seq[RecordTransaction]] =
                                        Map[PublicKey25519Proposition, Seq[RecordTransaction]]())
  extends RecordTransactionStorage {

  override def put(txs: Seq[RecordTransaction]): RecordTransactionStorage =
    new InMemoryRecordTransactionStorage(
      txs.foldLeft(store) { case (s, tx) =>
          s + (tx.patient ->
            (s.getOrElse(tx.patient, Seq[RecordTransaction]()) :+ tx))
      }
    )

  override def getByPatient(patientPK: PublicKey25519Proposition): Seq[RecordTransaction] =
    store.getOrElse(patientPK, Seq[RecordTransaction]())

}
