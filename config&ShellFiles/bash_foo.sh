#!/usr/bin/env bash

# 集群统一执行命令
function runAll() {
	for i in ck dc tp
	do
		echo ------------------- $i -------------------
		ssh youname@$i "source /etc/profile; $*"
	done
}

# ZK运行
function runZK() {
	case $1 in
	"start"){
		for i in ck dc tp
		do
			echo $i:
			ssh $i "source /etc/profile; /opt/module/zookeeper-3.4.10/bin/zkServer.sh start"
		done
	};;
	"stop"){
		for i in ck dc tp
		do
			echo $i:
			ssh $i "source /etc/profile; /opt/module/zookeeper-3.4.10/bin/zkServer.sh stop"
		done
	};;
	"status"){
		for i in ck dc tp
		do
			echo $i:
			ssh $i "source /etc/profile; /opt/module/zookeeper-3.4.10/bin/zkServer.sh status"
		done
	};;
	esac
}

# 启动Flume读取Xml
function runFlumeXml() {
case $1 in
	"start"){
		local fileName=`date +%Y-%m-%d`.log
		runAll mkdir -p /opt/module/flume/logs
		for i in tp
		do
			echo " --------启动 $i BSPVDFlume-------"
			# 注意路径需要修改成自己的
			ssh $i "source /etc/profile; nohup /opt/module/flume/bin/flume-ng agent --conf /opt/module/flume/conf/ --conf-file ~/temp/file-flume-kafka-BSPVD.conf --name a1 -Dflume.root.logger=INFO,LOGFILE >> /opt/module/flume/logs/$fileName 2>&1 &"
		done
	};;	
	"stop"){
		for i in tp
		do
			echo " --------停止 $i BSPVDFlume-------"
			ssh $i "ps -ef | grep file-flume-kafka-BSPVD | grep -v grep |awk '{print \$2}' |xargs kill"
		done

	};;
	esac
}

# 启动Flume读取Json
function runFlumeJson() {
case $1 in
	"start"){
		local fileName=`date +%Y-%m-%d`.log
		runAll mkdir -p /opt/module/flume/logs
		for i in tp
		do
			echo " --------启动 $i PVFlume-------"
			# 注意路径需要修改成自己的
			ssh $i "source /etc/profile; nohup /opt/module/flume/bin/flume-ng agent --conf /opt/module/flume/conf/ --conf-file ~/temp/file-flume-kafka-PV.conf --name a2 -Dflume.root.logger=INFO,LOGFILE >> /opt/module/flume/logs/$fileName 2>&1 &"
		done
	};;
	"stop"){
		for i in tp
		do
			echo " --------停止 $i PVFlume-------"
			ssh $i "ps -ef | grep file-flume-kafka-PV | grep -v grep |awk '{print \$2}' |xargs kill"
		done

	};;
	esac
}


# 启动Kafka
function runKafka() {
	case $1 in
	"start"){
        for i in ck dc tp
        do
			echo " --------启动 $i Kafka-------"
			# 用于KafkaManager监控
			ssh $i "source /etc/profile; export JMX_PORT=9988 && /opt/module/kafka/bin/kafka-server-start.sh -daemon /opt/module/kafka/config/server.properties "
		done
	};;
	"stop"){
        for i in ck dc tp
        do
			echo " --------停止 $i Kafka-------"
			# ssh $i "source /etc/profile; /opt/module/kafka/bin/kafka-server-stop.sh stop >/dev/null 2>&1"
			ssh youname@$i "source /etc/profile; jps |grep Kafka| awk '{print \$1}' |xargs kill -9"
		done
	};;
	esac
}

# 启动停止kafka-manager
function runKafkaManager() {
	case $1 in
	"start"){
        echo " -------- 启动 KM -------"
        nohup /opt/module/kafka-manager/bin/kafka-manager   -Dhttp.port=7456 > /opt/module/kafka-manager/start.log 2>&1 &
	};;
	"stop"){
        echo " -------- 停止 KM -------"
		# jps| grep ProdServerStart| awk '{print $1}'|xargs kill &
		ps -ef| grep kafka-manager| grep -v grep|awk '{print $2}'| xargs kill >/dev/null 2>&1
	};;
	esac
}

# 启动集群
function runCluster() {
	case $1 in
		"start" ) {
			echo "---------- 启动 集群 ----------"
			echo "--------- 启动 HADOOP ---------"
			ssh yourname@ck "source /etc/profile; /opt/module/hadoop-2.7.2/sbin/start-dfs.sh"
			ssh yourname@dc "source /etc/profile; /opt/module/hadoop-2.7.2/sbin/start-yarn.sh"

			echo "----------- 启动 ZK -----------"
			runZK start
			sleep 3s;
			# 启动Flume采集集群
			runFlume start
			runKafka start
			sleep 3s;
			# 启动Flume消费集群
			runFlumeConsumer start
			# 启动KafkaManager
			# runKafkaManager start
			echo "---------- 启动 完成 ----------"
		};;
		"stop" ) {
			echo "---------- 关闭 集群 ----------"
			# runKafkaManager stop
			# 关闭Flume消费集群
			runFlumeConsumer stop
			runKafka stop
			sleep 3s;
			# 关闭Flume生产集群
			runFlume stop
			runZK stop
			echo "--------- 关闭 HADOOP ---------"
			ssh youname@dc "source /etc/profile; /opt/module/hadoop-2.7.2/sbin/stop-yarn.sh"
			ssh youname@ck "source /etc/profile; /opt/module/hadoop-2.7.2/sbin/stop-dfs.sh"
			echo "---------- 完成 关闭 ----------"
		};;
	esac
}

