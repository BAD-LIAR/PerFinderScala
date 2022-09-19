package domain.pet.validation

import cats.data.EitherT

trait PetValidationAlgebra[F[_]] {
  def exists(petId: Option[Long]): EitherT[F, PetNotFoundError.type, Unit]

  def canUpdate(petId: Option[Long], userId: Long): EitherT[F, UpdateNotAllowed.type, Unit]
}
