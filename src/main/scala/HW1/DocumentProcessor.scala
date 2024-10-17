package src.main.scala.HW1

import org.apache.spark.SparkContext
import org.apache.spark.rdd.RDD
import java.io.File

object DocumentProcessor {

  def readAndCleanDocuments(sc: SparkContext, documentsPath: String): RDD[(String, String)] = {
    val rdd = sc.wholeTextFiles(documentsPath)
    rdd.map { case (docPath, content) =>
      val docName = new File(docPath).getName
      val cleanedContent = content
        .replaceAll("[^a-zA-Z\\s]", "")
        .replaceAll("""'+""", "")
        .toLowerCase
        .replaceAll("\\s+", " ")
        .trim

      (docName, cleanedContent)
    }
  }
}