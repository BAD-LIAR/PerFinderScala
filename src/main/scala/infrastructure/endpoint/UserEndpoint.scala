package infrastructure.endpoint

import cats.data.EitherT
import cats.effect.Sync
import cats.syntax.all._
import io.circe.generic.auto._
import io.circe.syntax._
import org.http4s.circe._
import org.http4s.dsl.Http4sDsl
import org.http4s.{EntityDecoder, HttpRoutes}
import domain._
import domain.auth._
import domain.user.Dto.UserResponseDto
import domain.user.Models.User
import domain.user.Service.UserService
import domain.user.Validation.{UserAlreadyExistsError, UserAuthenticationFailedError, UserNotFoundError}
import domain.user._
import org.mindrot.jbcrypt.BCrypt
import tsec.common.Verified
import tsec.jwt.algorithms.JWTMacAlgo
import tsec.authentication._

class UserEndpoints[F[_] : Sync, A, Auth: JWTMacAlgo] extends Http4sDsl[F] {

  import Pagination._

  /* Jsonization of our User type */

  implicit val userDecoder: EntityDecoder[F, User] = jsonOf
  implicit val loginReqDecoder: EntityDecoder[F, LoginRequest] = jsonOf
  implicit val signupReqDecoder: EntityDecoder[F, SignupRequest] = jsonOf

  private def loginEndpoint(
                             userService: UserService[F],
                             auth: Authenticator[F, Long, User, AugmentedJWT[Auth, Long]],
                           ): HttpRoutes[F] =
    HttpRoutes.of[F] { case req@POST -> Root / "login" =>
      val action = for {
        login <- EitherT.liftF(req.as[LoginRequest])
        name = login.username
        user <- userService.getUserByName(name).leftMap(_ => UserAuthenticationFailedError(name))
//        checkResult <- EitherT.liftF(
//        cryptService.checkpw(login.password, PasswordHash[A](user.hashPassword)),
//        )
        _ <-
          if (login.password.equals(user.hashPassword)) EitherT.rightT[F, UserAuthenticationFailedError](())
          else EitherT.leftT[F, User](UserAuthenticationFailedError(name))
        token <- user.id match {
          case None => throw new Exception("Impossible") // User is not properly modeled
          case Some(id) => EitherT.right[UserAuthenticationFailedError](auth.create(id))
        }
      } yield (user, token)

      action.value.flatMap {
        case Right((user, token)) => Ok(UserResponseDto.from(user).asJson).map(auth.embed(_, token))
        case Left(UserAuthenticationFailedError(name)) =>
          BadRequest(s"Authentication failed for user $name")
      }
    }

  private def signupEndpoint(
                              userService: UserService[F],
                            ): HttpRoutes[F] =
    HttpRoutes.of[F] { case req@POST -> Root / "signup"=>
      val action = for {
        signup <- req.as[SignupRequest]
//        hash <- BCrypt.hashpw(signup.password, BCrypt.gensalt(12)).toString
        user <- signup.asUser(signup.password).pure[F]
        result <- userService.createUser(user).value
      } yield result

      action.flatMap {
        case Right(saved) => Ok(UserResponseDto.from(saved).asJson)
        case Left(UserAlreadyExistsError(existing)) =>
          Conflict(s"The user with user name ${existing.username} already exists")
      }
    }

  private def updateEndpoint(userService: UserService[F]): AuthEndpoint[F, Auth] = {
    case req@PUT -> Root / name asAuthed _ =>
      val action = for {
        user <- req.request.as[User]
        updated = user.copy(username = name)
        result <- userService.update(updated).value
      } yield result

      action.flatMap {
        case Right(saved) => Ok(saved.asJson)
        case Left(UserNotFoundError) => NotFound("User not found")
      }
  }

  private def listEndpoint(userService: UserService[F]): AuthEndpoint[F, Auth] = {
    case GET -> Root :? OptionalPageSizeMatcher(pageSize) :? OptionalOffsetMatcher(offset,
    ) asAuthed _ =>
      for {
        retrieved <- userService.list(pageSize.getOrElse(10), offset.getOrElse(0))
        resp <- Ok(retrieved.asJson)
      } yield resp
  }

  private def searchByNameEndpoint(userService: UserService[F]): AuthEndpoint[F, Auth] = {
    case GET -> Root / userName asAuthed _ =>
      userService.getUserByName(userName).value.flatMap {
        case Right(found) => Ok(found.asJson)
        case Left(UserNotFoundError) => NotFound("The user was not found")
      }
  }

  private def deleteUserEndpoint(userService: UserService[F]): AuthEndpoint[F, Auth] = {
    case DELETE -> Root / userName asAuthed _ =>
      for {
        _ <- userService.deleteByUserName(userName)
        resp <- Ok()
      } yield resp
  }

  def endpoints(
                 userService: UserService[F],
                 auth: SecuredRequestHandler[F, Long, User, AugmentedJWT[Auth, Long]],
               ): HttpRoutes[F] = {
    val authEndpoints: AuthService[F, Auth] =
      Auth.adminOnly {
        updateEndpoint(userService)
          .orElse(listEndpoint(userService))
          .orElse(searchByNameEndpoint(userService))
          .orElse(deleteUserEndpoint(userService))
      }

    val unauthEndpoints =
      loginEndpoint(userService, auth.authenticator) <+>
        signupEndpoint(userService)

    unauthEndpoints <+> auth.liftService(authEndpoints)
  }
}

object UserEndpoints {
  def endpoints[F[_] : Sync, A, Auth: JWTMacAlgo](userService: UserService[F],
                                                  auth: SecuredRequestHandler[F, Long, User, AugmentedJWT[Auth, Long]],
                                                 ): HttpRoutes[F] =
    new UserEndpoints[F, A, Auth].endpoints(userService, auth)
}
