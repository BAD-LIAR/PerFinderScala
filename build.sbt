ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "2.13.8"

val CatsVersion = "2.6.1"
val CirceVersion = "0.14.1"
val CirceGenericExVersion = "0.14.1"
val CirceConfigVersion = "0.8.0"
val PureConfigVersion = "0.17.0"
val DoobieVersion = "0.13.4"
val EnumeratumCirceVersion = "1.7.0"
val Http4sVersion = "0.21.28"
val KindProjectorVersion = "0.13.2"
val LogbackVersion = "1.2.6"
val Slf4jVersion = "1.7.30"
val ScalaCheckVersion = "1.15.4"
val ScalaTestVersion = "3.2.9"
val ScalaTestPlusVersion = "3.2.2.0"
val FlywayVersion = "7.15.0"
val PostgresVersion = "42.3.6"
val TsecVersion = "0.2.1"

lazy val root = (project in file("."))
  .settings(
    name := "PerFinderScala",
    version := "0.0.1-SNAPSHOT",
    scalaVersion := "2.13.8",
    libraryDependencies ++= Seq(
      "org.typelevel" %% "cats-core" % CatsVersion,
      "io.circe" %% "circe-generic" % CirceVersion,
      "io.circe" %% "circe-literal" % CirceVersion,
      "io.circe" %% "circe-generic-extras" % CirceGenericExVersion,
      "io.circe" %% "circe-parser" % CirceVersion,
      "io.circe" %% "circe-config" % CirceConfigVersion,
      "org.tpolecat" %% "doobie-core" % DoobieVersion,
      "org.tpolecat" %% "doobie-postgres" % DoobieVersion,
      "org.tpolecat" %% "doobie-scalatest" % DoobieVersion,
      "org.tpolecat" %% "doobie-hikari" % DoobieVersion,
      "com.beachape" %% "enumeratum-circe" % EnumeratumCirceVersion,
      "org.http4s" %% "http4s-blaze-server" % Http4sVersion,
      "org.http4s" %% "http4s-circe" % Http4sVersion,
      "org.http4s" %% "http4s-dsl" % Http4sVersion,
      "ch.qos.logback" % "logback-classic" % LogbackVersion,
      "org.flywaydb" % "flyway-core" % FlywayVersion,
      "org.postgresql" % "postgresql" % PostgresVersion,
      "org.http4s" %% "http4s-blaze-client" % Http4sVersion % Test,
      "org.scalacheck" %% "scalacheck" % ScalaCheckVersion % Test,
      "org.scalatest" %% "scalatest" % ScalaTestVersion % Test,
      "org.scalatestplus" %% "scalacheck-1-14" % ScalaTestPlusVersion % Test,
      // Authentication dependencies
      "io.github.jmcardon" %% "tsec-common" % TsecVersion,
      "io.github.jmcardon" %% "tsec-password" % TsecVersion,
      "io.github.jmcardon" %% "tsec-mac" % TsecVersion,
      "io.github.jmcardon" %% "tsec-signatures" % TsecVersion,
      "io.github.jmcardon" %% "tsec-jwt-mac" % TsecVersion,
      "io.github.jmcardon" %% "tsec-jwt-sig" % TsecVersion,
      "io.github.jmcardon" %% "tsec-http4s" % TsecVersion,
      "com.github.pureconfig" %% "pureconfig" % PureConfigVersion,
      "org.mindrot"  % "jbcrypt"   % "0.3m"
    )
  )

dependencyOverrides += "org.slf4j" % "slf4j-api" % Slf4jVersion

resolvers += Resolver.sonatypeRepo("releases")

addCompilerPlugin("org.typelevel" % "kind-projector" % KindProjectorVersion cross CrossVersion.full)

