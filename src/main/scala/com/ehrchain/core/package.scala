package com.ehrchain

import supertagged.TaggedType

package object core {

  object RecordType extends TaggedType[Array[Byte]]

  type RecordType = RecordType.Type
}
