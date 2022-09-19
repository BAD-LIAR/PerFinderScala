package domain.pet.validation

import cats.Applicative
import cats.data.EitherT
import cats.syntax.all._
import domain.pet.repo.PetRepositoryAlgebra

class PetValidationInterpreter[F[_]: Applicative](petRepo: PetRepositoryAlgebra[F]) extends PetValidationAlgebra[F] {
  override def exists(itemId: Option[Long]): EitherT[F, PetNotFoundError.type, Unit] =
    EitherT {
      itemId match {
        case Some(id) =>
          petRepo.get(id).value.map {
            case Some(_) => Right(())
            case _ => Left[PetNotFoundError.type, Unit](PetNotFoundError)
          }
        case _ =>
          Either.left[PetNotFoundError.type, Unit](PetNotFoundError).pure[F]
      }
    }

  override def canUpdate(itemId: Option[Long], userId: Long): EitherT[F, UpdateNotAllowed.type, Unit] =
    EitherT {
      itemId match {
        case Some(id) => petRepo.get(id).value.map {
          case Some(item) => if ((userId == item.userId.getOrElse(-1))) {
            Either.right[UpdateNotAllowed.type, Unit](())
          } else {
            Either.left[UpdateNotAllowed.type , Unit](UpdateNotAllowed)
          }
          case _ => Left[UpdateNotAllowed.type, Unit](UpdateNotAllowed)
        }
        case _ =>
          Either.left[UpdateNotAllowed.type, Unit](UpdateNotAllowed).pure[F]
      }
    }
}

object PetValidationInterpreter {
  def apply[F[_]: Applicative](repository: PetRepositoryAlgebra[F]) =
    new PetValidationInterpreter[F](repository)
}