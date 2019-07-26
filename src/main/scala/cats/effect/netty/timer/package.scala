package cats.effect.netty.timer

import java.util.concurrent.{ThreadFactory, TimeUnit}
import java.util.concurrent.atomic.AtomicInteger

import cats.effect._
import cats.implicits._
import io.netty.util.{HashedWheelTimer, Timeout, TimerTask}

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.FiniteDuration
import scala.util.Try

object HWTimer {
  def unsafeCreate[F[_]: Concurrent](ec: ExecutionContext): Timer[F] = {
    val timer = newTimer()
    timer.start()
    fromHWTimer(timer, ec)
  }

  def create[F[_]](ec: ExecutionContext)(implicit F: Concurrent[F]): Resource[F, HashedWheelTimer] = {
    val create = for {
      t <- F.delay(newTimer())
      _ <- F.delay(t.start())
    } yield t

    val release = (t: HashedWheelTimer) => F.delay(t.stop()).as(())
    Resource.make(create)(release)
  }

  def fromHWTimer[F[_]: Concurrent](timer: HashedWheelTimer, ec: ExecutionContext): Timer[F] = {
    val F    = Concurrent[F]
    val done = ().asRight[Throwable]

    new Timer[F] {
      override val clock: Clock[F] = Clock.create[F]
      override def sleep(duration: FiniteDuration): F[Unit] = {
        F.cancelable { cb =>
          val timeout = F.fromTry {
            Try {
              // newTimeout throws
              timer.newTimeout(
                new TimerTask {
                  override def run(t: Timeout): Unit =
                    ec.execute(new Runnable {
                      override def run(): Unit = cb(done)
                    })
                },
                duration.length,
                duration.unit
              )
            }
          }

          timeout.flatMap { t =>
            F.delay(t.cancel()).as(())
          }
        }
      }
    }
  }

  def newTimer(): HashedWheelTimer = {
    new HashedWheelTimer(
      namedThreadFactory("netty-hwtimer-%d"),
      50,
      TimeUnit.MILLISECONDS,
      512
    )
  }

  def namedThreadFactory(format: String): ThreadFactory = {
    val _       = format.format(1)
    val default = java.util.concurrent.Executors.defaultThreadFactory()
    val i       = new AtomicInteger(0)
    new ThreadFactory {
      override def newThread(runnable: Runnable): Thread = {
        val t = default.newThread(runnable)
        t.setDaemon(true)
        t.setName(format.format(i.getAndIncrement()))
        t
      }
    }
  }
}
