package src.main.scala.HW1

import org.apache.log4j.BasicConfigurator
import org.apache.log4j.varia.NullAppender

object SearchEngineApp {
  def main(args: Array[String]): Unit = {
    val nullAppender = new NullAppender
    BasicConfigurator.configure(nullAppender)

    val mongoUri = "mongodb://localhost:27017"
    val dbName = "search_engine_db"
    val collectionName = "dictionary"
    val documentsPath = "src/main/resources/documents/archive"
    val outputFilePath = "src/main/resources/wholeInvertedIndex.txt"

    val engine = new SearchEngine(documentsPath, mongoUri, dbName, collectionName)

    val cleanedRDD = engine.processDocuments()
    engine.buildInvertedIndex(outputFilePath, cleanedRDD)
    engine.saveInvertedIndexToMongoDB(outputFilePath)

    val userQuery = "play soccer"
    val results = engine.queryMongoDB(userQuery)
    println(s"Documents matching query '$userQuery': ${results.mkString(", ")}")

    engine.close()
  }
}