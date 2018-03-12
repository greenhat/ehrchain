package ehr.wallet

import ehr.block.EhrBlock
import ehr.crypto.Curve25519KeyPair
import ehr.transaction.EhrTransaction
import scorex.core.VersionTag
import scorex.core.transaction.box.proposition.PublicKey25519Proposition
import scorex.core.transaction.state.PrivateKey25519Companion
import scorex.core.transaction.wallet.Vault
import scorex.core.utils.ScorexLogging

import scala.util.Try

final case class Wallet() extends Vault[PublicKey25519Proposition, EhrTransaction, EhrBlock, Wallet]
  with ScorexLogging {

  override type NVCT = this.type

  override def scanOffchain(tx: EhrTransaction): Wallet = this

  override def scanOffchain(txs: Seq[EhrTransaction]): Wallet = this

  override def scanPersistent(modifier: EhrBlock): Wallet = this

  override def rollback(to: VersionTag): Try[Wallet] = Try { this }

  val patientPK: PublicKey25519Proposition =
    PrivateKey25519Companion.generateKeys("patient key".getBytes)._2

  val providerKeyPair: Curve25519KeyPair =
    PrivateKey25519Companion.generateKeys("provider key".getBytes)

  val blockGeneratorKeyPair: Curve25519KeyPair =
    PrivateKey25519Companion.generateKeys("block generator key".getBytes)
}
