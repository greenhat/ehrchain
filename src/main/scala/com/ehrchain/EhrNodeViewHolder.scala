package com.ehrchain

import com.ehrchain.block.{EhrBlock, EhrBlockSerializer}
import com.ehrchain.history.{EhrBlockStream, EhrSyncInfo}
import com.ehrchain.state.EhrMinimalState
import com.ehrchain.transaction.{EhrTransaction, EhrTransactionSerializer}
import com.ehrchain.wallet.EhrWallet
import scorex.core.serialization.Serializer
import scorex.core.transaction.Transaction
import scorex.core.transaction.box.proposition.PublicKey25519Proposition
import scorex.core.{ModifierTypeId, NodeViewHolder, NodeViewModifier}

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
  override def restoreState(): Option[(EhrBlockStream, EhrMinimalState, EhrWallet, EhrTransactionMemPool)] = ???

  /**
    * Hard-coded initial view all the honest nodes in a network are making progress from.
    */
  override protected def genesisState: (EhrBlockStream, EhrMinimalState, EhrWallet, EhrTransactionMemPool) = ???

  /**
    * Serializers for modifiers, to be provided by a concrete instantiation
    */
  override val modifierSerializers: Map[ModifierTypeId, Serializer[_ <: NodeViewModifier]] =
    Map(EhrBlock.ModifierType -> EhrBlockSerializer,
      Transaction.ModifierTypeId -> EhrTransactionSerializer)
}
