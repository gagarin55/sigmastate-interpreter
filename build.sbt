name := "sigma-state"

version := "0.0.1"

scalaVersion := "2.12.2"

val testingDependencies = Seq(
  "org.scalatest" %% "scalatest" % "3.0.1" % "test",
  "org.scalactic" %% "scalactic" % "3.0.1" % "test",
  "org.scalacheck" %% "scalacheck" % "1.13.+" % "test"
)

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % "2.4.+",
  "org.scorexfoundation" %% "scorex-core" % "2.+",
  "org.bitbucket.inkytonik.kiama" %% "kiama" % "2.1.0-SNAPSHOT"
) ++ testingDependencies


//uncomment lines below if the Scala compiler hangs to see where it happens
//scalacOptions in Compile ++= Seq("-Xprompt", "-Ydebug", "-verbose" )
















