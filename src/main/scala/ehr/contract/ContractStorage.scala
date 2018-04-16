package ehr.contract

import scorex.core.transaction.box.proposition.PublicKey25519Proposition

import scala.collection.mutable
import scala.reflect.ClassTag

trait ContractStorage {

  def add(contracts: Seq[Contract]): Unit
  def contractsForPatient[T: ClassTag](patientPK: PublicKey25519Proposition,
                          providerPK: PublicKey25519Proposition): Seq[T]
}

@SuppressWarnings(Array("org.wartremover.warts.MutableDataStructures"))
final class InMemoryContractStorage() extends ContractStorage {

  val store: mutable.Map[String, Seq[Contract]] = mutable.Map[String, Seq[Contract]]()

  override def add(contracts: Seq[Contract]): Unit =
    contracts.foreach { contract =>
      store.update(contract.patientPK.address,
        store.getOrElse(contract.patientPK.address, Seq[Contract]()) :+ contract
      )
    }

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
