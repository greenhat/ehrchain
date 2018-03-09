package com.ehrchain.transaction

import scorex.core.transaction.box.proposition.PublicKey25519Proposition

trait RecordTransactionStorage {

  def put(txs: Seq[EhrRecordTransaction]): RecordTransactionStorage
  def getBySubject(subjectPK: PublicKey25519Proposition): Seq[EhrRecordTransaction]
}


@SuppressWarnings(Array("org.wartremover.warts.DefaultArguments"))
class InMemoryRecordTransactionStorage(
                                        store: Map[PublicKey25519Proposition, Seq[EhrRecordTransaction]] =
                                        Map[PublicKey25519Proposition, Seq[EhrRecordTransaction]]())
  extends RecordTransactionStorage {

  override def put(txs: Seq[EhrRecordTransaction]): RecordTransactionStorage =
    new InMemoryRecordTransactionStorage(
      txs.foldLeft(store) { case (s, tx) =>
          s + (tx.subject ->
            (s.getOrElse(tx.subject, Seq[EhrRecordTransaction]()) :+ tx))
      }
    )

  override def getBySubject(subjectPK: PublicKey25519Proposition): Seq[EhrRecordTransaction] =
    store.getOrElse(subjectPK, Seq[EhrRecordTransaction]())

}
