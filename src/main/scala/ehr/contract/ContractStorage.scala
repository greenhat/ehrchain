package ehr.contract

import scorex.core.transaction.box.proposition.PublicKey25519Proposition

trait ContractStorage {

  def add(contracts: Seq[Contract]): ContractStorage
  def contractsForPatient(patientPK: PublicKey25519Proposition): Seq[Contract]
  def readContractsForPatient(patientPK: PublicKey25519Proposition): Seq[ReadContract]
}

@SuppressWarnings(Array("org.wartremover.warts.DefaultArguments"))
class InMemoryContractStorage(store: Map[String, Seq[Contract]] = Map[String, Seq[Contract]]()) extends ContractStorage {

  override def add(contracts: Seq[Contract]): ContractStorage =
    new InMemoryContractStorage(
      contracts.foldLeft(store) { case (s, contract) =>
        s + (contract.patientPK.address -> s.get(contract.patientPK.address).map(_ :+ contract).getOrElse(Seq(contract)))
      }
    )

  override def contractsForPatient(patientPK: PublicKey25519Proposition): Seq[Contract] =
    store.getOrElse(patientPK.address, Seq[Contract]())

  override def readContractsForPatient(patientPK: PublicKey25519Proposition): Seq[ReadContract] =
    contractsForPatient(patientPK).flatMap {
      case readContract: ReadContract => Seq(readContract)
      case _ => Seq[ReadContract]()
    }
}
