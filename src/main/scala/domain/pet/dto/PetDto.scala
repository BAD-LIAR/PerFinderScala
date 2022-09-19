package domain.pet.dto

case class PetCreateRequestDto (id: Option[Long],
                   pet_type: String,
                   breed: String,
                   city: String,
                   color: String,
                   age: Int,
                   userId: Option[Long])

case class PetUpdateRequestDto (id: Option[Long],
                                pet_type: String,
                                breed: String,
                                city: String,
                                color: String,
                                age: Int,
                                userId: Option[Long])

