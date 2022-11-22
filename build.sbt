ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "2.13.10"

lazy val root = (project in file("."))
  .settings(
    name := "order-fsm",
    idePackagePrefix := Some("com.witcher.order")
  )

val AkkaVersion = "2.7.0"
libraryDependencies += "com.typesafe.akka" %% "akka-actor" % AkkaVersion
libraryDependencies += "com.typesafe.akka" %% "akka-testkit" % AkkaVersion % Test
libraryDependencies += "org.scalatest" %% "scalatest" % "3.2.14" % Test
