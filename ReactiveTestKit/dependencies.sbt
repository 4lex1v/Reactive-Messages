import Dependencies.{versions => V}

libraryDependencies ++= Seq(
  "org.reactivestreams" % "reactive-streams" % V.RStreams,
  "org.reactivestreams" % "reactive-streams-tck" % V.RStreams % "test",
  "com.typesafe.akka" %% "akka-testkit" % V.Akka % "test",
  "org.scalatest" %% "scalatest" % V.ScalaTest % "test"
)