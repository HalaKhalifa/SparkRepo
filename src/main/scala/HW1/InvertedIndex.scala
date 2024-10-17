package src.main.scala.HW1

import org.apache.spark.rdd.RDD
import java.io.{File, PrintWriter}

object InvertedIndex {

  def build(outputFilePath: String, cleanedRDD: RDD[(String, String)]): RDD[(String, (Int, List[String]))] = {
    val wordsWithDoc = cleanedRDD.flatMap { case (docName, cleanedContent) =>
      cleanedContent.split(" ").filter(_.nonEmpty).map(word => (word, docName))
    }

    val groupedWords = wordsWithDoc.distinct().groupByKey()

    val invertedIndex = groupedWords.mapValues { docNames =>
      val uniqueSortedDocs = docNames.toList.distinct.sorted
      (uniqueSortedDocs.size, uniqueSortedDocs)
    }

    val writer = new PrintWriter(new File(outputFilePath))
    try {
      invertedIndex.sortByKey().collect().foreach { case (word, (count, docList)) =>
        writer.write(s"$word, $count, ${docList.mkString(", ")}\n")
      }
    } finally {
      writer.close()
    }

    invertedIndex
  }
}