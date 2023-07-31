---
layout:  post
title:   超详细，从零开始搭建阿里云服务器（centos7）第六章 安装Maven和RocketMq
date:   2020-07-23 09:45:10
author:  'zhangtao'
image: '/img/post-bg-unix-linux.jpg'
catalog: [ WORK ]
tags:
- mq
- linux

---


RocketMq是真的难搭，废了好多功夫才搭建完，碰到了好多坑，下面我把我吃到的坑记录下来


## 1. 安装Maven

### 1.1 下载maven

```java
wget http://mirrors.shu.edu.cn/apache/maven/maven-3/3.6.0/source/apache-maven-3.6.0-src.tar.gz
```

解压maven

```java
tar -zxvf  apache-maven-3.6.0-src.tar.gz
```

### 1.2 编辑环境变量

```java
vim /etc/profile
```

添加以下配置

```java
export M2_HOME=/Users/zntzhang/WORK/apache-maven-3.6.3
export PATH=$PATH:$M2_HOME/bin
```

保存后

```java
source /etc/profile
```

生效

## 2. 下载解压Apache-RocketMQ

### 2.1 下载mq

```java
wget https://github.com/apache/rocketmq/archive/rocketmq-all-4.3.0.tar.gz
```

解压

```java
tar -zxvf rocketmq-all-4.3.0.tar.gz
```

### 2.2 maven打包

进入到mq根目录，打包

```java
mvn -Prelease-all -DskipTests clean install -U
```

找到打包后的target目录

```java
cd distribution/target/apache-rocketmq/
pwd
```

记录这个路径，打开配置文件，添加环境变量

```java
export rocketmq=/Users/zntzhang/WORK/rocketmq-rocketmq-all-4.3.0/distribution/target/apache-rocketmq
export PATH=$PATH:$rocketmq/bin
```

记得source以下立即生效。

## 3. 启动MQ

启动前先创建打日志的文件夹 如 /Users/zntzhang/WORK/log/rocketmqlogs/

然后进入到target的bin下启动

```java
nohup mqnamesrv >/Users/zntzhang/WORK/log/rocketmqlogs/namesrv.log 2>&1 &
```

```java
nohup mqbroker -n 127.0.0.1:9876 >/Users/zntzhang/WORK/log/rocketmqlogs/broker.log 2>&1 &
```


这么启动，大概率会启动失败，我们可以通过查看日志分析

1. 报错 Java HotSpot™ 64-Bit Server VM warning: INFO: os::commit_memory(0x00000006ec800000, 2147483648, 0) failed; error=‘Cannot allocate memory’


![img](https://imgconvert.csdnimg.cn/aHR0cHM6Ly9naXRlZS5jb20venQxOTk0MTIxNC9pbWFnZUJlZC9yYXcvbWFzdGVyL3VQaWMvbXExLnBuZw?x-oss-process=image/format,png)

RocketMq默认启动分配的内存极大，如果我们机子的内存没那么大的话是不会启动成功的。

具体需要修改jvm参数的配置文件是 runserver.sh runbroker.sh mqnamesrv.xml mqbroker.xml

1. 错误: 找不到或无法加载主类 org.apache.rocketmq.namesrv.NamesrvStartup


![img](https://imgconvert.csdnimg.cn/aHR0cHM6Ly9naXRlZS5jb20venQxOTk0MTIxNC9pbWFnZUJlZC9yYXcvbWFzdGVyL3VQaWMvbXEyLnBuZw?x-oss-process=image/format,png)

RocketMq的环境变量配错，检查是不是target下的。 如果环境变量没配置错的话，那么就是启动时没在target的bin下启动

 *参考文章：* 

 [在linux环境安装单机RocketMQ](https://juejin.im/post/5bffa58851882558ae3c23ea)

