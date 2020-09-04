import izumi.distage.model.definition.DIResource.DIResourceBase
import izumi.distage.model.effect.DIEffect

package object loader {

  implicit final class DIResourceNoRelease[F[_], A](private val resource: DIResourceBase[F, A]) extends AnyVal {
    def unsafeNoRelease[B](implicit F: DIEffect[F]): F[(A, F[Unit])] = {
      F.flatMap(resource.acquire)(a => F.maybeSuspend(resource.extract(a) -> resource.release(a)))
    }
  }

}
