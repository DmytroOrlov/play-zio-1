package loader

import commons._
import controllers.{AssetsComponents, UserController, Users}
import distage.{Injector, ModuleDef}
import play.api.ApplicationLoader.Context
import play.api._
import play.api.mvc.{ControllerComponents, EssentialFilter}
import play.api.routing.Router
import router.Routes
import users.UserRepository
import zio._

class AppLoader extends ApplicationLoader {

  def load(context: Context): Application = {
    LoggerConfigurator(context.environment.classLoader).foreach {
      _.configure(context.environment, context.initialConfiguration, Map.empty)
    }
    new BuiltInComponentsFromContext(context) with AssetsComponents {
      lazy val rts = Runtime.default

      val definition = new ModuleDef {
        make[Runtime[Any]].from(rts)
        make[ControllerComponents].from(controllerComponents)
        make[Logger].fromValue(Logger("users"))
        make[PlayLogger].from(PlayLogger.make _)
        make[UserRepository].fromHas(UserRepository.make)
        make[Users].fromHas(Users.make)
        make[UserController]
      }
      val (controllers, release) = rts.unsafeRun(
        Injector()
          .produceGetF[Task, UserController](definition)
          .unsafeNoRelease
      )

      applicationLifecycle.addStopHook(() => rts.unsafeRunToFuture(release))

      override def router: Router =
        new Routes(
          httpErrorHandler,
          controllers,
          controllers,
          assets
        )

      override def httpFilters: Seq[EssentialFilter] = Seq()
    }.application
  }
}
