package ehr.demo

import akka.actor.ActorRef
import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.scaladsl.Behaviors.same
import ehr.contract.ContractStorage
import ehr.demo.TypedActorWrapper.NodeViewHolderCallback
import ehr.record._
import ehr.transaction.RecordTransactionStorage

import scala.language.postfixOps

object ProviderBTransactionGenerator {

  def readRecords(contractStorage: ContractStorage,
                  recordTxStorage: RecordTransactionStorage,
                  recordFileStorage: RecordFileStorage): Unit = {
    RecordReader.decryptRecordsInMemoryWithProviderKeys(
      PatientTransactionGenerator.patientKeyPair.publicKey,
      PatientTransactionGenerator.providerBKeyPair,
      contractStorage,
      recordTxStorage,
      recordFileStorage)
    // todo print records content
  }

  def behavior(viewHolderRef: ActorRef,
               contractStorage: ContractStorage,
               recordTxStorage: RecordTransactionStorage,
               fileStorage: RecordFileStorage): Behavior[NodeViewHolderCallback] =
    Behaviors.immutable[NodeViewHolderCallback] { (ctx, msg) =>
      msg match {
        case NodeViewHolderCallback(_) =>
          readRecords(contractStorage, recordTxStorage, fileStorage)
          same
      }
    }
}
