package ehr

import java.time.Instant

import akka.actor.Props
import ehr.block.EhrBlock
import ehr.contract.{AppendContract, InMemoryContractStorage, Unlimited}
import ehr.crypto.Curve25519KeyPair
import ehr.history.{BlockStream, EhrSyncInfo, HistoryStorage}
import ehr.mempool.TransactionMemPool
import ehr.record.{FileHash, InMemoryRecordFileStorage, Record, RecordFileStorage}
import ehr.state.EhrMinimalState
import ehr.transaction.{ContractTransaction, EhrRecordTransactionCompanion, EhrTransaction, InMemoryRecordTransactionStorage}
import ehr.wallet.Wallet
import ehr.serialization._
import scorex.core.serialization.Serializer
import scorex.core.transaction.Transaction
import scorex.core.transaction.box.proposition.PublicKey25519Proposition
import scorex.core.transaction.state.PrivateKey25519Companion
import scorex.core.{ModifierTypeId, NodeViewHolder, NodeViewModifier, VersionTag}

@SerialVersionUID(0L)
class EhrNodeViewHolder(val recordFileStorage: RecordFileStorage)
  extends NodeViewHolder[PublicKey25519Proposition, EhrTransaction, EhrBlock] {

  override val networkChunkSize: Int = 10

  override type SI = EhrSyncInfo
  override type HIS = BlockStream
  override type MS = EhrMinimalState
  override type VL = Wallet
  override type MP = TransactionMemPool

  /**
    * Restore a local view during a node startup. If no any stored view found
    * (e.g. if it is a first launch of a node) None is to be returned
    */
  override def restoreState(): Option[(HIS, MS, VL, MP)] = None

  /**
    * Hard-coded initial view all the honest nodes in a network are making progress from.
    */
  override protected def genesisState: (HIS, MS, VL, MP) =
    EhrNodeViewHolder.generateGenesisState(recordFileStorage)

  /**
    * Serializers for modifiers, to be provided by a concrete instantiation
    */
  override val modifierSerializers: Map[ModifierTypeId, Serializer[_ <: NodeViewModifier]] =
    Map(EhrBlock.ModifierType -> byteSerializer[EhrBlock],
      Transaction.ModifierTypeId -> byteSerializer[EhrTransaction])
}

object EhrNodeViewHolder {

  def props(recordFileStorage: RecordFileStorage): Props = Props(
    new EhrNodeViewHolder(recordFileStorage))

  @SuppressWarnings(Array("org.wartremover.warts.TryPartial"))
  def generateGenesisState(recordFileStorage: RecordFileStorage
                          ): (BlockStream, EhrMinimalState, Wallet, TransactionMemPool) = {
    val genesisBlockAccount = PrivateKey25519Companion.generateKeys("genesis block".getBytes)
    val genesisPatientAccount: Curve25519KeyPair =
      PrivateKey25519Companion.generateKeys("genesis patient".getBytes)
    val genesisProviderAccount: Curve25519KeyPair =
      PrivateKey25519Companion.generateKeys("genesis provider".getBytes)
    val timestamp = Instant.ofEpochSecond(1518788012L)
    val genesisTxs = Seq(
      ContractTransaction.generate(genesisPatientAccount,
        AppendContract(genesisPatientAccount.publicKey, genesisProviderAccount.publicKey,
          timestamp, Unlimited),
        timestamp)
    )
    val genesisBlock = EhrBlock.generate(BlockStream.GenesisParentId, timestamp, genesisTxs,
      genesisBlockAccount, 0)

    val historyStorage = new HistoryStorage(recordFileStorage)
    val history = BlockStream.load(historyStorage).append(genesisBlock).get._1
    val gs = EhrMinimalState(VersionTag @@ genesisBlock.id,
      historyStorage.contractStorage,
      historyStorage.recordTransactionStorage)
    val gw = Wallet()

    (history, gs, gw, TransactionMemPool.emptyPool)
  }
}
