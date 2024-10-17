package src.main.scala.HW1

import org.apache.spark.sql.SparkSession

object SparkSessionProvider {
  def getSession(): SparkSession = {
    SparkSession.builder()
      .appName("SearchEngine")
      .master("local[*]")
      .getOrCreate()
  }
}