# 备份配置到Mac
function backupBashToMac() {
	fileName='bash'
	# 压缩本地文件
	backupCluster backup
	cd ~ && tar -zcf $fileName.tar.gz .bash* .vimrc script/ backup/ gmall/ project/
	# 发送文件到Mac
	scp ~/$fileName.tar.gz xxxx@192.168.3.42:/Users/kelvinchi/Desktop/ 1> /dev/null
	# 删除本地文件
	rm -rf ~/$fileName.tar.gz
	# 查看远程备份信息
	# ssh kelvinchi@192.168.3.42 "ls -lh /Users/kelvinchi/Desktop/*tar.gz"
}

# 删除所有集群资料
function rmAll() {

	for i in ck dc tp
	do
		ssh youname@$i "rm -rf ~/.hadoop /opt/module/hadoop-2.7.2/logs /opt/module/kafka/logs /opt/module/zookeeper-3.4.10/logs /opt/module/zookeeper-3.4.10/data/version-2 /opt/module/flume/logs"
		echo "---------- $i 删除完成 ---------"
	done
	ssh youname@tp "rm -rf /opt/module/flume/checkpoint/ /opt/module/flume/data/"
}

# 备份集群
function backupCluster() {

	case $1 in
	"backup" ) {
# hadoop
		rm -rf ~/backup/hadoop-2.7.2/etc/hadoop
		mkdir -p ~/backup/hadoop-2.7.2/etc/hadoop
		cp -r /opt/module/hadoop-2.7.2/etc/hadoop/* ~/backup/hadoop-2.7.2/etc/hadoop/
# hive
		rm -rf ~/backup/hive/conf
		mkdir -p ~/backup/hive/conf
		cp -r /opt/module/hive/conf/* ~/backup/hive/conf/
# ZK
		rm -rf ~/backup/zookeeper-3.4.10/conf
		mkdir -p ~/backup/zookeeper-3.4.10/conf
		cp -r /opt/module/zookeeper-3.4.10/conf/* ~/backup/zookeeper-3.4.10/conf/
# kafka
		rm -rf ~/backup/kafka/config
		mkdir -p ~/backup/kafka/config
		cp -r /opt/module/kafka/config/* ~/backup/kafka/config/
# flume
		rm -rf ~/backup/flume/conf
		mkdir -p ~/backup/flume/conf
		cp -r /opt/module/flume/conf/* ~/backup/flume/conf/
# sqoop
		rm -rf ~/backup/sqoop/conf
		mkdir -p ~/backup/sqoop/conf
		cp -r /opt/module/sqoop/conf/* ~/backup/sqoop/conf/
# spark
		rm -rf ~/backup/spark/conf
		mkdir -p ~/backup/spark/conf
		cp -r /opt/module/spark/conf/* ~/backup/spark/conf/
# config
		cp /etc/profile ~/backup/
		cp /etc/hosts ~/backup/
	};;
	"recover" ) {
		cp -r ~/backup/hadoop-2.7.2/etc/hadoop/* /opt/module/hadoop-2.7.2/etc/hadoop/
		cp -r ~/backup/hive/conf/* /opt/module/hive/conf/
		cp -r ~/backup/zookeeper-3.4.10/conf/* /opt/module/zookeeper-3.4.10/conf/
		cp -r ~/backup/kafka/config/* /opt/module/kafka/config/
		cp -r ~/backup/flume/conf/* /opt/module/flume/conf/
		cp -r ~/backup/sqoop/conf/* /opt/module/sqoop/conf/
		cp -r ~/backup/spark/conf/* /opt/module/spark/conf/
		cp ~/backup/profile /etc/
		cp ~/backup/hosts /etc/
	};;
	* ) {
		echo "参数应为backup或recover"
	};;
	esac
}

# 启动HDFS
function runHDFS() {

	case $1 in
	"start" )
		ssh youname@ck "source /etc/profile; /opt/module/hadoop-2.7.2/sbin/start-dfs.sh"
		ssh youname@ck "source /etc/profile; /opt/module/hadoop-2.7.2/sbin/mr-jobhistory-daemon.sh start historyserver"
		ssh youname@dc "source /etc/profile; /opt/module/hadoop-2.7.2/sbin/start-yarn.sh"
	;;
	"stop" )
		ssh youname@dc "source /etc/profile; /opt/module/hadoop-2.7.2/sbin/stop-yarn.sh"
		ssh youname@ck "source /etc/profile; /opt/module/hadoop-2.7.2/sbin/mr-jobhistory-daemon.sh stop historyserver"
		ssh youname@ck "source /etc/profile; /opt/module/hadoop-2.7.2/sbin/stop-dfs.sh"
	;;
	esac
}

# 启动停止Presto/Yanagishima
function runPresto() {
	case $1 in
	"start" )
		nohup /opt/module/hive/bin/hive --service metastore > /dev/null 2>&1 &
		for i in ck dc tp
			do
				echo "Presto启动"
				ssh $i "source /etc/profile; /opt/module/presto/bin/launcher start"
			done
			sleep 5s
			nohup /opt/module/yanagishima-18.0/bin/yanagishima-start.sh > /dev/null 2>&1 &
	;;
	"stop" )
		for i in ck dc tp
			do
				echo "Presto停止"
				ssh $i "source /etc/profile; ps -ef| grep presto| grep -v grep| awk '{print \$2}'| xargs kill"
			done
			/opt/module/yanagishima-18.0/bin/yanagishima-shutdown.sh
			rm /opt/module/yanagishima-18.0/currentpid
	;;
	esac
}

# 启停Ganglia
function runGanglia() {
	case $1 in
	"start" )
	service httpd start && service gmetad start && service gmond start
	;;
	"stop" )
	service httpd stop && service gmetad stop && service gmond stop
	;;
	esac
}
