package com.ehrchain

import com.ehrchain.history.EhrBlockStream
import com.ehrchain.state.EhrMinimalState
import com.ehrchain.wallet.EhrWallet
import scorex.core.NodeViewHolder.CurrentView
import supertagged.TaggedType

import scala.util.{Failure, Success, Try}

package object core {

  type NodeViewHolderCurrentView =
    CurrentView[EhrBlockStream, EhrMinimalState, EhrWallet, EhrTransactionMemPool]

  implicit class OptionOps[A](opt: Option[A]) {
    def toTry(msg: String): Try[A] = {
      opt
        .map(Success(_))
        .getOrElse(Failure[A](new NoSuchElementException(msg)))
    }
  }
}
