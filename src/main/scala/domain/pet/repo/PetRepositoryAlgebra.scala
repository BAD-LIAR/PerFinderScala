package domain.pet.repo

import cats.data.OptionT
import domain.pet.models.Pet

trait PetRepositoryAlgebra[F[_]] {
  def create(pet: Pet): F[Pet]

  def update(pet: Pet): OptionT[F, Pet]

  def get(petId: Long): OptionT[F, Pet]

  def list(pageSize: Int, offset: Int): F[List[Pet]]

}
