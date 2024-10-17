error id: file://<WORKSPACE>/src/main/scala/Main.scala:[13..20) in Input.VirtualFile("file://<WORKSPACE>/src/main/scala/Main.scala", "package your.package.name  // Replace with your actual package name

import org.apache.spark.sql.SparkSession
import org.apache.spark.{SparkContext, SparkConf}
import org.apache.spark.rdd.RDD

object SearchEngineApp {
  def main(args: Array[String]): Unit = {
    // Initialize Spark context and session
    val conf = new SparkConf().setAppName("SearchEngineApp").setMaster("local[*]")
    val sc = new SparkContext(conf)
    val spark = SparkSession.builder().config(conf).getOrCreate()

    // Call the read and clean documents function
    val cleanedRDD = readAndCleanDocuments(sc)
    cleanedRDD.take(10).foreach(println) // Print the first 10 cleaned lines
  }

  // Function to read and clean documents
  def readAndCleanDocuments(sc: SparkContext): RDD[String] = {
    val rdd = sc.textFile("src/main/resources/documents/archive")
    // Clean text (removing non-alphanumeric characters but preserving apostrophes)
    rdd.map(line => 
      line.replaceAll("[^a-zA-Z\\s']", "").toLowerCase()
    ).filter(_.trim.nonEmpty)
  }
}
")
file://<WORKSPACE>/src/main/scala/Main.scala
file://<WORKSPACE>/src/main/scala/Main.scala:1: error: expected identifier; obtained package
package your.package.name  // Replace with your actual package name
             ^
#### Short summary: 

expected identifier; obtained package