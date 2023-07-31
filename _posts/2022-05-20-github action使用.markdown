---
layout:  post
title:   github action使用
date:   2022-05-20 15:07:28
author:  'zhangtao'
image: '/img/post-bg-unix-linux.jpg'
catalog: [ WORK ]
tags:
- linux
- github
- 服务器
- ssh

---


 *当我们需要发布服务时，往往是怎么操作的？最古老的方法无非是本地打包，然后ssh到服务器上，上传新包，删除老包，执行脚本启动。这个过程自己操作起来十分复杂，而且容易出错。 后来我们了解到，可以通过一些持续集成工具比如Jekins来运行脚本进行打包上传到服务器。但是还是不够便捷，有什么方法可以更便捷？* 

下面就引出这次使用的新工具，github的action工具。它的功能十分强大，我这边只介绍它的一部分功能来帮我实现CICD。当我提交代码合并到某个分支后，github action就可以自动帮我拉下代码打包，上传新包，删除老包，执行脚本启动。所有操作都不需要我来做，action的脚本自动帮我搞定，是不是很强大

下面我贴一段脚本，大部分语句相信大家都看得懂，其中${ 
<!-- -->{secrets.SERVER_IP}}等参数是维护在项目的secrets里面的

```java
on:
  push:
    branches: [ master ]

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v2
      - name: Set up JDK 1.8
        # 这里使用java11的环境, 其他项目在github action中找到对应的语言环境就行
        uses: actions/setup-java@v1
        with:
          java-version: 1.8
      - name: Build with Maven
        # 这里maven的打包命令, 其他项目修改为对应的打包命令
        run: |
          mvn package
      - name: scp jar upload cloud server
        uses: kostya-ten/ssh-server-deploy@v4
        with:
          scp_source: target/jiajiawork3-0.0.1-SNAPSHOT.jar
          scp_target: /home/zntzhang
          host: ${
   {secrets.SERVER_IP}}
          username: zntzhang
          password: ${
   {secrets.SERVER_PWD}}
      - name: Deploy to cloud server
        uses: appleboy/ssh-action@master
        with:
          host: ${
   {secrets.SERVER_IP}}
          username: zntzhang
          password: ${
   {secrets.SERVER_PWD}}
          port: 22
          script: cd /home/zntzhang;
                  ps -ef | grep jiajiawork3-0.0.1-SNAPSHOT.jar | grep -v grep | awk '{print $2}' | xargs kill -9;
                  source /etc/profile;
                  nohup java -jar jiajiawork3-0.0.1-SNAPSHOT.jar > jiajiawork3.log 2>&1 &
```

