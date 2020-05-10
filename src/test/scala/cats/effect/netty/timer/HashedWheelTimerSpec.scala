package cats.effect.netty.timer

import java.util.concurrent.atomic.AtomicInteger

import cats.effect._
import cats.effect.internals.IOAppPlatform
import cats.effect.testing.minitest.IOTestSuite
import cats.implicits._

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

object HashedWheelTimerSpec extends IOTestSuite {
  implicit val contextShift: ContextShift[IO] = IOAppPlatform.defaultContextShift
  implicit val timer: Timer[IO]               = HashedWheelTimer.unsafe[IO](ExecutionContext.global)

  test("timer") {
    IO(true).delayBy(100.millis).map(assert(_))
  }

  test("cancel timer") {
    val i = new AtomicInteger(0)
    for {
      g <- IO(i.incrementAndGet()).delayBy(100.millis).start
      _ <- g.cancel
      _ <- IO.sleep(1.second)
    } yield assert(i.get() === 0)
  }
}
