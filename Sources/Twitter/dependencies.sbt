import Dependencies.{versions => V}

libraryDependencies ++= Seq(
  "org.twitter4j" % "twitter4j-stream" % V.T4J,
  "com.typesafe" % "config" % V.Config,
  "org.scalatest" %% "scalatest" % V.ScalaTest % "test"
)