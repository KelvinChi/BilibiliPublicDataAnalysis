# Readme

## 项目介绍

### 啰嗦

本项目基于Bilibili的分开信息，实现需求的主要模块为[Flink](https://flink.apache.org/) + [eCharts](https://www.echartsjs.com/zh/option.html)，点击直达官网。
项目代码主要是Scala + Python实现，建议学习大数据的最好掌握Java或Scala，学习资料丰富些。

[GitHub地址](https://github.com/KelvinChi/BilibiliPublicDataAnalysis)

### 项目架构

#### 项目环境版本信息

| Scala | Python | Flink | Flume | Kafka | CentOS | Redis |
| :-: | :-: | :-: | :-: | :-: | :-: | :-: |
| 2.11.8 | 3.7.3 | 1.7.2 | 1.7.0 | 0.11.0.2 | 6.8 | 2.8.17 |

#### 一图胜千言

![项目流程](https://upload-images.jianshu.io/upload_images/2083763-453b33fcacaa685f.png?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)

### 静态图表

来看看效果，先上个静态的，这个简单需求完全可以不用框架实现，但已奉上粗漏的Flink代码，让大佬们适应一小下。

![弹幕时间分布](https://upload-images.jianshu.io/upload_images/2083763-1a6995aa46791004.png?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)

### 动态图表
- 需求一：弹幕在视频时间线上分布情况

![弹幕散点](https://upload-images.jianshu.io/upload_images/2083763-b07565373b95000f.gif?imageMogr2/auto-orient/strip)

- 需求二：视频净观看量、设定时间段内增量及增长率动态统计

![观看量增长](https://upload-images.jianshu.io/upload_images/2083763-8d3d09d097da6dba.gif?imageMogr2/auto-orient/strip)

*为了调度代码我把时间设置得非常小，实际使用应该以小时计*

## 跑项目的一些小建议

### 启动顺序
1. Flume、Kafka启动顺序
   - 首先需要启动集群ZooKeeper，然后是Kafka服务、Flume
2. 启动Redis
   - 爬虫使用了Redis存储视频一些静态信息
2. 启动爬虫
   - 爬虫文件为PythonWorm/bilibiliWorm.py
3. 启动Flink
   - FlinkAnalysis/src/main/scala/com.ck.bulletScreen & pageView
   - PV还有静态代码在
4. 启动Flask
   - pyecharts下，每个需求一个文件
   - py会有一个对应的html文件，在html中写的eCharts代码，参考官方文档，要啥有啥

### 小提示
在config&ShellFiles里放了flume配置文件与一些脚本文件，方便有需要的借鉴~

#### 文件说明
- xsync.sh
  - 同步文件脚本
- .bash_foo
  - 函数脚本，将各种启动、维护代码写成函数，放入以统一管理
- .bash_aliases
  - alias统一管理脚本
- .bash_profile
  - 在这个文件中需要加入引用.bash_aliases的代码

**注意以上 . 开头的文件都放在 ~ 中**

## 项目优化

### 待优化
1. 暂时爬到的弹幕还是放在内存中去重，但大数据嘛，肯定不能这么干啊，之后会补充上布隆加Redis方案，使用布隆可能有少量数据会错误识别，但弹幕嘛，要求也没那么高
2. 暂时各个图表需求都是分散的，且没有改Flask端口，一次只能启动一个；之后会将所有图集成到一个网页中，也考虑多端口使用

### 已优化
1. ~~爬虫卡死：request添加timeout参数~~
2. ~~图表初始数据载入：通过设置flag及prepare_init_data函数处理初始数据，通过jsonify传递给js载入；散点图载入全量数据，PV图载入最后十条数据，如果数据不够则只显示对应条目数据~~
3. ~~散点图鼠标无触发信息，之后加上这个功能~~

## 一些参考链接
- [Java判断当前日期是否是节假日](https://blog.csdn.net/my_ha_ha/article/details/96144394)
  - 基于网页服务提供的，因为没法大量申请，所以放弃使用了。
- [Python操作Redis数据库](https://www.cnblogs.com/cnkai/p/7642787.html)
- [利用 Flask 动态展示 Pyecharts 图表数据的几种方法](https://www.jianshu.com/p/6910712e9b64)
- [解决github图片不显示的问题](https://blog.csdn.net/qq_38232598/article/details/91346392)
   - 我遇到的小问题，供遇同问题的大佬参考
