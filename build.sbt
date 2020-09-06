import com.typesafe.sbt.packager.docker._
import sbt.Keys.mappings

organization := "com.urdnot.iot"

name := "iotKafkaRainReader"
packageName := s"${organization}.rain"

version := "2.0.1"

val scalaMajorVersion = "2.13"
val scalaMinorVersion = "2"

scalaVersion := scalaMajorVersion.concat("." + scalaMinorVersion)


libraryDependencies ++= {
  val sprayJsonVersion = "1.3.5"
  val circeVersion = "0.12.3"
  val logbackClassicVersion = "1.2.3"
  val scalatestVersion = "3.1.1"
  val akkaVersion = "2.5.30"
  val AkkaHttpVersion = "10.2.0"
  val scalaLoggingVersion = "3.9.2"
  val akkaStreamKafkaVersion = "2.0.4"
  val testContainersVersion = "1.12.4"

  Seq(
    "com.typesafe.akka" %% "akka-actor" % akkaVersion,
    "com.typesafe.akka" %% "akka-stream-kafka" % akkaStreamKafkaVersion,
    "com.typesafe.akka" %% "akka-stream" % akkaVersion,
    "com.typesafe.akka" %% "akka-http" % AkkaHttpVersion,
    "com.lightbend.akka" %% "akka-stream-alpakka-influxdb" % "2.0.1",
    "io.spray" %% "spray-json" % sprayJsonVersion,
    "ch.qos.logback" % "logback-classic" % logbackClassicVersion,
    "com.typesafe.akka" %% "akka-testkit" % akkaVersion % Test,
    "io.circe" %% "circe-core" % circeVersion,
    "io.circe" %% "circe-generic" % circeVersion,
    "io.circe" %% "circe-parser" % circeVersion,
    "org.scalatest" %% "scalatest" % scalatestVersion % Test,
    "org.testcontainers" % "kafka" %  testContainersVersion % Test,
    "com.typesafe.scala-logging" %% "scala-logging" % scalaLoggingVersion
  )
}

enablePlugins(DockerPlugin)

mainClass in (Compile, assembly) := Some(s"${packageName}.DataReader")

assemblyJarName := s"${name.value}.v${version.value}.jar"
val meta = """META.INF(.)*""".r

mappings in(Compile, packageBin) ~= {
  _.filterNot {
    case (_, name) => Seq("application.conf").contains(name)
  }
}
assemblyMergeStrategy in assembly := {
  case n if n.endsWith(".properties") => MergeStrategy.concat
  case PathList("reference.conf") => MergeStrategy.concat
  case PathList("resources/application.conf") => MergeStrategy.discard
  case meta(_) => MergeStrategy.discard
  case x => MergeStrategy.first
}

dockerBuildOptions += "--no-cache"
dockerUpdateLatest := true
dockerPackageMappings in Docker += file(s"target/scala-2.13/${assemblyJarName.value}") -> s"opt/docker/${assemblyJarName.value}"
mappings in Docker += file("src/main/resources/application.conf") -> "opt/docker/application.conf"
mappings in Docker += file("src/main/resources/logback.xml") -> "opt/docker/logback.xml"
dockerExposedPorts := Seq(8081)

dockerCommands := Seq(
  Cmd("FROM", "openjdk:11-jdk-slim"),
  Cmd("LABEL", s"""MAINTAINER="Jeffrey Sewell""""),
  Cmd("COPY", s"opt/docker/${assemblyJarName.value}", s"/opt/docker/${assemblyJarName.value}"),
  Cmd("COPY", "opt/docker/application.conf", "/var/application.conf"),
  Cmd("COPY", "opt/docker/logback.xml", "/var/logback.xml"),
  Cmd("ENV", "CLASSPATH=/opt/docker/application.conf:/opt/docker/logback.xml"),
  Cmd("ENTRYPOINT", s"java -cp /opt/docker/${assemblyJarName.value} ${mainClass}")
)