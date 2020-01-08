#!/usr/bin/env python3
# -*- coding: utf-8 -*-
# Author: CK
# CreateTime: 2020/1/7 12:27
import os
import time

from flask import Flask, render_template, jsonify
# 需要添加文件夹为source
from FlaskECharts.static.code_reuse import get_data, time_format, init_data

app = Flask(__name__, static_folder="templates")


@app.route("/")
def index():
    return render_template("PageView.html")


@app.route("/dynamicData")
def update_bar_data():
    global flag
    if flag == 0:
        flag += 1
        return jsonify(prepare_init_data())
    try:
        for i in data:
            line = i.split(",")
            x_axis = time_format(int(line[2]) // 1000)
            y_axis = [float(line[1]), float(line[4]), float(line[7])]
            return jsonify({"time": x_axis, "value": y_axis})
    except StopIteration:
        time.sleep(5)
        exit()

# 构建初始化图表数据
def prepare_init_data():
    try:
        data = init_data(file_path)[-10:]
        names = [data[0][0], data[0][3], data[0][6]]
        init_x_list = []
        init_y_list_0 = []
        init_y_list_1 = []
        init_y_list_2 = []
        for i in data:
            init_x_list.append(time_format((int(i[2]) // 1000)))
            init_y_list_0.append(float(i[1]))
            init_y_list_1.append(float(i[4]))
            init_y_list_2.append(float(i[7]))
        return {"time": init_x_list, "init_y_list_0": init_y_list_0,
                "init_y_list_1": init_y_list_1, "init_y_list_2": init_y_list_2,
                "name": names, "title": title, "subtitle": subtitle, "type": type}
    except IndexError:
        print("必须保证文件内至少有一条数据")


if __name__ == "__main__":
    # flag作用是标识初始化的数据
    flag = 0

    package_path = os.path.dirname(os.path.dirname(os.path.dirname(__file__)))  # 套娃获取整包目录
    file_path = os.path.join(package_path, "FlinkAnalysis/out/viewIncreasement.csv")

    # 这里设置好图的主副标题哦，可以不写
    title = "视频观看净增长量"
    subtitle = "数据名为视频编码"
    # 这里可设置线图或柱图（line, bar），线图默认开启平滑
    type = 'bar'

    data = get_data(file_path)
    # print(prepare_init_data())
    app.run()

