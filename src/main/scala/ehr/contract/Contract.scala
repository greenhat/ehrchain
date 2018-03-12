package ehr.contract

import java.time.Instant

import scorex.core.serialization.BytesSerializable

import scala.util.Try

trait ContractTerm extends Serializable

@SerialVersionUID(0L)
case object Unlimited extends ContractTerm
@SerialVersionUID(0L)
final case class ValidUntil(date: Instant) extends ContractTerm

trait Contract extends BytesSerializable {

  val timestamp: Instant

  def semanticValidity: Try[Unit]
}
