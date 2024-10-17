package src.main.scala.HW1

import org.apache.spark.rdd.RDD
import org.apache.spark.SparkContext
import org.apache.spark.sql.SparkSession
import org.mongodb.scala._
import scala.concurrent.Await
import scala.concurrent.duration.Duration

class SearchEngine(documentsPath: String, mongoUri: String, dbName: String, collectionName: String) {

  val sc = SparkContextProvider.getContext()
  val spark = SparkSessionProvider.getSession()

  // MongoDB client and collection
  val mongoClient: MongoClient = MongoDBHandler.getMongoClient(mongoUri)
  val collection: MongoCollection[Document] = MongoDBHandler.getMongoCollection(mongoClient, dbName, collectionName)

  def processDocuments(): RDD[(String, String)] = DocumentProcessor.readAndCleanDocuments(sc, documentsPath)

  def buildInvertedIndex(outputFilePath: String, cleanedRDD: RDD[(String, String)]): RDD[(String, (Int, List[String]))] =
    InvertedIndex.build(outputFilePath, cleanedRDD)

  def saveInvertedIndexToMongoDB(filePath: String, batchSize: Int = 3000): Unit = 
    MongoDBHandler.saveInvertedIndex(collection, filePath, batchSize)

  def queryMongoDB(query: String): List[String] = MongoDBHandler.query(collection, query)

  def close(): Unit = {
    SparkContextProvider.close(sc)
    mongoClient.close()
  }
}