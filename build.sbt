
/**
 * I bet there's a better way to configure global stuff...
 */
version in ThisBuild := "0.0.1"
scalaVersion in ThisBuild := "2.11.8"
shellPrompt in ThisBuild := { Project.extract(_).currentProject.id + " >> "}


/**
 * Read as "Common". De fines core abstractions and data types
 * to connect different kinds of sources to the ReactiveMessages Publisher
 */
lazy val ReactiveSources = project.in(file("./ReactiveSources"))

/**
 * Examples and stuff...
 */
lazy val Scratchpad = project.in(file("./Scratchpad")).dependsOn(ReactiveSources)