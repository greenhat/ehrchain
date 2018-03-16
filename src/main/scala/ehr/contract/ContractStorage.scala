package ehr.contract

import scorex.core.transaction.box.proposition.PublicKey25519Proposition

import scala.reflect.ClassTag

trait ContractStorage {

  def add(contracts: Seq[Contract]): ContractStorage
  def contractsForPatient[T: ClassTag](patientPK: PublicKey25519Proposition,
                          providerPK: PublicKey25519Proposition): Seq[T]
}

@SuppressWarnings(Array("org.wartremover.warts.DefaultArguments"))
class InMemoryContractStorage(store: Map[String, Seq[Contract]] = Map[String, Seq[Contract]]()) extends ContractStorage {

  override def add(contracts: Seq[Contract]): ContractStorage =
    new InMemoryContractStorage(
      contracts.foldLeft(store) { case (s, contract) =>
        s + (contract.patientPK.address -> s.get(contract.patientPK.address).map(_ :+ contract).getOrElse(Seq(contract)))
      }
    )

  @SuppressWarnings(Array("org.wartremover.warts.AsInstanceOf"))
  override def contractsForPatient[T: ClassTag](patientPK: PublicKey25519Proposition,
                                      providerPK: PublicKey25519Proposition): Seq[T] =
    store.get(patientPK.address)
      .map(_.filter(contract => contract.providerPK == providerPK))
      .map(_.flatMap { contract: Contract =>
        val clazz = implicitly[ClassTag[T]].runtimeClass
        contract match {
          case typedContract if clazz.isInstance(typedContract) => Seq[T](typedContract.asInstanceOf[T])
          case _ => Seq[T]()
        }
      })
      .getOrElse(Seq[T]())
}
