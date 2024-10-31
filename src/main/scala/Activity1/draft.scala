package Activity1
// BloomFilterApp without spliting the dataset to train and test
import org.apache.log4j.BasicConfigurator
import org.apache.log4j.varia.NullAppender
import org.apache.spark.rdd.RDD
import org.apache.spark.{SparkConf, SparkContext}
import breeze.util.BloomFilter

object BloomFilterApp1 {
  def main(args: Array[String]): Unit = {
    // Suppress logging output
    val nullAppender = new NullAppender
    BasicConfigurator.configure(nullAppender)

    // Set up Spark configuration and context
    val conf = new SparkConf().setAppName("BloomFilterApp").setMaster("local[*]")
    val sc = new SparkContext(conf)

    // Load documents into an RDD
    val document: RDD[String] = sc.textFile("src/main/resources/wholeInvertedIndex.txt")
      .map(_.replaceAll("[^A-Za-z ]", "")) // Keep only alphabetic characters and spaces
      .filter(_.nonEmpty) // Remove any empty strings

    // Extract words from documents
    val words: RDD[String] = document.flatMap(_.split("\\s+")).filter(_.nonEmpty)

    // Create and populate the Bloom Filter with specified parameters
    val uniqueWords = words.distinct()
    val count = uniqueWords.count()
    println(s"Total unique words to add to BloomFilter: $count")

    // Define parameters for the Bloom Filter
    val expectedNumItems = count.toInt
    val falsePositiveRate = 0.005 

    // Create the Bloom Filter using mapPartitions
    val bloomFilter = uniqueWords.mapPartitions { iter =>
      val bf = BloomFilter.optimallySized[String](expectedNumItems, falsePositiveRate)
      iter.foreach(word => bf += word)
      Iterator(bf)
    }.reduce(_ | _)

    // Load test words from a file and clean it by removing non-alphabetic characters
    val testWordsRDD: RDD[String] = sc.textFile("src/main/resources/documents/archive/alt.atheism.txt")
      .map(_.replaceAll("[^A-Za-z]", "")) // Keep only alphabetic characters
      .filter(_.nonEmpty) // Remove any empty strings

    // Sample the same number of words as the count of unique words for testing
    val sampledTestWordsRDD = testWordsRDD.take(count.toInt)

    // Convert sampled words to an RDD
    val testWordsRDDLimited = sc.parallelize(sampledTestWordsRDD)

    // Broadcast unique words as a Set
    val uniqueWordsSet = sc.broadcast(uniqueWords.collect().toSet)

    // Count true positives, false positives, true negatives, and false negatives
    val testResults = testWordsRDDLimited.map { word =>
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