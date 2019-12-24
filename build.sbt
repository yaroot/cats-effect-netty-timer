// format: off
name := "cats-effect-netty-timer"
organization := "com.github.yaroot"
scalaVersion := "2.12.10"
crossScalaVersions := Seq("2.12.10", "2.13.1")

// scalacOptions in (Compile, console) --= Seq("-Ywarn-unused:imports", "-Xfatal-warnings")

fork in run := true

libraryDependencies ++= {
  Seq(
    "io.netty"      % "netty-common" % "4.1.44.Final",
    "org.typelevel" %% "cats-effect" % "2.0.0",
    "org.specs2"    %% "specs2-core" % "4.8.1" % "test"
  )
}

scalafmtOnCompile := true
cancelable in Global := true

// wartremoverErrors in (Compile, compile) ++= Warts.all
wartremoverErrors ++= Warts.all
