package domain.pet.validation

trait PetValidationError extends Product with Serializable

case object PetNotFoundError extends PetValidationError
case object UpdateNotAllowed extends PetValidationError
