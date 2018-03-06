package com.ehrchain.crypto

import java.nio.ByteBuffer
import java.security.MessageDigest

import scorex.core.transaction.box.proposition.PublicKey25519Proposition
import scorex.crypto.signatures.Curve25519

object ECDHDerivedKey {

  def derivedKey(party1Keys: Curve25519KeyPair, party2PK: PublicKey25519Proposition): Array[Byte] = {
    val sharedSecret = Curve25519.createSharedSecret(party1Keys.privateKey.privKeyBytes, party2PK.pubKeyBytes)
    // Derive a key from the shared secret and both public keys
    /*
       Designers using these curves should be aware that for each public
   key, there are several publicly computable public keys that are
   equivalent to it, i.e., they produce the same shared secrets.  Thus
   using a public key as an identifier and knowledge of a shared secret
   as proof of ownership (without including the public keys in the key
   derivation) might lead to subtle vulnerabilities.

   see https://www.rfc-editor.org/rfc/rfc7748.txt
     */
    val hash = MessageDigest.getInstance("SHA-256")
    hash.update(sharedSecret)
    // Simple deterministic ordering to be sure to apply keys in the same order by both parties
    List(ByteBuffer.wrap(party1Keys.publicKey.bytes), ByteBuffer.wrap(party2PK.bytes))
      .sorted
      .foreach(hash.update(_))
    hash.digest
  }
}
