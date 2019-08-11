name := "eventSourcing"

version := "0.2"

scalaVersion := "2.11.8"

libraryDependencies ++= Seq(
  "org.scalaz"                   %% "scalaz-core"                   % "7.2.4",
  "org.scalaz"                   %% "scalaz-concurrent"             % "7.2.4",
  "joda-time"                     % "joda-time"                     % "2.9.1",
  "org.joda"                      % "joda-convert"                  % "1.8.1",
  "io.spray"                     %% "spray-json"                    % "1.3.2",
  "com.typesafe.akka"            %% "akka-actor"                    % "2.4.8",
  "com.typesafe.akka"            %% "akka-persistence"              % "2.4.8",
  "com.typesafe.akka"            %% "akka-stream"                   % "2.4.8",
  "com.typesafe.scala-logging"   %% "scala-logging-slf4j"           % "2.1.2",
  "ch.qos.logback"                % "logback-classic"               % "1.1.3"
)