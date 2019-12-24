package cats.effect.netty.timer

import java.util.concurrent.ThreadFactory
import java.util.concurrent.atomic.AtomicInteger

import cats.effect._
import cats.implicits._
import io.netty.util.{HashedWheelTimer => NettyTimer, Timeout}

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import scala.util.Try

object HashedWheelTimer {
  def unsafe[F[_]: Concurrent](ec: ExecutionContext): Timer[F] = {
    val timer = defaultNettyTimer()
    timer.start()
    fromNetty[F](timer, ec)
  }

  def apply[F[_]: Concurrent](ec: ExecutionContext): Resource[F, Timer[F]] = {
    nettyTimerResource[F]
      .map(fromNetty[F](_, ec))
  }

  def nettyTimerResource[F[_]](implicit F: Sync[F]): Resource[F, NettyTimer] = {
    val create = for {
      t <- F.delay(defaultNettyTimer())
      _ <- F.delay(t.start())
    } yield t

    val release = (t: NettyTimer) => F.delay(t.stop()).as(())
    Resource.make(create)(release)
  }

  def fromNetty[F[_]: Concurrent](timer: NettyTimer, ec: ExecutionContext): Timer[F] = {
    val F    = Concurrent[F]
    val done = ().asRight[Throwable]

    new Timer[F] {
      override val clock: Clock[F] = Clock.create[F]
      override def sleep(duration: FiniteDuration): F[Unit] = {
        F.cancelable { callback =>
          val timeout = F.fromTry(
            Try(
              timer.newTimeout(
                (_: Timeout) => ec.execute(() => callback(done)),
                duration.length,
                duration.unit
              )
            )
          )

          timeout.flatMap { t =>
            F.delay(t.cancel()).as(())
          }
        }
      }
    }
  }

  def defaultNettyTimer(): NettyTimer =
    new NettyTimer(
      namedThreadFactory("netty-timer-%d", daemon = true),
      10,
      MILLISECONDS,
      512
    )

  def namedThreadFactory(format: String, daemon: Boolean): ThreadFactory = {
    val _       = format.format(1)
    val default = java.util.concurrent.Executors.defaultThreadFactory()
    val i       = new AtomicInteger(0)
    new ThreadFactory {
      override def newThread(runnable: Runnable): Thread = {
        val t = default.newThread(runnable)
        t.setDaemon(daemon)
        t.setName(format.format(i.getAndIncrement()))
        t
      }
    }
  }
}
