package com.ehrchain.wallet

import com.ehrchain.block.EhrBlock
import com.ehrchain.transaction.EhrTransaction
import scorex.core.VersionTag
import scorex.core.transaction.box.proposition.PublicKey25519Proposition
import scorex.core.transaction.wallet.Vault
import scorex.core.utils.ScorexLogging

import scala.util.Try

final case class EhrWallet() extends Vault[PublicKey25519Proposition, EhrTransaction, EhrBlock, EhrWallet]
  with ScorexLogging {

  override type NVCT = this.type

  override def scanOffchain(tx: EhrTransaction): EhrWallet = this

  override def scanOffchain(txs: Seq[EhrTransaction]): EhrWallet = this

  override def scanPersistent(modifier: EhrBlock): EhrWallet = this

  override def rollback(to: VersionTag): Try[EhrWallet] = Try { this }
}
