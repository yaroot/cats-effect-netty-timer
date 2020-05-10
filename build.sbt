// format: off
name := "cats-effect-netty-timer"
organization := "com.github.yaroot"
scalaVersion := "2.12.11"
crossScalaVersions := Seq("2.12.11", "2.13.2")


fork in run := true

libraryDependencies ++= {
  Seq(
    "io.netty"      % "netty-common" % "4.1.49.Final",
    "org.typelevel" %% "cats-effect" % "2.1.3",
    "org.specs2"    %% "specs2-core" % "4.9.4" % "test"
  )
}

scalafmtOnCompile := true
cancelable in Global := true

// wartremoverErrors in (Compile, compile) ++= Warts.all
wartremoverErrors ++= Warts.all
