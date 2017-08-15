import Dependencies._
import sbt.Keys._
import de.heikoseeberger.sbtheader.license.Apache2_0

name := "geotrellis-pointcloud"
version := Version.geotrellis
scalaVersion := Version.scala
description := Info.description
organization := "org.locationtech.geotrellis"
licenses := Seq("Apache-2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0.html"))
homepage := Some(url(Info.url))
scalacOptions ++= Seq(
  "-deprecation",
  "-unchecked",
  "-feature",
  "-language:implicitConversions",
  "-language:reflectiveCalls",
  "-language:higherKinds",
  "-language:postfixOps",
  "-language:existentials",
  "-language:experimental.macros",
  "-feature"
)
publishMavenStyle := true
publishArtifact in Test := false
pomIncludeRepository := { _ => false }

addCompilerPlugin("org.spire-math" % "kind-projector" % "0.9.4" cross CrossVersion.binary)
addCompilerPlugin("org.scalamacros" %% "paradise" % "2.1.0" cross CrossVersion.full)

pomExtra := (
  <scm>
    <url>git@github.com:geotrellis/geotrellis.git</url>
    <connection>scm:git:git@github.com:geotrellis/geotrellis.git</connection>
  </scm>
  <developers>
    <developer>
      <id>echeipesh</id>
      <name>Eugene Cheipesh</name>
      <url>http://github.com/echeipesh/</url>
    </developer>
    <developer>
      <id>lossyrob</id>
      <name>Rob Emanuele</name>
      <url>http://github.com/lossyrob/</url>
    </developer>
    <developer>
      <id>pomadchin</id>
      <name>Grigory Pomadchin</name>
      <url>http://github.com/pomadchin/</url>
    </developer>
  </developers>
)

shellPrompt := { s => Project.extract(s).currentProject.id + " > " }
dependencyUpdatesExclusions := moduleFilter(organization = "org.scala-lang")

resolvers ++= Seq(
  Resolver.sonatypeRepo("releases"),
  Resolver.sonatypeRepo("snapshots")
)

libraryDependencies ++= Seq(
  geotrellisSpark % Provided, 
  geotrellisRaster, 
  geotrellisS3 % Provided, 
  geotrellisSparkTestkit % Test, 
  geotrellisS3Testkit % Test,
  pdalScala,
  sparkCore % Provided,
  hadoopClient % Provided,
  scalatest % Test
)

fork in Test := true
parallelExecution in Test := false
connectInput in Test := true

javaOptions += s"-Djava.library.path=${Environment.ldLibraryPath}"
