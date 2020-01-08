package com.ck.pageView

import java.io._
import java.nio.file.Paths
import java.sql.Timestamp
import java.util.{Date, Properties}

import org.apache.flink.api.common.functions.AggregateFunction
import org.apache.flink.api.common.serialization.SimpleStringSchema
import org.apache.flink.api.common.state.{ListState, ListStateDescriptor}
import org.apache.flink.streaming.api.TimeCharacteristic
import org.apache.flink.streaming.api.functions.KeyedProcessFunction
import org.apache.flink.streaming.api.scala._
import org.apache.flink.streaming.api.scala.function.WindowFunction
import org.apache.flink.streaming.api.windowing.time.Time
import org.apache.flink.streaming.api.windowing.windows.TimeWindow
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaConsumer
import org.apache.flink.util.Collector

import scala.collection.mutable.ListBuffer


// 建立用于解析数据格式的样例类
case class JsonLog(aid: Int, like: Int, dislike: Int, view: Int, share: Int, coin: Int, bsNum: Int, timestamp: Long)

// 窗口聚合后的输出样例类
case class PVResult(aid: Int, windowStart: Long, windowEnd: Long, icsList: ListBuffer[Long])

// 观看数量样例类
case class viewTimes(aid: Int, windowEnd: Long, view: Long)

// 增长率样例类
case class incrsRatio(aid: Int, windowEnd: Long, ratio: Double)

object PageView {

  def main(args: Array[String]) {
    val absPath = System.getProperty("user.dir") // 获取当前包绝对路径

    // 先定义好想对比的三个视频
    val compareRange = List[Int](81394266, 81463167, 81417721)

    val env = StreamExecutionEnvironment.getExecutionEnvironment
    env.setStreamTimeCharacteristic(TimeCharacteristic.EventTime)
    //    env.setStreamTimeCharacteristic(TimeCharacteristic.ProcessingTime)

    val properties = new Properties()
    properties.setProperty("bootstrap.servers", "192.168.3.60:9092")
    properties.setProperty("group.id", "flume-consumer")
    properties.setProperty("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer")
    properties.setProperty("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer")
    properties.setProperty("auto.offset.reset", "latest")

    //    val source = Paths.get(absPath, "PythonWorm/output/jsonLog.csv").toString
    val stream = env.addSource(new FlinkKafkaConsumer[String]("topic-PV", new SimpleStringSchema(), properties))
      //    val stream = env.readTextFile(source)
      .map(line => {
      val lineArray = line.split(",")
      JsonLog(lineArray(0).toInt, lineArray(1).toInt, lineArray(2).toInt, lineArray(3).toInt,
        lineArray(4).toInt, lineArray(5).toInt, lineArray(6).toInt, lineArray(7).toLong)
    })
      .filter(line => {
        compareRange.contains(line.aid)
      })
      .assignAscendingTimestamps(_.timestamp)
      .keyBy(_.aid)
      .timeWindow(Time.minutes(5), Time.minutes(2)) // 前者表示统计多大时间范围数据，后者表示多少频率统计一次
      .aggregate(new CountAgg(), new WindowResult())
      .keyBy(_.windowEnd)
      .process(new CombineProcess()).setParallelism(1) // 注意如果不把最后的并行度设为一会造成结果数据乱序

    env.execute("PV Per Vedio")
  }

  class CombineProcess() extends KeyedProcessFunction[Long, PVResult, String] {

    val absPath = System.getProperty("user.dir") // 获取当前包绝对路径

    lazy val PVResultState: ListState[PVResult] = getRuntimeContext.getListState(
      new ListStateDescriptor[PVResult]("PVResultState", classOf[PVResult]))

    // 将小数转为百分比格式输出
    def ratioTransform(ratio: Double): String = {
      (ratio * 100).formatted("%.2f") + "%"
    }

    // 持久化
    def writeToFile(path: String, con: String): Unit = {
      val file = new File(path)
      if (!file.exists()) {
        file.createNewFile()
      }
      val writer = new FileWriter(path, true) // true表示以append方式追加
      val temp = con.dropRight(1) // 将字符最后一个逗号去掉
      writer.write(temp + "\n")
      writer.close()
    }

