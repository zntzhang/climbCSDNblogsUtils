---
layout:  post
title:   学习ES搜索引擎(一)--Mac安装ES和Kibana及简单实用
date:   2022-08-03 11:34:42
author:  'zhangtao'
image: '/img/post-bg-unix-linux.jpg'
catalog: [ WORK ]
tags:
- es
- elasticsearch

---


 *最近打算研究下ES和Kibana,打算先自己安装学习一下。通常我们服务器会通过log4j会把服务器日志输出到到控制台上来或者log文件中。但是如果是分布式系统，我们查看日志会很不方便，所以我们可以通过es+logstash+kibana来管理日志，首先通过logstash来将log文件写入到es中，然后通过kibana终端来搜索es中的日志* 


mac下安装软件非常方便，我们只需要输入下面命令即可安装

```java
brew install elasticsearch
```

安装完毕后输入下面命令就可以启动elasticsearch，启动后访问http://localhost:9200/如果能正常访问说明启动成功

```java
elasticsearch
```


也是同样，通过homebrew安装很方便

```java
brew install kibana
```

安装完后，我们需要修改下配置文件来实现连接ES，homebrew安装的软件都会在/usr/local/etc/目录下，我们修改配置文件，主要增加了这两项配置。我们用的kibana版本是7.0.2,不同版本配置的字段可能不同。

```java
elasticsearch.hosts: ["http://localhost:9200"]
kibana.index: ".kibana"
```

然后我们需要启动kibana,直接在命令行输入kibana就可以启动kibana。这里注意关闭命令行kibana也会关闭



我们已经成功将Kibana连接上了我们的ES，Kibana的控制台可以很方便的操作ES ![img](https://img-blog.csdnimg.cn/img_convert/e87c45ece4bf7881ff9c137dd3a894ce.png)

可以通过下面语句来练习ES语法

```java
// 新增文档
PUT /ecommerce/product/3
{
   
    "name" : "jiajieshi yagao",
    "desc" :  "youxiao fangzhu",
    "price" :  25,
    "producer" :      "jiajieshi producer",
    "tags": [ "fangzhu" ]
}

// 修改文档
PUT /ecommerce/product/3
{
   
    "name" : "zhonghua yagao1",
    "desc" :  "caoben zhiwu"
}

// 查询文档
GET /ecommerce/_search 
{
   "query":{
   "bool":{
   "must":[{
   "match_all":{
   }}],"must_not":[],"should":[]}},"from":0,"size":10,"sort":[],"aggs":{
   }}

// 删除文档
DELETE /ecommerce/product/3


// 查询所有索引
GET /_cat/indices?v

...
```



首先我们点击Management里的Stack Management菜单，然后创建一个index pattern (索引匹配规则) ![img](https://img-blog.csdnimg.cn/img_convert/7ec182b130a3ffbf8aba97f4d0532cd1.png)


创建完成后，Kibana会将es的index中的属性转换为Kibana的fields ![img](https://img-blog.csdnimg.cn/img_convert/6822325609193744bc05dad3a2265d01.png)


创建成功后，返回Discover菜单，即可查询对应index的数据 ![img](https://img-blog.csdnimg.cn/img_convert/a4a21630e02fac9e2d3bc5267826c947.png)

参考资料  [Kibana7.x基本的操作](https://blog.csdn.net/zjcjava/article/details/99370346)

