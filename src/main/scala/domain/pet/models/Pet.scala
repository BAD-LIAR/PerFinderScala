package domain.pet.models

import domain.pet.dto.{PetCreateRequestDto, PetUpdateRequestDto}

case class Pet(
                id: Option[Long],
                pet_type: String,
                breed: String,
                city: String,
                color: String,
                age: Int,
                userId: Option[Long])


object Pet {
  def from(petDto: PetCreateRequestDto): Pet =
    new Pet(petDto.id, petDto.pet_type, petDto.breed, petDto.city, petDto.color, petDto.age, petDto.userId)

  def from(petDto: PetUpdateRequestDto): Pet =
    new Pet(petDto.id, petDto.pet_type, petDto.breed, petDto.city, petDto.color, petDto.age, petDto.userId)

}
