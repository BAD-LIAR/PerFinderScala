package infrastructure.endpoint

import cats.effect.Sync
import cats.syntax.all._
import domain.Auth
import domain.pet.dto.{PetCreateRequestDto, PetUpdateRequestDto}
import domain.pet.models.Pet
import domain.pet.setvice.PetService
import domain.pet.validation.PetNotFoundError
import domain.user.Models.User
import domain.user.Validation.UserNotFoundError
import infrastructure.endpoint.Pagination.{OptionalOffsetMatcher, OptionalPageSizeMatcher}
import org.http4s.{EntityDecoder, HttpRoutes, QueryParamDecoder}
import org.http4s.circe._
import org.http4s.dsl.Http4sDsl
import io.circe.generic.auto._
import org.http4s.circe.CirceEntityCodec.circeEntityEncoder
import tsec.authentication.{AugmentedJWT, SecuredRequestHandler, asAuthed}
import tsec.jwt.algorithms.JWTMacAlgo
import io.circe.syntax._

class PetEndpont[F[_] : Sync, Auth: JWTMacAlgo] extends Http4sDsl[F] {

  implicit val petDecoder: EntityDecoder[F, Pet] = jsonOf

  implicit val petCreateRequestDto: EntityDecoder[F, PetCreateRequestDto] = jsonOf

  implicit val petUpdateRequestDto: EntityDecoder[F, PetUpdateRequestDto] = jsonOf


  private def createPetEndpoint(
                                 petService: PetService[F]
                               ): AuthEndpoint[F, Auth] = {
    case req@POST -> Root asAuthed user =>
      user.id match {
        case Some(id) => Ok(
          for {
            petDto <- req.request.as[PetCreateRequestDto]
            pet = Pet.from(petDto)
            result <- petService.createPet(pet)
          } yield result
        )
        case None => NotFound(UserNotFoundError)
      }
  }

  private def getPetEndpoint(
                              petService: PetService[F]
                            ): HttpRoutes[F] = HttpRoutes.of[F] {
    case req@GET -> Root / LongVar(id) =>
      petService.get(id).value.flatMap {
        case Right(pet) => Ok(pet.asJson)
        case Left(PetNotFoundError) => NotFound(PetNotFoundError)
      }
  }

  private def getPetsEndpoint(
                               petService: PetService[F]
                             ): HttpRoutes[F] = HttpRoutes.of[F] {
    case req@GET -> Root :? OptionalPageSizeMatcher(limit) :? OptionalOffsetMatcher(offset,
    ) =>
      for {
        retrieved <- petService.list(limit.getOrElse(20), offset.getOrElse(0))
        resp <- Ok(retrieved.asJson)
      } yield resp
  }

  private def updatePetEndpoint(
                                 petService: PetService[F]
                               ): AuthEndpoint[F, Auth] = {
    case req@PUT -> Root asAuthed user =>
      user.id match {
        case Some(id) =>
          val result = for {
            petDto <- req.request.as[PetUpdateRequestDto]
            pet = Pet.from(petDto)
            result <- petService.update(pet).value
          } yield result
          result.flatMap {
            case Right(item) => Ok(item)
            case Left(_) => Forbidden()
          }
        case None => NotFound(UserNotFoundError)
      }
  }

  def endpoints(
                 petService: PetService[F],
                 auth: SecuredRequestHandler[F, Long, User, AugmentedJWT[Auth, Long]],
               ): HttpRoutes[F] = {
    val authEndpoints: AuthService[F, Auth] = {
      val allRoles =
        createPetEndpoint(petService)
          .orElse(updatePetEndpoint(petService))

      Auth.allRoles {
        allRoles
      }
    }

    val unauthorized = getPetEndpoint(petService) <+> getPetsEndpoint(petService)

    unauthorized <+> auth.liftService(authEndpoints)
  }
}

object PetEndponts {
  def endpoints[F[_] : Sync, Auth: JWTMacAlgo](
                                                petService: PetService[F],
                                                auth: SecuredRequestHandler[F, Long, User, AugmentedJWT[Auth, Long]],
                                              ): HttpRoutes[F] =
    new PetEndpont[F, Auth].endpoints(petService, auth)
}
