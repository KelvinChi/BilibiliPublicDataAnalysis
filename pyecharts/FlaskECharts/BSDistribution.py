#!/usr/bin/env python3
# -*- coding: utf-8 -*-
# Author: CK
# CreateTime: 2020/1/6 20:21
import os

from flask import Flask, render_template
from pyecharts import options as opts
from pyecharts.charts import Bar

app = Flask(__name__, static_folder="templates")

def bar_base(initX, initY0, initY1) -> Bar:
    bar = (
        Bar()
            .add_xaxis(initX)  # 初始x轴状态
            .add_yaxis('工作日', initY0, label_opts=opts.LabelOpts(is_show=False))
            .add_yaxis('周末', initY1, label_opts=opts.LabelOpts(is_show=False))
            .set_global_opts(
                title_opts=opts.TitleOpts(title="Bilibili弹幕时间分布", subtitle=""),
                xaxis_opts=opts.AxisOpts(is_scale=True, type_="category", boundary_gap=True),
                yaxis_opts=opts.AxisOpts(is_scale=False, type_="value")
        )
    )
    return bar

@app.route("/")
def index():
    return render_template("BSDistribution.html")


@app.route("/barChart")
def get_bar_chart():
    with open(bsWorkdayPath) as bswd:
        bswdLines = bswd.readlines()
    initX = []
    initY1 = []
    for i in bswdLines:
        x = i.split(",")[0]
        y = i.split(",")[1][:-1]
        initX.append(x)
        initY1.append([x, y])

    with open(bsWeekendPath) as bswe:
        bsweLines = bswe.readlines()
    initY2 = []
    for j in bsweLines:
        m = j.split(",")[0]
        n = j.split(",")[1][:-1]
        initY2.append([m, n])
    # print(initX, initY1, initY2, sep='\n')
    c = bar_base(initX, initY1, initY2)
    return c.dump_options_with_quotes()


if __name__ == "__main__":
    packagePath = os.path.dirname(os.path.dirname(os.path.dirname(__file__))) # 套娃获取整包目录
    bsWorkdayPath = os.path.join(packagePath, "FlinkAnalysis/out/BSWorkday.csv")
    bsWeekendPath = os.path.join(packagePath, "FlinkAnalysis/out/BSWeekend.csv")
    # print(get_line_chart())
    # get_bar_chart()
    app.run()
