package ehr

import ehr.history.BlockStream
import ehr.state.EhrMinimalState
import ehr.wallet.Wallet
import scorex.core.NodeViewHolder.CurrentView

import scala.util.{Failure, Success, Try}

package object core {

  type NodeViewHolderCurrentView =
    CurrentView[BlockStream, EhrMinimalState, Wallet, TransactionMemPool]

  implicit class OptionOps[A](opt: Option[A]) {
    def toTry(msg: String): Try[A] = {
      opt
        .map(Success(_))
        .getOrElse(Failure[A](new NoSuchElementException(msg)))
    }
  }
}
