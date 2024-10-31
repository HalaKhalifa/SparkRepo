package Activity1

import org.apache.log4j.BasicConfigurator
import org.apache.log4j.varia.NullAppender
import org.apache.spark.rdd.RDD
import org.apache.spark.{SparkConf, SparkContext}
import breeze.util.BloomFilter

object BloomFilterApp {
  def main(args: Array[String]): Unit = {
    // Suppress logging output
    val nullAppender = new NullAppender
    BasicConfigurator.configure(nullAppender)

    // Set up Spark configuration and context
    val conf = new SparkConf().setAppName("BloomFilterApp").setMaster("local[*]")
    val sc = new SparkContext(conf)

    // Load all text files from the specified directory into an RDD
    val documents: RDD[String] = sc.textFile("src/main/resources/documents/archive/*.txt")
      .map(_.replaceAll("[^A-Za-z ]", "")) // Keep only alphabetic characters and spaces
      .filter(_.nonEmpty) // Remove any empty strings

    // Extract words from documents
    val words: RDD[String] = documents.flatMap(_.split("\\s+")).filter(_.nonEmpty)

    // Create and populate the Bloom Filter with specified parameters
    val uniqueWords = words.distinct()
    val count = uniqueWords.count()
    println(s"Total unique words to add to BloomFilter: $count")

    // Define parameters for the Bloom Filter
    val expectedNumItems = count.toInt
    val falsePositiveRate = 0.001 

    // Split words into training and testing datasets (70% train, 30% test)
    val splitRatio = Array(0.7, 0.3) 
    val Array(trainingWords, testingWords) = words.randomSplit(splitRatio)

    // Create the Bloom Filter using the training set
    val bloomFilter = trainingWords.distinct().mapPartitions { iter =>
      val bf = BloomFilter.optimallySized[String](expectedNumItems, falsePositiveRate)
      iter.foreach(word => bf += word)
      Iterator(bf)
    }.reduce(_ | _)

    // Prepare the testing set (distinct and filtered)
    val testWordsRDD = testingWords.distinct()

    // Broadcast unique words from the training set as a Set for efficient look-up
    val uniqueWordsSet = sc.broadcast(trainingWords.distinct().collect().toSet)

    // Count true positives, false positives, true negatives, and false negatives
    val testResults = testWordsRDD.map { word =>
      val inBloomFilter = bloomFilter.contains(word)
      val actualPresence = uniqueWordsSet.value.contains(word)

      if (inBloomFilter && actualPresence) {
        ("truePositives", 1)
      } else if (inBloomFilter && !actualPresence) {
        ("falsePositives", 1)
      } else if (!inBloomFilter && !actualPresence) {
        ("trueNegatives", 1)
      } else {
        ("falseNegatives", 1)
      }
    }

    // Aggregate counts for each category
    val counts = testResults.countByKey().mapValues(_.toInt)

    // Retrieve counts or set to 0 if missing
    val truePositives = counts.getOrElse("truePositives", 0)
    val falsePositives = counts.getOrElse("falsePositives", 0)
    val trueNegatives = counts.getOrElse("trueNegatives", 0)
    val falseNegatives = counts.getOrElse("falseNegatives", 0)

    // Calculate error rates
    val totalTestedWords = truePositives + falsePositives + trueNegatives + falseNegatives
    val empiricalFalsePositiveRate = if (totalTestedWords > 0) falsePositives.toDouble / totalTestedWords else 0.0
    val empiricalFalseNegativeRate = if (totalTestedWords > 0) falseNegatives.toDouble / totalTestedWords else 0.0

    println(s"True Positives: $truePositives")
    println(s"False Positives: $falsePositives")
    println(s"True Negatives: $trueNegatives")
    println(s"False Negatives: $falseNegatives")
    println(s"Empirical False Positive Rate: $empiricalFalsePositiveRate")
    println(s"Empirical False Negative Rate: $empiricalFalseNegativeRate")
    println(s"Total Tested Words: $totalTestedWords")

    sc.stop()
  }
}