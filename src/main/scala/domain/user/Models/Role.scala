package domain.user.Models

import cats.Eq
import tsec.authorization.{AuthGroup, SimpleAuthEnum}

final case class Role(roleRepr: String)

object Role extends SimpleAuthEnum[Role, String] {
  val Admin: Role = Role("Admin")
  val Default: Role = Role("Default")

  override val values: AuthGroup[Role] = AuthGroup(Admin, Default)

  override def getRepr(t: Role): String = t.roleRepr

  implicit val eqRole: Eq[Role] = Eq.fromUniversalEquals[Role]
}
