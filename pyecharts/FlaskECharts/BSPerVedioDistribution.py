#!/usr/bin/env python3
# -*- coding: utf-8 -*-
# Author: CK
# CreateTime: 2020/1/6 23:27
import os
import time
from FlaskECharts.static.code_reuse import get_data

from flask import Flask, render_template, jsonify

app = Flask(__name__, static_folder="templates")


@app.route("/")
def index():
    return render_template("BSPerVedioDistribution.html")


@app.route("/dynamicData")
def update_data():
    global flag
    if flag == 0:
        flag += 1
        init_data = prepare_init_data(file_path)
        return jsonify({"value": init_data[0], "name": init_data[1]})
    try:
        for i in data:
            y_axis = i.split(",")
            # json_feedback.append(datum)
            # print(yAxis)
            # yAxis[1] = time_format(int(yAxis[1]))
            return jsonify({"value": y_axis})
    except StopIteration:
        time.sleep(5)
        exit()


def prepare_init_data(path):
    result = []
    with open(path, 'r') as f:
        for i in f.readlines():
            result.append(i.strip().split(","))
    return result, result[0][2]


if __name__ == "__main__":
    package_path = os.path.dirname(os.path.dirname(os.path.dirname(__file__)))  # 套娃获取整包目录
    file_path = os.path.join(package_path, "FlinkAnalysis/out/BSPerVedio.csv")
    # 用于判断是否首次载入数据
    flag = 0
    data = get_data(file_path)
    json_feedback = []
    # update_data()
    # print(prepare_init_data(file_path)[0], prepare_init_data(file_path)[1])
    app.run()
