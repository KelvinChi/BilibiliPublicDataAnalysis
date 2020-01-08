package com.ck.pageView

import java.io._
import java.nio.file.Paths
import java.sql.Timestamp
import java.util.Properties

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


object PageViewBatchProcess {

  var count = 0L

  def main(args: Array[String]) {
    val absPath = System.getProperty("user.dir") // 获取当前包绝对路径

    val env = StreamExecutionEnvironment.getExecutionEnvironment
    env.setStreamTimeCharacteristic(TimeCharacteristic.EventTime)


    val source = Paths.get(absPath, "PythonWorm/output/jsonLog.csv").toString
    val stream = env.readTextFile(source)
      .map(line => {
        val lineArray = line.split(",")
        JsonLog(lineArray(0).toInt, lineArray(1).toInt, lineArray(2).toInt, lineArray(3).toInt,
          lineArray(4).toInt, lineArray(5).toInt, lineArray(6).toInt, lineArray(7).toLong)
      })
      .assignAscendingTimestamps(_.timestamp)
      .keyBy(_.aid)
      .timeWindow(Time.hours(1), Time.minutes(30)) // 前者表示统计多大时间范围数据，后者表示多少频率统计一次
      .aggregate(new CountAgg(), new WindowResult())
      .keyBy(_.windowEnd)
      // 传入参数为统计top多少；注意因为前N会动态变化，所以通常的图表无法处理（也许是我水平差）
      .process(new CombineProcess(3))

    val outPut = stream.print().setParallelism(1)

    env.execute("PV Per Vedio")
    printf("Job done, append totally %d lines", count / 3) // 最后数据三列放一行，便于后续统计
  }

  class CombineProcess(topN: Int) extends KeyedProcessFunction[Long, PVResult, String] {

    val absPath = System.getProperty("user.dir") // 获取当前包绝对路径

    lazy val PVResultState: ListState[PVResult] = getRuntimeContext.getListState(
      new ListStateDescriptor[PVResult]("PVResultState", classOf[PVResult]))

    // 将小数转为百分比格式输出
    def ratioTransform(ratio: Double): String = {
      (ratio * 100).formatted("%.2f") + "%"
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
        count += 1
        viewTimesList += viewTimes(pvr.aid, pvr.windowEnd, pvr.icsList.max)
        viewIncrsList += viewTimes(pvr.aid, pvr.windowEnd, pvr.icsList.max - pvr.icsList.min)
        viewIncrsRatioList += incrsRatio(
          pvr.aid, pvr.windowEnd, (pvr.icsList.max - pvr.icsList.min).toDouble / pvr.icsList.max.toDouble)
      }
      PVResultState.clear()

      // 总观看量前N
      val sortedVTList = viewTimesList.sortWith(_.view > _.view).take(topN)
      // 观看增长率前N
      val sortedVIList = viewIncrsList.sortWith(_.view > _.view).take(topN)
      // 观看增长率前N
      val sortedVRList = viewIncrsRatioList.sortWith(_.ratio > _.ratio).take(topN)
      // 创建文件

      // 构建输出文本
      val result = new StringBuilder
      result.append("====================================\n")
        .append(new Timestamp(timestamp - 100))
        .append("\n").append("总观看量排名：\n")
      for (i <- sortedVTList.indices) {
        val curItem = sortedVTList(i)
        result.append("No.").append(i + 1).append(": 编号 ")
          .append(curItem.aid).append(" 总观看数: ")
          .append(curItem.view).append("\n")
      }

      result.append("观看增长量排名：\n")
      for (j <- sortedVIList.indices) {
        val curItem = sortedVIList(j)
        result.append("No.").append(j + 1).append(": 编号 ")
          .append(curItem.aid).append(" 净增数: ")
          .append(curItem.view).append("\n")
      }

      result.append("观看增长率排名：\n")
      for (j <- sortedVRList.indices) {
        val curItem = sortedVRList(j)
        result.append("No.").append(j + 1).append(": 编号 ")
          .append(curItem.aid).append(" 增率: ")
          .append(ratioTransform(curItem.ratio)).append("\n")
      }

      Thread.sleep(2000)
      out.collect(result.toString())
    }
  }

}
