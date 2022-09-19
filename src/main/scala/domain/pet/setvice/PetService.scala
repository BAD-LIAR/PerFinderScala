package domain.pet.setvice

import cats.Monad
import cats.data.EitherT
import domain.pet.models.Pet
import domain.pet.repo.PetRepositoryAlgebra
import domain.pet.validation.{PetNotFoundError, PetValidationAlgebra, UpdateNotAllowed}

class PetService [F[_]: Monad](petRepo: PetRepositoryAlgebra[F], validation: PetValidationAlgebra[F]) {

  def createPet(pet: Pet) (implicit M: Monad[F]): F[Pet] = petRepo.create(pet)

  def list(pageSize: Int, offset: Int): F[List[Pet]] =
    petRepo.list(pageSize, offset)

  def get(petId: Long): EitherT[F, PetNotFoundError.type, Pet] = petRepo.get(petId).toRight(PetNotFoundError)

  def update(pet: Pet): EitherT[F, UpdateNotAllowed.type, Pet] =
    for {
//      _ <- validation.canUpdate(item.id, userId)
      saved <- petRepo.update(pet).toRight(UpdateNotAllowed)
    } yield saved
}
object PetService {
  def apply[F[_]: Monad](
                          repository: PetRepositoryAlgebra[F],
                          validation: PetValidationAlgebra[F],
                        ): PetService[F] =
    new PetService[F](repository, validation)
}