import Dependencies.{versions => V}

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % V.Akka,
  "org.reactivestreams" % "reactive-streams" % V.RStreams,
  "com.typesafe.akka" %% "akka-testkit" % V.Akka % "test",
  "org.scalatest" %% "scalatest" % V.ScalaTest % "test"
)