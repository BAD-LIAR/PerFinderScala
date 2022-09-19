package domain.user.Dto

import domain.user.Models.{Role, User}

case class UserResponseDto(id: Option[Long], username: String, email: String, role: Role)

object UserResponseDto {
  def from(user: User): UserResponseDto = UserResponseDto(user.id, user.username, user.email, user.role)
}