    override def processElement(value: PVResult, ctx: KeyedProcessFunction[Long, PVResult, String]#Context,
                                out: Collector[String]): Unit = {
      PVResultState.add(value)
      ctx.timerService().registerEventTimeTimer(value.windowEnd + 100)
    }

    override def onTimer(timestamp: Long, ctx: KeyedProcessFunction[Long, PVResult, String]#OnTimerContext,
                         out: Collector[String]): Unit = {
      // 全部数据
      val finalList = new collection.mutable.ListBuffer[PVResult]()
      // 视频最大净观看量列表
      val viewTimesList = collection.mutable.ListBuffer[viewTimes]()
      // 视频观看增长量列表
      val viewIncrsList = collection.mutable.ListBuffer[viewTimes]()
      // 视频增长率列表
      val viewIncrsRatioList = collection.mutable.ListBuffer[incrsRatio]()
      import scala.collection.JavaConversions._
      for (pvr <- PVResultState.get()) {
        viewTimesList += viewTimes(pvr.aid, pvr.windowEnd, pvr.icsList.max)
        viewIncrsList += viewTimes(pvr.aid, pvr.windowEnd, pvr.icsList.max - pvr.icsList.min)
        viewIncrsRatioList += incrsRatio(
          pvr.aid, pvr.windowEnd, (pvr.icsList.max - pvr.icsList.min).toDouble / pvr.icsList.max.toDouble)
      }
      PVResultState.clear()

      // 总观看量
      val sortedVTList = viewTimesList.sortWith(_.aid > _.aid)
      val pathVT = Paths.get(absPath, "FlinkAnalysis/out/viewTimes.csv").toString
      var lineVT = ""
      // 观看净增数
      val sortedVIList = viewIncrsList.sortWith(_.aid > _.aid)
      val pathVI = Paths.get(absPath, "FlinkAnalysis/out/viewIncreasement.csv").toString
      var lineVI = ""
      // 观看增长率
      val sortedVRList = viewIncrsRatioList.sortWith(_.aid > _.aid)
      val pathVR = Paths.get(absPath, "FlinkAnalysis/out/viewIncrsRatio.csv").toString
      var lineVR = ""
      for (i <- sortedVTList.indices) {
        val curItem = sortedVTList(i)
        lineVT += curItem.aid + "," + curItem.view + "," + curItem.windowEnd + ","
      }
      writeToFile(pathVT, lineVT)

      for (j <- sortedVIList.indices) {
        val curItem = sortedVIList(j)
        lineVI += curItem.aid + "," + curItem.view + "," + curItem.windowEnd + ","
      }
      writeToFile(pathVI, lineVI)

      for (j <- sortedVRList.indices) {
        val curItem = sortedVRList(j)
        lineVR += curItem.aid + "," + (curItem.ratio * 100).formatted("%.2f") + "," + curItem.windowEnd + ","
      }
      writeToFile(pathVR, lineVR)
      println(new Date().toString)
      println("总观看次数：" + lineVT)
      println("观看次数增长量：" + lineVI)
      println("增长率变化：" + lineVR)
    }
  }

}

// 预聚合，将view数据加入列表中
class CountAgg() extends AggregateFunction[JsonLog, ListBuffer[Long], ListBuffer[Long]] {
  override def createAccumulator(): ListBuffer[Long] = collection.mutable.ListBuffer[Long]()

  override def merge(acc: ListBuffer[Long], acc1: ListBuffer[Long]): ListBuffer[Long] = acc ++ acc1

  override def getResult(acc: ListBuffer[Long]): ListBuffer[Long] = acc

  override def add(in: JsonLog, acc: ListBuffer[Long]): ListBuffer[Long] = acc += in.view
}

class WindowResult() extends WindowFunction[ListBuffer[Long], PVResult, Int, TimeWindow] {
  override def apply(key: Int, window: TimeWindow, input: Iterable[ListBuffer[Long]],
                     out: Collector[PVResult]): Unit = {
    out.collect(PVResult(key, window.getStart, window.getEnd, input.iterator.next))
  }
}

