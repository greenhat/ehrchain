package com.ehrchain

import akka.actor.Props
import com.ehrchain.block.{EhrBlock, EhrBlockSerializer}
import com.ehrchain.core.{RecordType, TimeStamp}
import com.ehrchain.history.{EhrBlockStream, EhrHistoryStorage, EhrSyncInfo}
import com.ehrchain.state.EhrMinimalState
import com.ehrchain.transaction.{EhrTransaction, EhrTransactionCompanion, EhrTransactionSerializer}
import com.ehrchain.wallet.EhrWallet
import scorex.core.serialization.Serializer
import scorex.core.transaction.Transaction
import scorex.core.transaction.box.proposition.PublicKey25519Proposition
import scorex.core.transaction.state.PrivateKey25519Companion
import scorex.core.{ModifierTypeId, NodeViewHolder, NodeViewModifier, VersionTag}

class EhrNodeViewHolder extends NodeViewHolder[PublicKey25519Proposition, EhrTransaction, EhrBlock] {

  override val networkChunkSize: Int = 10

  override type SI = EhrSyncInfo
  override type HIS = EhrBlockStream
  override type MS = EhrMinimalState
  override type VL = EhrWallet
  override type MP = EhrTransactionMemPool

  /**
    * Restore a local view during a node startup. If no any stored view found
    * (e.g. if it is a first launch of a node) None is to be returned
    */
  override def restoreState(): Option[(HIS, MS, VL, MP)] = None

  /**
    * Hard-coded initial view all the honest nodes in a network are making progress from.
    */
  override protected def genesisState: (HIS, MS, VL, MP) = EhrNodeViewHolder.generateGenesisState

  /**
    * Serializers for modifiers, to be provided by a concrete instantiation
    */
  override val modifierSerializers: Map[ModifierTypeId, Serializer[_ <: NodeViewModifier]] =
    Map(EhrBlock.ModifierType -> EhrBlockSerializer,
      Transaction.ModifierTypeId -> EhrTransactionSerializer)
}

object EhrNodeViewHolder {

  def props: Props = Props(new EhrNodeViewHolder)

  @SuppressWarnings(Array("org.wartremover.warts.TryPartial"))
  def generateGenesisState: (EhrBlockStream, EhrMinimalState, EhrWallet, EhrTransactionMemPool) = {
    val genesisBlockAccount = PrivateKey25519Companion.generateKeys("genesis block".getBytes)
    val genesisPatientAccount = PrivateKey25519Companion.generateKeys("genesis patient".getBytes)
    val genesisProviderAccount = PrivateKey25519Companion.generateKeys("genesis provider".getBytes)
    val timestamp = TimeStamp @@ 1518788012L
    val genesisRecord = RecordType @@ "genesis record".getBytes
    val genesisTxs = Seq(
      EhrTransactionCompanion.generate(genesisPatientAccount._2, genesisProviderAccount, genesisRecord,
        timestamp)
    )
    val genesisBlock = EhrBlock.generate(EhrBlockStream.GenesisParentId, timestamp, genesisTxs,
      genesisBlockAccount, 0)

    val history = EhrBlockStream.load(new EhrHistoryStorage()).append(genesisBlock).get._1

    val gs = EhrMinimalState(VersionTag @@ genesisBlock.id)
    val gw = EhrWallet()

    (history, gs, gw, EhrTransactionMemPool.emptyPool)
  }
}