package ehr.wallet

import java.time.Instant

import akka.actor.{Actor, ActorRef, Props}
import ehr.TransactionMemPool
import ehr.core.NodeViewHolderCurrentView
import ehr.history.BlockStream
import ehr.record.{Record, FileHash}
import ehr.state.EhrMinimalState
import ehr.transaction.{RecordTransaction, EhrRecordTransactionCompanion}
import scorex.core.LocallyGeneratedModifiersMessages.ReceivableMessages.LocallyGeneratedTransaction
import scorex.core.NodeViewHolder.ReceivableMessages.GetDataFromCurrentView
import scorex.core.transaction.box.proposition.PublicKey25519Proposition

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.FiniteDuration

class TransactionGenerator(viewHolderRef: ActorRef) extends Actor {

  import TransactionGenerator._

  private val getRequiredData = GetDataFromCurrentView[
    BlockStream,
    EhrMinimalState,
    Wallet,
    TransactionMemPool,
    GenerateTransaction]
    {  view: NodeViewHolderCurrentView =>
      TransactionGenerator.GenerateTransaction(view.vault)
    }

  @SuppressWarnings(Array("org.wartremover.warts.Any"))
  override def receive: Receive = {
    case StartGeneration(duration) =>
      val _ = context.system.scheduler.schedule(duration, duration, viewHolderRef, getRequiredData)

    case GenerateTransaction(wallet) =>
      viewHolderRef ! LocallyGeneratedTransaction[PublicKey25519Proposition, RecordTransaction](generateTx(wallet))
  }
}

object TransactionGenerator {

  def props(nodeViewHolder: ActorRef): Props = Props(new TransactionGenerator(nodeViewHolder))

  final case class StartGeneration(delay: FiniteDuration)

  final case class GenerateTransaction(wallet: Wallet)

  @SuppressWarnings(Array("org.wartremover.warts.TryPartial"))
  def generateTx(wallet: Wallet): RecordTransaction =
    EhrRecordTransactionCompanion.generate(
      wallet.patientPK,
      wallet.providerKeyPair,
      Record(Seq(FileHash.generate("generator record".getBytes).get)),
      Instant.now)
}
