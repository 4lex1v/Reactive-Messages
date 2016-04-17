import Dependencies.{versions => V}

libraryDependencies ++= Seq(
  "org.twitter4j" % "twitter4j-stream" % V.T4J,
  "org.scalatest" %% "scalatest" % V.ScalaTest % "test"
)