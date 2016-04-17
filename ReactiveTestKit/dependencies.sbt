import Dependencies.{versions => V}

libraryDependencies ++= Seq(
  "org.reactivestreams" % "reactive-streams" % V.RStreams,
  "org.scalatest" %% "scalatest" % V.ScalaTest % "test"
)