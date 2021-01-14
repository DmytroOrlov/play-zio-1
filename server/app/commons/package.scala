import play.api.libs.json.{Json, Writes}
import play.api.mvc.Results._
import play.api.mvc.{Action, ActionBuilder, BodyParser, Result}
import play.api.{Logger, MarkerContext}
import zio.macros.accessible
import zio.{RIO, Runtime, Task, UIO, ZIO}

package object commons {

  implicit class ActionBuilderOps[+R[_], B](val ab: ActionBuilder[R, B]) extends AnyVal {
    def asyncTask[Env](cb: R[B] => RIO[Env, Result])(implicit rts: Runtime[Env]): Action[B] =
      ab.async { c =>
        val zio = cb(c)
        rts.unsafeRunToFuture(zio)
      }

    def asyncTask[Env](
        bp: BodyParser[B]
    )(cb: R[B] => RIO[Env, Result])(implicit rts: Runtime[Env]): Action[B] =
      ab.async(bp) { c =>
        val zio = cb(c)
        rts.unsafeRunToFuture(zio)
      }

    def asyncZio[Env](cb: R[B] => ZIO[Env, Result, Result])(implicit rts: Runtime[Env]): Action[B] =
      ab.async { c =>
        val zio = cb(c).either.map(_.merge)
        rts.unsafeRunToFuture(zio)
      }

    def asyncZio[A, Env](
        bp: BodyParser[A]
    )(cb: R[A] => ZIO[Env, Result, Result])(implicit rts: Runtime[Env]): Action[A] =
      ab.async[A](bp) { c =>
        val zio = cb(c).either.map(_.merge)
        rts.unsafeRunToFuture(zio)
      }

    def fromTask[A: Writes](cb: R[B] => Task[A])(implicit rts: Runtime[Any]): Action[B] =
      ab.async { c =>
        val zio = cb(c).bimap(
          e => InternalServerError(s"$e"),
          a => Ok(Json.toJson(a))
        ).either
          .map(_.merge)
        rts.unsafeRunToFuture(zio)
      }

    def fromTask[A: Writes, E](e: => String)(cb: R[B] => Task[Option[A]])(implicit rts: Runtime[Any]): Action[B] =
      ab.async { c =>
        val zio = cb(c).mapError(e => InternalServerError(s"$e"))
          .flatMap { maybeA =>
            ZIO.fromOption(maybeA).bimap(
              _ => NotFound(Json.obj("message" -> e)),
              a => Ok(Json.toJson(a))
            )
          }.either
          .map(_.merge)
        rts.unsafeRunToFuture(zio)
      }

    def fromTaskEither[A: Writes, C](
        bp: BodyParser[C]
    )(cb: R[C] => Task[Either[String, A]])(implicit rts: Runtime[Any]): Action[C] =
      ab.async(bp) { c =>
        val zio = cb(c).mapError(e => InternalServerError(s"$e"))
          .flatMap(
            either => ZIO.fromEither(either).bimap(
              e => NotFound(Json.obj("message" -> e)),
              a => Ok(Json.toJson(a))
            )
          ).either
          .map(_.merge)
        rts.unsafeRunToFuture(zio)
      }

    def fromTask[A: Writes, C](
        bp: BodyParser[C]
    )(cb: R[C] => Task[A])(implicit rts: Runtime[Any]): Action[C] =
      ab.async(bp) { c =>
        val zio = cb(c).bimap(
          e => InternalServerError(s"$e"),
          a => Ok(Json.toJson(a))
        ).either
          .map(_.merge)
        rts.unsafeRunToFuture(zio)
      }
  }

  @accessible
  trait PlayLogger {
    def info(message: => String)(implicit mc: MarkerContext): UIO[Unit]

    def debug(message: => String)(implicit mc: MarkerContext): UIO[Unit]
  }

  object PlayLogger {
    def make(logger: Logger) =
      new PlayLogger {
        def info(message: => String)(implicit mc: MarkerContext) = UIO(logger.info(message))

        def debug(message: => String)(implicit mc: MarkerContext) = UIO(logger.debug(message))
      }
  }

}
