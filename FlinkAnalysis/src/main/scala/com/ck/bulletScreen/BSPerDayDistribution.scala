package com.ck.bulletScreen

import java.io.{FileWriter, File}
import java.nio.file.Paths
import java.sql.Timestamp
import java.text.SimpleDateFormat
import java.util.Calendar

import org.apache.flink.api.common.functions.MapFunction
import org.apache.flink.streaming.api.TimeCharacteristic
import org.apache.flink.streaming.api.scala._

import scala.collection.mutable.ListBuffer

/*
统计一天每个小时内的弹幕数量，所以需要把时间戳转为同一天
 */

object BSPerDayDistribution {

  var count = 0L
  var flag = true

  val source = System.getProperty("user.dir")
  val BSWorkdayFile = Paths.get(source, "FlinkAnalysis/out/BSWorkday.csv").toString
  val BSWeekendFile = Paths.get(source, "FlinkAnalysis/out/BSWeekend.csv").toString

  val bsMapWorkday = collection.mutable.Map[String, Long]()
  val bsMapWeekend = collection.mutable.Map[String, Long]()

  def main(args: Array[String]) {
    // 先删除原有文件
    val fileWorkday = new File(BSWorkdayFile)
    if (fileWorkday.exists()) fileWorkday.delete()

    val fileHoliday = new File(BSWeekendFile)
    if (fileHoliday.exists()) fileHoliday.delete()

    val env = StreamExecutionEnvironment.getExecutionEnvironment
    env.setStreamTimeCharacteristic(TimeCharacteristic.EventTime)

    val filePath = Paths.get(source, "PythonWorm/output/xmlLog.csv").toString
    val stream = env.readTextFile(filePath)
      .map(new BSMap())
    env.execute("BSPerDay Job")

    orderAndWrite(BSWorkdayFile, bsMapWorkday)
    println("工作日数据写入完成")
    orderAndWrite(BSWeekendFile, bsMapWeekend)
    println("节假日数据写入完成")
  }

  // 排序输出
  def orderAndWrite(path: String, map: collection.mutable.Map[String, Long]): Unit = {
    val bsList = ListBuffer[String]()
    for ((k, v) <- map) {
      bsList += (k + "," + v + "\n")
    }
    val sortedBSWDList = bsList.sortWith(_ < _)
    for (bsLine <- sortedBSWDList) {
      writeToFile(path, bsLine)
    }
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


  class BSMap() extends MapFunction[String, String] {
    override def map(t: String): String = {
      // 注意弹幕内容中会出现逗号
      val splitList = t.split(",")
      val aid = splitList.head
      val vedioTime = splitList(1)
      val timestamp = splitList.last
      val getHour = new SimpleDateFormat("HH").format(new Timestamp(timestamp.toLong * 1000)) + ":00"
      // 区别对待工作日与周末
      val flag = judgeDay(timestamp.toLong * 1000)
      if (!flag) {
        if (bsMapWorkday.contains(getHour)) {
          bsMapWorkday(getHour) += 1
        } else {
          bsMapWorkday(getHour) = 1
        }
      } else {
        if (bsMapWeekend.contains(getHour)) {
          bsMapWeekend(getHour) += 1
        } else {
          bsMapWeekend(getHour) = 1
        }
      }
      "Done"
    }
  }

  // 判断时间戳对应的是工作日还是周末
  def judgeDay(timestamp: Long): Boolean = {
    val formattedTime = new SimpleDateFormat("yyyy/MM/dd").format(new Timestamp(timestamp.toLong))
    IsWeekend.isWeekend(formattedTime)
  }
}