package src.main.scala.HW1

import org.apache.spark.{SparkConf, SparkContext}

object SparkContextProvider {
  def getContext(): SparkContext = {
    val conf = new SparkConf().setAppName("SearchEngine").setMaster("local[*]")
    new SparkContext(conf)
  }

  def close(sc: SparkContext): Unit = {
    if (!sc.isStopped) {
      sc.stop()
    }
  }
}