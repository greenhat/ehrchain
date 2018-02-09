package com.ehrchain.history

import com.ehrchain.EhrGenerators
import org.scalatest.prop.{GeneratorDrivenPropertyChecks, PropertyChecks}
import org.scalatest.{FlatSpec, Matchers}

class EhrHistorySpec extends FlatSpec
  with PropertyChecks
  with GeneratorDrivenPropertyChecks
  with Matchers
  with EhrGenerators {

  "generated history" should "have height" in {
      generateHistory(2).height shouldEqual 2
  }

}
