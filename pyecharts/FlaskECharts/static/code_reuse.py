#!/usr/bin/env python3
# -*- coding: utf-8 -*-
# Author: CK
# CreateTime: 2020/1/7 20:09
import time
import FlaskECharts.static.Threadsafe as ts


# 实现类似tail -f的功能，通过装饰器实现线程安全，拒绝出现ValueError: generator already executing
@ts.threadsafe_generator
def get_data(path):
    with open(path, 'r') as f:
        f.seek(0, 2)  # 00表示文件指针从头开始，02表示末尾
        while True:
            line = f.readline()
            if not line:
                time.sleep(0.1)
                continue
            yield line.strip()


def init_data(path):
    result = []
    with open(path, 'r') as f:
        for i in f.readlines():
            result.append(i.strip().split(","))
    return result


def time_format(timestamp):
    timeStr = time.strftime("%H:%M", time.localtime(timestamp))
    return timeStr