#!/usr/bin/env python3
# -*- coding: utf-8 -*-
# Author: CK
# CreateTime: 2020/1/3 15:17

"""
1. 抓取两个文件，一为视频的基本浏览信息，二为视频弹幕列表
2. 两个文件要么同时获取成功，要么失败
3. 统计以30分钟计，故爬取的数据粒度不需要太高
"""
import random
import redis
import requests
import json
import re
import time
import os


def xmlCreate(aid, xmlUrl):
    xmlPath = "output/xmlLog.csv"
    filePath = os.path.join(absPath, xmlPath)
    try:
        xmlReq = requests.get(url=xmlUrl, headers={"User-Agent": random.choice(userAgentList).strip()}, timeout=1)
        print("，\033[32mXml响应成功\033[0m") if xmlReq.status_code == 200 else print("，\033[31mXml响应失败\033[0m")
    except: # 有多种未知错误，偷懒，全部丢弃
        print("，\033[31mXml响应失败\033[0m")
        bsNum = -1
        return bsNum  # 表示xml响应失败
    xmlCon = xmlReq.content.decode(encoding="utf-8")
    nums = re.findall(r"<d p=\"(.*?)\">", xmlCon)
    comments = re.findall(r"\">(.*?)\<\/d", xmlCon)
    # 得到xml文件中时间与评论的元组列表
    conTuple = list(zip(nums, comments))
    # 得到弹幕总数
    bsNum = str(len(conTuple))
    # 将已有文件存入内存，因为弹幕会被去重，故体积不会很大，之后考虑存入MySQL
    if os.path.isfile(filePath):
        with open(filePath) as xl:
            xmlFileCon = xl.readlines()
    else:
        xmlFileCon = []
    xmlFile = open(filePath, "a")
    for i in conTuple:
        timeInfo = i[0].split(",")
        # 弹幕对应的视频时间
        vTime = timeInfo[0]
        # 弹幕创建时间
        cTime = timeInfo[4]
        # 单条弹幕内容
        comment = i[1]
        # 暂时数据量不会很大，因此先把log文件直接接进内存进行去重--------------------------------------------------------------
        xmlLog = aid + "," + vTime + "," + comment + "," + cTime + "\n"
        if xmlLog not in xmlFileCon:
            xmlFile.write(xmlLog)
    xmlFile.close()
    return bsNum


def jsonCreate(aid, bsNum, jsonAll):
    jsonPath = "output/jsonLog.csv"
    filePath = os.path.join(absPath, jsonPath)
    jsonData = jsonAll["data"]
    jsonOwner = jsonData["owner"]
    jsonStat = jsonData["stat"]
    # 为了避免数据冗余，将视频固有属性以aid为键存入Redis
    redisConnect = redis.Redis(host="localhost", port=6379, db=0)
    # hsetnx表示无则存入有则跳过
    redisConnect.hsetnx("bilibiliWorm", str(jsonData["aid"]), str(jsonData["title"]) + "," \
                        + str(jsonOwner["name"] + "," + str(jsonData["ctime"]) + "," + str(jsonData["duration"])))
    redisConnect.close()
    # 构建jsonLog样式
    jsonLog = aid + "," + str(jsonStat["like"]) + "," + str(jsonStat["dislike"]) + "," \
              + str(jsonStat["view"]) + "," + str(jsonStat["share"]) + "," + str(jsonStat["coin"]) + "," \
              + bsNum + "," + str(int(time.time() * 1000))
    with open(filePath, "a") as jl:
        jl.write(jsonLog + "\n")


def entrance(url, userAgentList):
    # 通过json获得xml的url，再通过xml得到弹幕总数
    aid = re.findall(r"av(\d+)\??", url)[0]
    jsonUrl = "https://api.bilibili.com/x/web-interface/view?aid=" + aid
    # 随机从文件中读取headers信息
    headersBody = random.choice(userAgentList).strip()
    try:
        jsonReq = requests.get(url=jsonUrl, headers={"User-Agent": headersBody}, timeout=1)
    except: # 有多种未知错误，偷懒，全部丢弃
        print("\033[31mJson请求失败\033[0m")
        return
    # print("HeadersBody %s获取正常" % headersBody)
    print("\033[32mJson响应成功\033[0m", end='') if jsonReq.status_code == 200 \
        else print("\033[31mJson请求失败\033[0m", end='')
    jsonAll = json.loads(jsonReq.content.decode(encoding="utf-8"))
    # 只有code为0才有意义
    if jsonAll["code"] == 0:
        # 从json数据中取出cid，得到弹幕xml列表
        try:
            cid = jsonAll["data"]["cid"]
            xmlUrl = "https://comment.bilibili.com/" + str(cid) + ".xml"
            # 获取弹幕数量，同时实现保存十秒内弹幕
            bsNum = xmlCreate(aid, xmlUrl)
        except KeyError:
            bsNum = "0"
            print("编号为%s的视频没有弹幕" % aid)
        if bsNum == -1:  # -1即响应失败，丢弃该条数据
            return
        jsonCreate(aid, bsNum, jsonAll)


def run(urlList):
    for i in urlList:
        url = i.strip()
        # 防止可能输入多个空格
        if url != "":
            print("正在处理%s" % url, "当前时间：" + time.strftime("%H-%M-%S", time.localtime(time.time())))
            entrance(url, userAgentList)
            # 设置随机等待时间
            time.sleep(random.random() * 3)


def main():
    urlsPath = "input/urls.csv"
    filePath = os.path.join(absPath, urlsPath)
    # 获取当前时间，单位为毫秒
    curTime = int(time.time() * 1000)

    # url获取
    # urlRaw = input("请输入网址，多个网址请以空格相隔，暂不支持视频列表\n")
    # urlList = urlRaw.split(" ")
    with open(filePath) as urls:
        urlList = urls.readlines()

    timeSet = input("如需设定时间停止，请输入时间（分钟），输入ctrl + c手动停止\n").strip()
    if timeSet == "":
        while True:
            run(urlList)
            time.sleep(10)
    try:
        timeLimit = int(timeSet * 1000) + curTime
        while int(time.time() * 1000) < timeLimit:
            time.sleep(10)
            run(urlList)
    except ValueError:
        print("输入有误，需要输入纯数字")
        # 这里最好写成循环之后再来-----------------------------------------------------------------------------------------
        exit()


if __name__ == '__main__':
    absPath = os.path.abspath(".")
    # 多个函数需要使用userAgentList故放外边
    with open("input/headers.csv") as ua:
        userAgentList = ua.readlines()
    main()
