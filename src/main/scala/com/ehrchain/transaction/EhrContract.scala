package com.ehrchain.transaction

import scorex.core.transaction.box.proposition.PublicKey25519Proposition

trait EhrContract {
}

final case class EhrAppendContract(encryptedPatientPK: Array[Byte],
                             providerPK: PublicKey25519Proposition)
  extends EhrContract {
}
