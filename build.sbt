organization := "com.yang-bo"

name := "TailCall"

crossPaths := false

autoScalaLibrary := false

libraryDependencies += "org.junit.jupiter" % "junit-jupiter-engine" % "5.2.0" % Test

libraryDependencies += "org.junit.platform" % "junit-platform-runner" % "1.2.0" % Test

libraryDependencies += "net.aichler" % "jupiter-interface" % JupiterKeys.jupiterVersion.value % Test

libraryDependencies += "net.jodah" % "typetools" % "0.6.3"
