import Dependencies._
import sbt.Keys._

name := "geotrellis-pointcloud"
version := Version.geotrellisPointCloud
scalaVersion := Version.scala
description := "GeoTrellis PointCloud library"
organization := "com.azavea.geotrellis"
licenses := Seq("Apache-2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0.html"))
homepage := Some(url("http://geotrellis.github.io"))
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
bintrayRepository := "geotrellis"
bintrayOrganization := None
bintrayPackageLabels := Seq("geotrellis", "maps", "gis", "geographic", "data", "raster", "processing", "pdal", "pointcloud")
bintrayVcsUrl := Some("https://github.com/geotrellis/geotrellis-pointcloud")

addCompilerPlugin("org.spire-math" % "kind-projector" % "0.9.6" cross CrossVersion.binary)
addCompilerPlugin("org.scalamacros" %% "paradise" % "2.1.1" cross CrossVersion.full)

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
headerLicense := Some(HeaderLicense.ALv2("2017", "Azavea"))

sources in (Compile, doc) ~= (_ filterNot (_.getAbsolutePath contains "geotrellis/vector"))

resolvers ++= Seq(
  Resolver.sonatypeRepo("releases"),
  Resolver.sonatypeRepo("snapshots"),
  "Locationtech Releases" at "https://repo.locationtech.org/content/groups/releases",
  "Locationtech Snapshots" at "https://repo.locationtech.org/content/groups/snapshots"
)

libraryDependencies ++= Seq(
  geotrellisSpark % Provided,
  geotrellisRaster % Provided,
  geotrellisS3 % Provided,
  geotrellisSparkTestkit % Test,
  geotrellisS3Testkit % Test,
  pdalScala,
  pdalNative,
  sparkCore % Provided,
  hadoopClient % Provided,
  scalatest % Test
)

fork in Test := true
parallelExecution in Test := false
connectInput in Test := true
