package com.ehrchain.transaction

import scorex.core.transaction.box.proposition.PublicKey25519Proposition

trait EhrContract {
}

final case class EhrAppendContract(patientPK: PublicKey25519Proposition,
                                   providerPK: PublicKey25519Proposition)
  extends EhrContract {
}
