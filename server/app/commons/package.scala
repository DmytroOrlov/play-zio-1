import play.api.MarkerContext
import play.api.mvc.{Action, ActionBuilder, BodyParser, Result}
import zio.macros.accessible
import zio.{RIO, Runtime, UIO, ZIO}

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
  trait AppLogger {
    def info(message: => String)(implicit mc: MarkerContext): UIO[Unit]

    def debug(message: => String)(implicit mc: MarkerContext): UIO[Unit]
  }

  object AppLogger {

    import play.api.Logger

    def make(logger: Logger) = {
      new AppLogger {
        override def info(message: => String)(implicit mc: MarkerContext): UIO[Unit] = UIO(logger.info(message))

        override def debug(message: => String)(implicit mc: MarkerContext): UIO[Unit] = UIO(logger.debug(message))
      }
    }
  }

}
