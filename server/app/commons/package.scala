import play.api.mvc.{Action, ActionBuilder, BodyParser, Result}
import zio.{RIO, Runtime, UIO, ZIO}
import play.api.MarkerContext
import zio.macros.accessible
import play.api.Logger

package object commons {

  implicit class ActionBuilderOps[+R[_], B](val ab: ActionBuilder[R, B]) extends AnyVal {
    def asyncTask[Env](cb: R[B] => RIO[Env, Result])(implicit rts: Runtime[Env]): Action[B] =
      ab.async { c =>
        val zio = cb(c)
        rts.unsafeRunToFuture(zio)
      }

    def asyncTask[A, Env](
        bp: BodyParser[A]
    )(cb: R[A] => RIO[Env, Result])(implicit rts: Runtime[Env]): Action[A] =
      ab.async[A](bp) { c =>
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
