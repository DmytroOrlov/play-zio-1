package controllers

import java.util.UUID

import com.example.playscalajs.controllers.RootController
import commons._
import play.api.libs.json.{JsError, JsResult, JsValue, Json}
import play.api.mvc._
import users._
import zio._

trait UserController {
  def list: Action[AnyContent]

  def getById(id: String): Action[AnyContent]

  def create: Action[JsValue]

  def update(id: String): Action[JsValue]
}

object Controllers {

  case class UserDto(name: String)

  object UserDto {
    implicit val format = Json.format[UserDto]
  }

  def make(controllerComponents: ControllerComponents) =
    for {
      rts <- ZIO.runtime[Has[PlayLogger] with Has[UserRepository]]
    } yield {
      implicit val r = rts

      new AbstractController(controllerComponents) with UserController with RootController {
        val list = Action.asyncZio { _ =>
          for {
            _ <- PlayLogger.debug(s"Listing all users")
            users <- UserRepository.list.mapError(e => InternalServerError(s"$e"))
          } yield Ok(Json.toJson(users))
        }
        val create = Action.asyncZio(parse.json) { req =>
          val userParsed: JsResult[UserDto] = req.body.validate[UserDto]
          for {
            _ <- PlayLogger.debug(s"Creating user")
            user <- ZIO.fromEither(userParsed.asEither).mapError(err => BadRequest(JsError.toJson(err)))
            id <- Task(UUID.randomUUID().toString).mapError(e => InternalServerError(""))
            _ <- UserRepository.save(User(id, user.name)).mapError(e => InternalServerError(""))
          } yield Ok(Json.toJson(user))
        }

        def getById(id: String) = Action.asyncZio { _ =>
          for {
            _ <- PlayLogger.debug(s"Looking for user $id")
            mayBeUser <- UserRepository.getById(id).mapError(e => InternalServerError(""))
            user <- ZIO.fromOption(mayBeUser).mapError(_ => NotFound(Json.obj("message" -> s"No user for $id")))
          } yield Ok(Json.toJson(user))
        }

        def update(id: String) = Action.asyncZio(parse.json) { req =>
          val userParsed: JsResult[User] = req.body.validate[User]
          for {
            _ <- PlayLogger.debug(s"Updating user $id")
            user <- ZIO.fromEither(userParsed.asEither).mapError(err => BadRequest(JsError.toJson(err)))
            mayBeUser <- UserRepository.getById(id).mapError(_ => InternalServerError(""))
            _ <- ZIO.fromOption(mayBeUser).mapError(_ => BadRequest(Json.obj("message" -> s"User $id should exists")))
            _ <- UserRepository.delete(id).mapError(_ => InternalServerError(""))
            _ <- UserRepository.save(User(id, user.name)).mapError(e => InternalServerError(""))
          } yield Ok(Json.toJson(user))
        }
      }
    }
}
