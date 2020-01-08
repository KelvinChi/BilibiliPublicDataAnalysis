package com.ck.bulletScreen


import java.nio.file.Paths
import java.util.Properties

import org.apache.flink.api.common.serialization.SimpleStringSchema
import org.apache.flink.streaming.api.TimeCharacteristic
import org.apache.flink.streaming.api.scala._
import org.apache.flink.streaming.api.scala.function.ProcessAllWindowFunction
import org.apache.flink.streaming.api.windowing.time.Time
import org.apache.flink.streaming.api.windowing.windows.TimeWindow
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaConsumer
import org.apache.flink.util.Collector
import redis.clients.jedis.Jedis

case class BSCon(aid: Int, bsTime: Double, content: String, timestamp: Long)

object BSPerVedioDistribution {

  val source = System.getProperty("user.dir")
  val aid = 81793657
  val bsPerVedioFile = Paths.get(source, "FlinkAnalysis/out/BSPerVedio.csv").toString
  var count = 0
  lazy val jedis = new Jedis("localhost", 6379)

  def main(args: Array[String]) {

    val properties = new Properties()
    properties.setProperty("bootstrap.servers", "192.168.3.60:9092")
    properties.setProperty("group.id", "flume-consumer")
    properties.setProperty("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer")
    properties.setProperty("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer")
    properties.setProperty("auto.offset.reset", "latest")

    val env = StreamExecutionEnvironment.getExecutionEnvironment
    // 如果使用EventTime则弹幕数据低达时并不会触发写入，需要等下一条且间隔时间超过设定窗口时间才会触发
    // 针对类似实时性要求不是非常高的应用，还是使用ProcessingTime
    // 对于本地数据处理，还是需要用EventTime实现数据的时序
    //    env.setStreamTimeCharacteristic(TimeCharacteristic.EventTime)
    env.setStreamTimeCharacteristic(TimeCharacteristic.ProcessingTime)

    val stream = env.addSource(new FlinkKafkaConsumer[String]("topic-BSPVD", new SimpleStringSchema(), properties))
      //    val filePath = Paths.get(source, "PythonWorm/output/xmlLog.csv").toString
      //    val stream = env.readTextFile(filePath)
      .map(line => {
      val splitList = line.split(",")
      val aid = splitList.head
      val vedioTimeRatio = splitList(1)
      val timestamp = splitList.last
      val conTemp = splitList.drop(1).drop(1).dropRight(1)
      var content = ""
      for (i <- conTemp) {
        content += i + ","
      }
      BSCon(aid.toInt, vedioTimeRatio.toDouble, content.dropRight(1), timestamp.toLong)
    })
      .filter(_.aid == aid)

    val aggStream = stream
      //      .assignAscendingTimestamps(_.timestamp * 1000) // 踩坑，时间戳的指定位置在类似的分键数据中要在filter之后
      .timeWindowAll(Time.seconds(1)) // 实时数据时每秒刷新一次
      .process(new BSData()).setParallelism(1)


    env.execute("BSPerVedio Job")
    printf("Job done, append totally %d lines", count)
  }


  // 这里注意数据位置，echarts散点图默认了第一个值为x轴第二为y轴
  class BSData() extends ProcessAllWindowFunction[BSCon, String, TimeWindow] {

    override def process(context: Context, elements: Iterable[BSCon], out: Collector[String]): Unit = {
      val info = jedis.hget("bilibiliWorm", aid.toString).split(",")
      val vedioTime = info.last.toDouble
      val iter = elements.toIterator
      while (iter.hasNext) {
        count += 1
        val item = iter.next()
        val finalString = (item.bsTime / vedioTime).formatted("%.2f") +
          "," + item.timestamp + "," + item.aid + "," + item.content.length + "\n"
        println(finalString)
        BSPerDayDistribution.writeToFile(bsPerVedioFile, finalString)
      }
    }
  }

}