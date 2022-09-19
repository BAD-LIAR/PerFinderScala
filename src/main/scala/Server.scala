import cats.effect.{Blocker, ConcurrentEffect, ContextShift, ExitCode, IO, IOApp, Resource, Timer}
import doobie.util.ExecutionContexts
import io.circe.config.parser
import org.http4s.server.{Router, Server => H4Server}
import org.http4s.server.blaze.BlazeServerBuilder
import org.http4s.implicits._
import tsec.authentication.SecuredRequestHandler
import tsec.mac.jca.HMACSHA256
import tsec.passwordhashers.jca.BCrypt
import config._
import config.psDec
import domain.Auth
import domain.pet.setvice.PetService
import domain.pet.validation.PetValidationInterpreter
import domain.user.Service.UserService
import domain.user.Validation.UserValidationInterpreter
import infrastructure.endpoint.{PetEndponts, UserEndpoints}
import infrastructure.repository.{DoobieAuthRepositoryInterpreter, DoobiePetRepositoryInterpreter, DoobieUserRepositoryInterpreter}


object Server extends IOApp {
  def createServer[F[_]: ContextShift: ConcurrentEffect: Timer]: Resource[F, H4Server[F]] =
    for {
      conf <- Resource.eval(parser.decodePathF[F, PetFinderConfig]("petFinder"))
      serverEc <- ExecutionContexts.cachedThreadPool[F]
      connEc <- ExecutionContexts.fixedThreadPool[F](conf.db.connections.poolSize)
      txnEc <- ExecutionContexts.cachedThreadPool[F]
      xa <- DatabaseConfig.dbTransactor(conf.db, connEc, Blocker.liftExecutionContext(txnEc))
      key <- Resource.eval(HMACSHA256.generateKey[F])
      authRepo = DoobieAuthRepositoryInterpreter[F, HMACSHA256](key, xa)
      userRepo = DoobieUserRepositoryInterpreter[F](xa)
      userValidation = UserValidationInterpreter[F](userRepo)
      userService = UserService[F](userRepo, userValidation)
      petRepo = DoobiePetRepositoryInterpreter[F](xa)
      petValidation = PetValidationInterpreter(petRepo)
      petService = PetService(petRepo, petValidation)

      authenticator = Auth.jwtAuthenticator[F, HMACSHA256](key, authRepo, userRepo)
      routeAuth = SecuredRequestHandler(authenticator)
      httpApp = Router(
        "/pets" -> PetEndponts.endpoints[F, HMACSHA256](petService, routeAuth),
        "/users" -> UserEndpoints
          .endpoints[F, BCrypt, HMACSHA256](userService, routeAuth),
      ).orNotFound
      _ <- Resource.eval(DatabaseConfig.initializeDb(conf.db))
      server <- BlazeServerBuilder[F](serverEc)
        .bindHttp(8000, conf.serverConfig.host)
        .withHttpApp(httpApp)
        .resource
    } yield server

  def run(args: List[String]): IO[ExitCode] = createServer.use(_ => IO.never).as(ExitCode.Success)
}
