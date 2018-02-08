package com.ehrchain.mining

import scorex.core.ModifierId

class EhrMiningSettings {
  lazy val GenesisParentId: ModifierId = ModifierId @@ Array.fill(32)(1: Byte)
}
