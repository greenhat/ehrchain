package com.ehrchain.crypto

import scorex.core.transaction.box.proposition.{ProofOfKnowledgeProposition, PublicKey25519Proposition}
import scorex.core.transaction.state.{PrivateKey25519, Secret}

trait AsymmCipherKeyPair[S <: Secret] {
  val privateKey: S
  val publicKey: ProofOfKnowledgeProposition[S]
}

final case class Curve25519KeyPair(privateKey: PrivateKey25519,
                                   publicKey: PublicKey25519Proposition)
  extends AsymmCipherKeyPair[PrivateKey25519] {
}

@SuppressWarnings(Array("org.wartremover.warts.ImplicitConversion"))
object Curve25519KeyPair {

  implicit def fromTuple(keysTuple: (PrivateKey25519, PublicKey25519Proposition)): Curve25519KeyPair =
    Curve25519KeyPair(keysTuple._1, keysTuple._2)

  implicit def toTuple(keyPair: Curve25519KeyPair): (PrivateKey25519, PublicKey25519Proposition) =
    (keyPair.privateKey, keyPair.publicKey)
}
