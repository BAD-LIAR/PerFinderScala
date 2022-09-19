package infrastructure.repository

import cats.data.OptionT
import cats.effect.Bracket
import cats.implicits.{catsSyntaxOptionId, toFunctorOps}
import domain.pet.models.Pet
import domain.pet.repo.PetRepositoryAlgebra
import doobie._
import doobie.implicits._
import infrastructure.repository.SQLPagination.paginate
import tsec.authentication.IdentityStore

private object PetSQL {

  def insert(pet: Pet): Update0 = sql"""
    INSERT INTO PETS (pet_type, breed, city, color, age, userId)
    VALUES (${pet.pet_type}, ${pet.breed}, ${pet.city}, ${pet.color}, ${pet.age}, ${pet.userId})
""".update

  def update(pet: Pet): Update0 = sql"""
    UPDATE PETS
    SET pet_type = ${pet.pet_type},
        breed = ${pet.breed},
        city = ${pet.city},
        color = ${pet.color},
        age = ${pet.age},
        userId = ${pet.userId}
    WHERE ID = ${pet.id}
""".update

  def select(petId: Long): Query0[Pet] =
    sql"""
      SELECT *
      FROM PETS
      WHERE ID = $petId
    """.query

  val selectAll: Query0[Pet] =
    sql"""
      SELECT *
      FROM PETS
    """.query

}
class DoobiePetRepositoryInterpreter[F[_]: Bracket[*[_], Throwable]](val xa: Transactor[F])
  extends PetRepositoryAlgebra[F]
    with IdentityStore[F, Long, Pet] { self =>
  import  PetSQL._

  override def create(pet: Pet): F[Pet] = insert(pet)
    .withUniqueGeneratedKeys[Long](columns = "id")
    .map(id => pet.copy(id = id.some)).transact(xa)

  override def update(pet: Pet): OptionT[F, Pet] = OptionT
    .fromOption[ConnectionIO](pet.id)
    .semiflatMap(id => PetSQL.update(pet).run.as(pet))
    .transact(xa)

  override def list(pageSize: Int, offset: Int): F[List[Pet]] = paginate(pageSize, offset)(selectAll).to[List].transact(xa)

  override def get(id: Long): OptionT[F, Pet] = OptionT(select(id).option.transact(xa))

}

object DoobiePetRepositoryInterpreter {
  def apply[F[_]: Bracket[*[_], Throwable]](xa: Transactor[F]): DoobiePetRepositoryInterpreter[F] =
    new DoobiePetRepositoryInterpreter(xa)
}
