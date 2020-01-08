#!/usr/bin/env python3
# -*- coding: utf-8 -*-
# Author: CK
# CreateTime: 2020/1/7 20:00
import threading


# 代码来源https://www.jianshu.com/p/79dcfd77e56c
class Threadsafe_iter:
    def __init__(self, it):
        self.it = it
        self.lock = threading.Lock()

    def __iter__(self):
        return self

    def __next__(self):  # python3
        with self.lock:
            return self.it.__next__()


def threadsafe_generator(f):
    def g(*a, **kw):
        return Threadsafe_iter(f(*a, **kw))
    return g
