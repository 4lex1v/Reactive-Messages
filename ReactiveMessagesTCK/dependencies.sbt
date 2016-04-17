import Dependencies.{versions => V}

libraryDependencies ++= Seq(
  "org.reactivestreams" % "reactive-streams-tck" % V.RStreams % "test",
  "org.scalatest" %% "scalatest" % V.ScalaTest % "test"
)