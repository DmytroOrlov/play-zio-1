import play.api.MarkerContext
import play.api.mvc.{Action, ActionBuilder, BodyParser, Result}
import zio.macros.accessible
import zio.{Has, IO, Layer, RIO, Runtime, UIO, ZIO, ZLayer}

import scala.concurrent.Future

package object commons {
  import users._

  type AppEnv = Has[UserRepository] with Has[AppLogger]

  object AppEnv {
    val live: Layer[Throwable, AppEnv] = AppLogger.live >>> UserRepository.live.passthrough
  }

  implicit class ActionBuilderOps[+R[_], B](val ab: ActionBuilder[R, B]) extends AnyVal {
    def asyncTask[Ctx](cb: R[B] => RIO[Ctx, Result])(implicit r: Layer[Throwable, Ctx]): Action[B] =
      ab.async { c =>
        val value: IO[Throwable, Result] = cb(c).provideLayer(r)
        val future: Future[Result] = Runtime.default.unsafeRunToFuture(value)
        future
      }

    def asyncTask[A, Ctx](
        bp: BodyParser[A]
    )(cb: R[A] => RIO[Ctx, Result])(implicit r: Layer[Throwable, Ctx]): Action[A] =
      ab.async[A](bp) { c =>
        val value: IO[Throwable, Result] = cb(c).provideLayer(r)
        val future: Future[Result] = Runtime.default.unsafeRunToFuture(value)
        future
      }

    def asyncZio[Ctx](cb: R[B] => ZIO[Ctx, Result, Result])(implicit r: Layer[Throwable, Ctx]): Action[B] =
      ab.async { c =>
        val value: IO[Throwable, Result] = cb(c).either.map(_.merge).provideLayer(r)
        val future: Future[Result] = Runtime.default.unsafeRunToFuture(value)
        future
      }

    def asyncZio[A, Ctx](
        bp: BodyParser[A]
    )(cb: R[A] => ZIO[Ctx, Result, Result])(implicit r: Layer[Throwable, Ctx]): Action[A] =
      ab.async[A](bp) { c =>
        val value: IO[Throwable, Result] = cb(c).either.map(_.merge).provideLayer(r)
        val future: Future[Result] = Runtime.default.unsafeRunToFuture(value)
        future
      }
  }

  @accessible
  trait AppLogger {
    def info(message: => String)(implicit mc: MarkerContext): UIO[Unit]

    def debug(message: => String)(implicit mc: MarkerContext): UIO[Unit]
  }

  object AppLogger {

    import play.api.Logger

    val live: ZLayer[Any, Nothing, Has[AppLogger]] = ZLayer.succeed(new ProdLogger())

    class ProdLogger(logger: Logger = Logger("application")) extends AppLogger {
      override def info(message: => String)(implicit mc: MarkerContext): UIO[Unit] = UIO(logger.info(message))

      override def debug(message: => String)(implicit mc: MarkerContext): UIO[Unit] = UIO(logger.debug(message))
    }

  }

}
