#!/usr/bin/env python3
# -*- coding: utf-8 -*-
# Author: CK
# CreateTime: 2020/1/4 20:30
import time
from pyecharts.charts import Line, Bar, Timeline, Tab
from pyecharts import options as opts
from pyecharts.commons.utils import JsCode
from pyecharts.faker import Faker
from pyecharts.render import make_snapshot
# 使用 snapshot-selenium 渲染图片
from snapshot_selenium import snapshot
from pyecharts.globals import ThemeType


def viewTime(timestamp, data1, data2):
    formatTimeList = []
    for i in timestamp:
        formatTimeList.append(time.strftime("%H:%M", time.localtime(int(i) // 1000)))
    bar = (Bar(init_opts=opts.InitOpts(page_title=title,
                animation_opts=opts
                # 动画效果有“cubicOut”（默认）、“quadraticOut”、“quarticOut”、“quinticOut”、“sinusoidalOut”
                # “exponentialOut”、“circularOut”、“elasticOut”、“backOut”、“bounceOut”
                    .AnimationOpts(animation_delay=0, animation_easing="cubicOut"),
                    width="800px", height="500px"))
           .add_xaxis(formatTimeList)
           .add_yaxis("商家A", data1)
           .add_yaxis("商家B", data2)
           .set_global_opts(
            title_opts=opts.TitleOpts(title="主标题", subtitle="副标题"),
            toolbox_opts=opts.ToolboxOpts(), # 显示ToolBox
            legend_opts=opts.LegendOpts(is_show=True),
            datazoom_opts=[opts.DataZoomOpts(range_start=0, range_end=100)]))

    line = (Line()
           .add_xaxis(formatTimeList)
           .add_yaxis("商家A", data1,
                      label_opts=opts.LabelOpts(is_show=False),
                      linestyle_opts=opts.LineStyleOpts(width=2))
           .add_yaxis("商家B", data2,
                      label_opts=opts.LabelOpts(is_show=False),
                      linestyle_opts=opts.LineStyleOpts(width=2)))
    # 对数据最大值进行标注 markpoint_opts=opts.MarkPointOpts(data=[opts.MarkPointItem(type_="max")])
    # 启动层叠
    bar.overlap(line)
    # 获取全局option
    # print(bar.dump_options())
    # 渲染成html
    return bar
# # 渲染成图
# make_snapshot(snapshot, bar.render(), "./bar.png")



def timeline_bar(timestamp, data1, data2) -> Timeline:
    formatTimeList = []
    for i in timestamp:
        formatTimeList.append(time.strftime("%H:%M", time.localtime(int(i) // 1000)))
    tl = Timeline(init_opts=opts.InitOpts(width="900px", height="500px"))
    for i in range(2015, 2020):
        bar = (
            Bar()
                .add_xaxis(formatTimeList)
                .add_yaxis("商家A", Faker.values())
                .add_yaxis("商家B", Faker.values())
                .set_global_opts(title_opts=opts.TitleOpts("某商店{}年营业额".format(i)))
        )
        (tl.add(bar, "{}年".format(i))
         .add_schema(orient="horizontal", play_interval=500, symbol="circle",
                     symbol_size=2, pos_bottom="-5px", height="37px"))
    return tl


if __name__ == '__main__':
    title = "viewTimes"
    timestampList = ['1578059280100', '1578059460100', '1578059640100', '1578059820100',
                 '1578060000100', '1578060180100', '1578060360100']
    filePath = title + ".html"
    data1 = [465, 1488, 2474, 3951, 4903, 5345, 7304]
    data2 = [265, 1388, 2374, 3541, 4123, 5045, 7004]

    tab = Tab()
    tab.add(viewTime(timestampList, data1, data2), "viewTimes")
    tab.add(timeline_bar(timestampList, data1, data2), "Temp")

    tab.render(filePath)