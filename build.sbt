name := "Spark"

version := "0.1"

scalaVersion := "2.12.18"

libraryDependencies ++= Seq(
  "org.apache.spark" %% "spark-core" % "3.5.3",
  "org.apache.spark" %% "spark-sql" % "3.5.3",
  "org.apache.spark" %% "spark-mllib" % "3.5.3",
  "org.mongodb.scala" %% "mongo-scala-driver" % "4.5.0",
  "com.johnsnowlabs.nlp" %% "spark-nlp" % "5.0.0"
)

resolvers += "Spark Packages Repo" at "https://repos.spark-packages.org/"