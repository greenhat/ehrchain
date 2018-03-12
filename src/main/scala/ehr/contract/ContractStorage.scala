package ehr.contract

import scorex.core.transaction.box.proposition.PublicKey25519Proposition

trait ContractStorage {

  def add(contracts: Seq[Contract]): ContractStorage
  def contractsForPatient(patientPK: PublicKey25519Proposition): Seq[Contract]
}

@SuppressWarnings(Array("org.wartremover.warts.DefaultArguments"))
class InMemoryContractStorage(store: Map[String, Contract] = Map[String, Contract]()) extends ContractStorage {

  override def add(contracts: Seq[Contract]): ContractStorage =
    new InMemoryContractStorage(
      contracts.flatMap {
        case append: AppendContract => Seq(append)
        case _ => Seq[AppendContract]()
      }.foldLeft(store) { case (s, contract) =>
        s + (contract.patientPK.address -> contract)
      }
    )

  override def contractsForPatient(patientPK: PublicKey25519Proposition): Seq[Contract] =
    store.get(patientPK.address).map(Seq(_)).getOrElse(Seq[Contract]())
}
