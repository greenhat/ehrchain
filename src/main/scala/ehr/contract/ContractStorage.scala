package ehr.contract

import scorex.core.transaction.box.proposition.PublicKey25519Proposition

trait ContractStorage {

  def add(contracts: Seq[Contract]): ContractStorage
  def contractsForPatient(patientPK: PublicKey25519Proposition): Seq[Contract]
  def readContractsForPatient(patientPK: PublicKey25519Proposition): Seq[ReadContract]
}

@SuppressWarnings(Array("org.wartremover.warts.DefaultArguments"))
class InMemoryContractStorage(store: Map[String, Contract] = Map[String, Contract]()) extends ContractStorage {

  // todo handle read contracts
  override def add(contracts: Seq[Contract]): ContractStorage =
    new InMemoryContractStorage(
      contracts.foldLeft(store) { case (s, contract) =>
        s + (contract.patientPK.address -> contract)
      }
    )

  override def contractsForPatient(patientPK: PublicKey25519Proposition): Seq[Contract] =
    store.get(patientPK.address).map(Seq(_)).getOrElse(Seq[Contract]())

  override def readContractsForPatient(patientPK: PublicKey25519Proposition): Seq[ReadContract] =
    contractsForPatient(patientPK).flatMap {
      case readContract: ReadContract => Seq(readContract)
      case _ => Seq[ReadContract]()
    }
}
