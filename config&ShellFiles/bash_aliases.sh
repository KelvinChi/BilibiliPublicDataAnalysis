#!/usr/bin/env bash

# 先把函数文件包引用下，类似import
#. ~/.bash_foo
. ~/.bash_foo1
. ~/.bash_hive

# 以下是自定义内容，按自己的习惯来即可

# some more ls aliases
alias ls='ls -GF'
alias ll='ls -lhF -G'
alias la='ls -A'
alias l='ls -CF'

# 刷新
alias re='source ~/.bashrc && source /etc/profile && echo 刷新完成，注意同步'
# 编辑本文件
alias ca='vim ~/.bash_aliases'
alias cb='vim ~/.bashrc'
alias cc='vim ~/.bash_foo'
alias vr='vim ~/.vimrc'
alias ch='vim ~/.bash_hive'
# 登录其它主机
alias ck='ssh youname@ck'
alias dc='ssh youname@dc'
alias tp='ssh youname@tp'
# 建立临时链接
alias pl='python3 -m http.server 8000'

# 同步脚本
alias xs='bash ~/script/xsync.sh'
# 同步配置文件
alias syn='
xs ~/.bashrc > /dev/null 2>&1 &&
xs ~/.bash_aliases > /dev/null 2>&1 &&
xs ~/.bash_foo > /dev/null 2>&1 &&
xs ~/.bash_profile > /dev/null 2>&1 &&
xs ~/.bash_hive > /dev/null 2>&1 &&
xs ~/.vimrc > /dev/null 2>&1 &&
xs /etc/profile > /dev/null 2>&1 &&
xs /etc/hosts > /dev/null 2>&1 &&
bk backup &&
bkpbash
echo ".bash*同步完成"'
# 备份配置到Mac
alias bkpbash='backupBashToMac'

# 启动集群
alias stdf='/opt/module/hadoop-2.7.2/sbin/start-dfs.sh'
alias spdf='/opt/module/hadoop-2.7.2/sbin/stop-dfs.sh'
alias styn='/opt/module/hadoop-2.7.2/sbin/start-yarn.sh'
alias spyn='/opt/module/hadoop-2.7.2/sbin/stop-yarn.sh'
alias hf='/opt/module/hadoop-2.7.2/bin/hdfs namenode -format'

# 生成日志数据
alias lg='logCreater'

# 集群运行
alias all='runAll'

# 修改集群时间
alias tcall='timeChange'

# 启动、停止flume
alias stfl='runFlume start'
alias spfl='runFlume stop'

alias stflc='runFlumeConsumer start'
alias spflc='runFlumeConsumer stop'

# 启动集群
alias stcl='runCluster start'
alias spcl='runCluster stop'

# 删除集群资料
alias rmall='rmAll'

# 备份数据
alias bk='backupCluster'

# 登录mysql
alias mr='mysql -uyourname -p000000 --prompt="\\u [\\d]> "'

# 启动HDFS
alias hst='runHDFS start'
alias hsp='runHDFS stop'

# hive快捷键
alias hive="/opt/module/hive/bin/hive"
# sqoop快捷键
alias sqoop="/opt/module/sqoop/bin/sqoop"

# 找错
alias fe='grep -i "error" -C 1'

# 启停azkaban
alias stak='/opt/module/azkaban/executor/bin/azkaban-executor-start.sh && sleep 2s && sh /opt/module/azkaban/server/bin/azkaban-web-start.sh > /dev/null 2>&1 &'
alias spak='sh /opt/module/azkaban/server/bin/azkaban-web-shutdown.sh &&sh /opt/module/azkaban/executor/bin/azkaban-executor-shutdown.sh'

# 启停Presto
alias stpy='runPresto start'
alias sppy='runPresto stop'

# safemode
alias sf='hdfs dfsadmin -safemode'

# 启停Spark
alias stsk='/opt/module/spark/sbin/start-all.sh'
alias spsk='/opt/module/spark/sbin/stop-all.sh'

# 后台启动hive metastore
alias sthm='nohup /opt/module/hive/bin/hive --service metastore > /dev/null 2>&1 &'

# 启动jupyter
alias stjp='jupyter notebook --allow-root'

# 启停Ganglia
alias stgl='runGanglia start'
alias spgl='runGanglia stop'

# 手动回收内存
alias gc='echo 3 > /proc/sys/vm/drop_caches && sleep 1s && echo 0 > /proc/sys/vm/drop_caches'

# fold size
alias fs='du -lh --max-depth=1'

# bilibili相关命令
# 启停flume读取xmlLog文件，注意Flume的配置文件地址与.bash_foo中的runFlumeXml函数的配置地址
alias stflx='runFlumeXml start'
alias spflx='runFlumeXml stop'
# 同理flume读取jsonLog
alias stflj='runFlumeJson start'
alias spflj='runFlumeJson stop'

# 操作zk，Kafka启动前记得先起kafka
alias stzk='/opt/module/zookeeper-3.4.10/bin/zkServer.sh start'
alias spzk='/opt/module/zookeeper-3.4.10/bin/zkServer.sh stop'
alias zkss='/opt/module/zookeeper-3.4.10/bin/zkServer.sh status'
alias stzkall='runZK start'
alias spzkall='runZK stop'
alias zkssall='runZK status'
alias zk='/opt/module/zookeeper-3.4.10/bin/zkServer.sh'
alias stzc='/opt/module/zookeeper-3.4.10/bin/zkCli.sh'

# 启动、停止kafka
alias stkf='runKafka start'
alias spkf='runKafka stop'
alias stkm='runKafkaManager start'
alias spkm='runKafkaManager stop'
alias kf='/opt/module/kafka/bin/kafka-topics.sh --zookeeper ck:2181,ck:2181,tp:2181'