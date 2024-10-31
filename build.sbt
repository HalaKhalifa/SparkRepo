name := "SparkSearchEngine"

version := "0.1"

scalaVersion := "2.12.18"

libraryDependencies += "org.apache.spark" %% "spark-core" % "3.5.3"
libraryDependencies += "org.apache.spark" %% "spark-sql" % "3.5.3"
libraryDependencies += "org.scalanlp" %% "breeze" % "2.1.0"

libraryDependencies += "org.mongodb.scala" %% "mongo-scala-driver" % "4.5.0"

resolvers += "Spark Packages Repo" at "https://repos.spark-packages.org/"