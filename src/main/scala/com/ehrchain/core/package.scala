package com.ehrchain

import com.ehrchain.history.EhrBlockStream
import com.ehrchain.state.EhrMinimalState
import com.ehrchain.wallet.EhrWallet
import scorex.core.NodeViewHolder.CurrentView
import supertagged.TaggedType

package object core {

  object TimeStamp extends TaggedType[Long]

  type TimeStamp = TimeStamp.Type

  type NodeViewHolderCurrentView =
    CurrentView[EhrBlockStream, EhrMinimalState, EhrWallet, EhrTransactionMemPool]
}
