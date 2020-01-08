#!/usr/bin/env python3
# -*- coding: utf-8 -*-
# Author: CK
# CreateTime: 2020/1/7 12:27
import os
import time

from flask import Flask, render_template, jsonify
from pyecharts import options as opts
from pyecharts.charts import Bar
from pyecharts.charts import Line
# 需要添加文件夹为source
from FlaskECharts.static.code_reuse import time_format

app = Flask(__name__, static_folder="templates")


def line_base(init_x, init_y0, init_y1, init_y2) -> Line:
    line = (
        Line()
            .add_xaxis(init_x)  # 初始x轴状态
            .add_yaxis('81394266', init_y0, label_opts=opts.LabelOpts(is_show=False))
            .add_yaxis('81516881', init_y1, label_opts=opts.LabelOpts(is_show=False))
            .add_yaxis('81417721', init_y2, label_opts=opts.LabelOpts(is_show=False))
            .set_global_opts(
                title_opts=opts.TitleOpts(title="Bilibili视频观看数", subtitle="标签为视频编号"),
                xaxis_opts=opts.AxisOpts(is_scale=True, type_="category", boundary_gap=True),
                yaxis_opts=opts.AxisOpts(is_scale=False, type_="value")
        )
    )
    return line


def bar_base(init_x, init_y0, init_y1, init_y2) -> Bar:
    bar = (
        Bar()
            .add_xaxis(init_x)  # 初始x轴状态
            .add_yaxis('81394266', init_y0, label_opts=opts.LabelOpts(is_show=False))
            .add_yaxis('81516881', init_y1, label_opts=opts.LabelOpts(is_show=False))
            .add_yaxis('81417721', init_y2, label_opts=opts.LabelOpts(is_show=False))
            .set_global_opts(
                title_opts=opts.TitleOpts(title="Bilibili视频观看数", subtitle="标签为视频编号"),
                xaxis_opts=opts.AxisOpts(is_scale=True, type_="category", boundary_gap=True),
                yaxis_opts=opts.AxisOpts(is_scale=False, type_="value")
        )
    )
    return bar


@app.route("/")
def index():
    return render_template("PageView.html")


@app.route("/lineChart")
def get_chart():
    try:
        init_data = next(data).split(",")
        start_time = int(init_data[2]) // 1000  # 时间戳单位转为秒
        init_x = []
        for i in range(10):
            init_x.append(time_format(start_time))
            start_time += 60 * 30
        init_y0 = [[init_x[0], init_data[1]]]
        init_y1 = [[init_x[0], init_data[4]]]
        init_y2 = [[init_x[0], init_data[7]]]
        for i in range(9):
            line = next(data).split(",")
            init_y0.append([init_x[i + 1], line[1]])
            init_y1.append([init_x[i + 1], line[4]])
            init_y2.append([init_x[i + 1], line[7]])
        print(init_x, init_y0, init_y1, init_y2, sep='\n')
        # if chose == "1" or chose == "bar":
        #     c = bar_base(init_x, init_y0, init_y1, init_y2)
        # else:  # 其它内容会在输入阶段得到筛选
        #     c = line_base(init_x, init_y0, init_y1, init_y2)
        # return c.dump_options_with_quotes()
    except StopIteration:
        time.sleep(5)
        exit()


@app.route("/lineDynamicData")
def update_bar_data():
    try:
        line = next(data).split(",")
        xAxis = time_format(int(line[2]) // 1000)
        yAxis = [line[1], line[4], line[7]]
        # 注意name的值如果不设置成str会无法显示
        return jsonify({"name": xAxis, "value": yAxis})
    except StopIteration:
        time.sleep(5)
        exit()

def local_data(path):
    with open(path, 'r') as f:
        for i in f.readlines():
            yield i.strip()


if __name__ == "__main__":
    # chose = input("请选择输入图样式：1. Bar；2. Line\n").lower().strip()
    # while chose != "1" and chose != "2" and chose != "bar" and chose != "line":
    #     chose = input("输入有点问题，请重新输入：1. Bar；2. Line\n")

    package_path = os.path.dirname(os.path.dirname(os.path.dirname(__file__)))  # 套娃获取整包目录
    file_path = os.path.join(package_path, "FlinkAnalysis/out/viewIncrsRatio.csv")
    data = local_data(file_path)
    # print(get_line_chart())
    get_chart()
    # app.run()
