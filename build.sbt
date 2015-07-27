name := "xs4s-test"

version := "1.0"

scalaVersion := "2.11.7"

libraryDependencies ++= {
  val akkaVersion = "2.3.12"
  val akkaStreamVersion = "1.0"
  Seq (
    "com.typesafe.akka" %% "akka-actor" % akkaVersion,
    "com.typesafe.akka" %% "akka-camel" % akkaVersion,
    "com.typesafe.akka" %% "akka-stream-experimental" % akkaStreamVersion,
    "com.typesafe.akka" %% "akka-testkit" % akkaVersion % "test",
    "org.scalatest" %% "scalatest" % "2.2.4" % "test"
  )
}

lazy val root: Project = project.in(file("."))
  .dependsOn(xs4sProject)

lazy val xs4sProject: RootProject =
  RootProject(uri("git://github.com/dnvriend/xs4s.git"))

