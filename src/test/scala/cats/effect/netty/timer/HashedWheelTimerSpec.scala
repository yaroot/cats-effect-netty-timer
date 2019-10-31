package cats.effect.netty.timer

import java.util.concurrent.TimeoutException
import java.util.concurrent.atomic.AtomicInteger

import cats.effect._
import cats.effect.internals.IOAppPlatform
import cats.implicits._
import org.specs2.mutable.Specification

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

class HashedWheelTimerSpec extends Specification {
  implicit val contextShift: ContextShift[IO] = IOAppPlatform.defaultContextShift
  implicit val timer: Timer[IO]               = HashedWheelTimer.unsafe[IO](ExecutionContext.global)

  "Timer should" >> {
    "work" >> {
      val f = IO.never.as(1).timeout(100.millis)
      f.unsafeRunSync() should throwA[TimeoutException]
    }

    "be cancelled when interrupted" >> {
      val i = new AtomicInteger(0)
      val f = for {
        g <- (timer.sleep(1.second) >> IO.delay(i.incrementAndGet())).start
        _ <- g.cancel
        _ <- timer.sleep(2.second)
      } yield i.get()
      f.unsafeRunSync() should_== 0
    }
  }
}
