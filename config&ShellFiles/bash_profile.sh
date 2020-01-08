#!/usr/bin/env bash


# 在自己的.bash_profile或.bashrc中添加下边这段用以调用.bash_aliases
# 把所有的alias命令都放在.bash_aliases中统一管理
if [ -f ~/.bash_aliases ]; then
    . ~/.bash_aliases
fi