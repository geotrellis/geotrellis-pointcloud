import Dependencies._
import sbt.Keys._
import de.heikoseeberger.sbtheader.{CommentCreator, CommentStyle, FileType}
import de.heikoseeberger.sbtheader.HeaderPlugin.autoImport.{HeaderLicense, headerLicense, headerMappings}

name := "geotrellis-pointcloud"
version := Version.geotrellisPointCloud
scalaVersion := Version.scala
crossScalaVersions := Version.crossScala
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
  "-feature",
  "-Ypartial-unification", // Required by Cats
  "-target:jvm-1.8"
)
publishMavenStyle := true
Test / publishArtifact := false
pomIncludeRepository := { _ => false }
bintrayRepository := "geotrellis"
bintrayOrganization := Some("azavea")
bintrayPackageLabels := Seq("geotrellis", "maps", "gis", "geographic", "data", "raster", "processing", "pdal", "pointcloud")
bintrayVcsUrl := Some("https://github.com/geotrellis/geotrellis-pointcloud")

addCompilerPlugin("org.typelevel" %% "kind-projector" % "0.11.0" cross CrossVersion.full)
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
headerLicense := Some(HeaderLicense.ALv2(java.time.Year.now.getValue.toString, "Azavea"))
headerMappings := Map(
  FileType.scala -> CommentStyle.cStyleBlockComment.copy(commentCreator = new CommentCreator() {
    val Pattern = "(?s).*?(\\d{4}(-\\d{4})?).*".r
    def findYear(header: String): Option[String] = header match {
      case Pattern(years, _) => Some(years)
      case _                 => None
    }
    def apply(text: String, existingText: Option[String]): String = {
      // preserve year of old headers
      val newText = CommentStyle.cStyleBlockComment.commentCreator.apply(text, existingText)
      existingText.flatMap(_ => existingText.map(_.trim)).getOrElse(newText)
    }
  })
)

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
  geotrellisS3Spark % Provided,
  geotrellisSparkTestkit % Test,
  pdalScala,
  pdalNative,
  sparkCore % Provided,
  sparkSQL % Provided,
  hadoopClient % Provided,
  hadoopAWS % Test,
  scalatest % Test
)

/** https://github.com/lucidworks/spark-solr/issues/179 */
dependencyOverrides ++= {
  val deps = Seq(
    "com.fasterxml.jackson.core" % "jackson-core" % "2.6.7",
    "com.fasterxml.jackson.core" % "jackson-databind" % "2.6.7",
    "com.fasterxml.jackson.core" % "jackson-annotations" % "2.6.7"
  )
  CrossVersion.partialVersion(scalaVersion.value) match {
    // if Scala 2.12+ is used
    case Some((2, scalaMajor)) if scalaMajor >= 12 => deps
    case _ => deps :+ "com.fasterxml.jackson.module" %% "jackson-module-scala" % "2.6.7"
  }
}

Test / fork := true
Test / parallelExecution := false
Test / connectInput := true
Test / testOptions += Tests.Argument("-oDF")
