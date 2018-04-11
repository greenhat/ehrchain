package ehr.demo

import akka.actor.ActorRef
import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.scaladsl.Behaviors.same
import com.google.common.io.ByteStreams
import ehr.contract.ContractStorage
import ehr.demo.TypedActorWrapper.NodeViewHolderCallback
import ehr.record._
import ehr.transaction.RecordTransactionStorage
import scorex.core.utils.ScorexLogging

import scala.language.postfixOps

object ProviderBTransactionGenerator extends ScorexLogging {

  @SuppressWarnings(Array("org.wartremover.warts.TryPartial"))
  def readRecords(contractStorage: ContractStorage,
                  recordTxStorage: RecordTransactionStorage,
                  recordFileStorage: RecordFileStorage): Unit = {
    RecordReader.decryptRecordsInMemoryWithProviderKeys(
      PatientTransactionGenerator.patientKeyPair.publicKey,
      PatientTransactionGenerator.providerBKeyPair,
      contractStorage,
      recordTxStorage,
      recordFileStorage)
        .map(fileSource => ByteStreams.toByteArray(fileSource.get.inputStream))
        .foreach(bytes => log.info(s"record: $bytes.toString"))
  }

  def behavior(viewHolderRef: ActorRef): Behavior[NodeViewHolderCallback] =
    Behaviors.immutable[NodeViewHolderCallback] { (_, msg) =>
      msg match {
        case NodeViewHolderCallback(view) =>
          readRecords(view.history.storage.contractStorage,
            view.history.storage.recordTransactionStorage,
            view.history.storage.recordFileStorage)
          same
      }
    }
}
