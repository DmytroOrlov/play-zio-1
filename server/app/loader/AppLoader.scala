package loader

import com.example.playscalajs.controllers.RootController
import commons._
import controllers.{AssetsComponents, Controllers, UserController}
import distage.{Injector, ModuleDef}
import play.api.ApplicationLoader.Context
import play.api.mvc.{ControllerComponents, EssentialFilter}
import play.api.routing.Router
import play.api.{Application, ApplicationLoader, BuiltInComponentsFromContext, Logger, LoggerConfigurator}
import router.Routes
import users.UserRepository
import zio._

class AppLoader extends ApplicationLoader {

  def load(context: Context): Application = {
    LoggerConfigurator(context.environment.classLoader).foreach {
      _.configure(context.environment, context.initialConfiguration, Map.empty)
    }
    new BuiltInComponentsFromContext(context) with AssetsComponents {
      val rts = Runtime.default

      val definition = new ModuleDef {
        make[ControllerComponents].from(controllerComponents)
        make[Logger].from(Logger("users"))
        make[PlayLogger].from(PlayLogger.make _)
        make[UserRepository].fromHas(UserRepository.make)
        make[UserController with RootController].fromHas(Controllers.make _)
      }
      val (controllers, release) = rts.unsafeRun(
        Injector()
          .produceGetF[Task, UserController with RootController](definition)
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
