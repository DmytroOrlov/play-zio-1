import java.io.File

import org.iq80.leveldb.impl.Iq80DBFactory.{asString, bytes, factory}
import org.iq80.leveldb.{DB, DBIterator, Options}
import play.api.libs.json.{JsError, Json}
import zio._
import zio.macros.accessible

package object users {
  import commons._

  object User {
    implicit val format = Json.format[User]
  }

  case class User(id: String, name: String)

  @accessible
  trait UserRepository {
    def list: Task[List[User]]
    def getById(id: String): Task[Option[User]]
    def save(user: User): Task[Unit]
    def delete(id: String): Task[Unit]
  }

  object UserRepository {
    val live: ZLayer[Has[AppLogger], Throwable, Has[UserRepository]] =
      ZLayer.fromManaged(
        ZManaged.make(
          AppLogger.>.info("Opening level DB at target/leveldb") *>
            Task {
              factory.open(
                new File("target/leveldb").getAbsoluteFile,
                new Options().createIfMissing(true)
              )
            }
        )(db => AppLogger.>.info("Closing level DB at target/leveldb") *> UIO(db.close()))
          .map { db =>
            new LevelDbUserRepository(db)
          }
      )

    class LevelDbUserRepository(db: DB) extends UserRepository {

      def parseJson(str: String): Task[User] =
        Task(Json.parse(str)).flatMap { json =>
          json
            .validate[User]
            .fold(
              err => Task.fail(new RuntimeException(s"Error parsing user: ${Json.stringify(JsError.toJson(err))}")),
              ok => Task.succeed(ok)
            )
        }

      override def list: Task[List[User]] =
        listAll(db.iterator())

      def listAll(iterator: DBIterator): Task[List[User]] =
        for {
          hasNext <- Task(iterator.hasNext)
          value <- if (hasNext) {
                    for {
                      nextValue <- Task(iterator.next())
                      user      <- parseJson(asString(nextValue.getValue))
                      n         <- listAll(iterator)
                    } yield user :: n
                  } else {
                    Task(List.empty[User])
                  }
        } yield value

      override def getById(id: String): Task[Option[User]] =
        for {
          stringValue <- Task { asString(db.get(bytes(id))) }
          user <- if (stringValue != null) {
                   parseJson(stringValue).map(Option.apply)
                 } else Task.succeed(Option.empty[User])
        } yield user

      override def save(user: User): Task[Unit] =
        Task {
          val stringUser = Json.stringify(Json.toJson(user))
          db.put(bytes(user.id), bytes(stringUser))
        }

      override def delete(id: String): Task[Unit] = Task(db.delete(bytes(id)))
    }
  }

}
