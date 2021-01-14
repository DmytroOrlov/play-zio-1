package controllers

import cats.syntax.either._
import commons._
import play.api.libs.json._
import play.api.mvc._
import users._
import zio._

import java.util.UUID

case class UserDto(name: String)

object UserDto {
  implicit val format = Json.format[UserDto]
}

trait Users {
  def list: Task[List[User]]

  def create(user: UserDto): Task[UserDto]

  def getById(id: String): Task[Option[User]]

  def update(id: String, user: User): Task[Either[String, User]]
}

object Users {
  val make =
    for {
      env <- ZIO.environment[Has[PlayLogger] with Has[UserRepository]]
    } yield new Users {
      val list = (for {
        _ <- PlayLogger.debug(s"Listing all users")
        users <- UserRepository.list
      } yield users)
        .provide(env)

      def create(user: UserDto) =
        (for {
          _ <- PlayLogger.debug(s"Creating user")
          id <- Task(UUID.randomUUID().toString)
          _ <- UserRepository.save(User(id, user.name))
        } yield user)
          .provide(env)

      def getById(id: String) =
        (for {
          _ <- PlayLogger.debug(s"Looking for user $id")
          maybeUser <- UserRepository.getById(id)
        } yield maybeUser)
          .provide(env)

      def update(id: String, user: User) =
        (for {
          _ <- PlayLogger.debug(s"Updating user $id")
          mayBeUser <- UserRepository.getById(id)
          res <- mayBeUser.fold(Task(s"User $id should exists".asLeft[User])) { _ =>
            (for {
              _ <- UserRepository.delete(id)
              _ <- UserRepository.save(User(id, user.name))
            } yield user.asRight[String])
              .provide(env)
          }
        } yield res)
          .provide(env)
    }
}

class UserController(
    controllerComponents: ControllerComponents,
    users: Users,
)(implicit val rts: Runtime[Any]) extends AbstractController(controllerComponents) with RootController {
  val list = Action.fromTask(_ => users.list)

  val create = Action.fromTask(parse.json[UserDto]) { req =>
    users.create(req.body)
  }

  def getById(id: String) =
    Action.fromTask(s"No user for $id") { _ =>
      users.getById(id)
    }

  def update(id: String) =
    Action.fromTaskEither(parse.json[User]) { req =>
      users.update(id, req.body)
    }
}
