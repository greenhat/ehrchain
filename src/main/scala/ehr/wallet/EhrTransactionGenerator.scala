package ehr.wallet

import java.time.Instant

import akka.actor.{Actor, ActorRef, Props}
import ehr.EhrTransactionMemPool
import ehr.core.NodeViewHolderCurrentView
import ehr.history.EhrBlockStream
import ehr.record.{Record, RecordFile}
import ehr.state.EhrMinimalState
import ehr.transaction.{EhrRecordTransaction, EhrRecordTransactionCompanion}
import scorex.core.LocallyGeneratedModifiersMessages.ReceivableMessages.LocallyGeneratedTransaction
import scorex.core.NodeViewHolder.ReceivableMessages.GetDataFromCurrentView
import scorex.core.transaction.box.proposition.PublicKey25519Proposition

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.FiniteDuration

class EhrTransactionGenerator(viewHolderRef: ActorRef) extends Actor {

  import EhrTransactionGenerator._

  private val getRequiredData = GetDataFromCurrentView[
    EhrBlockStream,
    EhrMinimalState,
    EhrWallet,
    EhrTransactionMemPool,
    GenerateTransaction]
    {  view: NodeViewHolderCurrentView =>
      EhrTransactionGenerator.GenerateTransaction(view.vault)
    }

  @SuppressWarnings(Array("org.wartremover.warts.Any"))
  override def receive: Receive = {
    case StartGeneration(duration) =>
      val _ = context.system.scheduler.schedule(duration, duration, viewHolderRef, getRequiredData)

    case GenerateTransaction(wallet) =>
      viewHolderRef ! LocallyGeneratedTransaction[PublicKey25519Proposition, EhrRecordTransaction](generateTx(wallet))
  }
}

object EhrTransactionGenerator {

  def props(nodeViewHolder: ActorRef): Props = Props(new EhrTransactionGenerator(nodeViewHolder))

  final case class StartGeneration(delay: FiniteDuration)

  final case class GenerateTransaction(wallet: EhrWallet)

  @SuppressWarnings(Array("org.wartremover.warts.TryPartial"))
  def generateTx(wallet: EhrWallet): EhrRecordTransaction =
    EhrRecordTransactionCompanion.generate(
      wallet.patientPK,
      wallet.providerKeyPair,
      Record(Seq(RecordFile.generate("generator record".getBytes).get)),
      Instant.now)
}
