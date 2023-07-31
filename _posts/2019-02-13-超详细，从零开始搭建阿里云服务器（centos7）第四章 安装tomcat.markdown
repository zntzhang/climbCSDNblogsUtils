---
layout:  post
title:   超详细，从零开始搭建阿里云服务器（centos7）第四章 安装tomcat
date:   2019-02-13 15:42:15
author:  'zhangtao'
image: '/img/post-bg-unix-linux.jpg'
catalog: [ WORK ]
tags:
- linux

---


下面到了最关键的一步，安装tomcat过程中也是不断地爬坑。。

## 1.下载并上传tomcat压缩包

跟上一章类似，不细说了，我安装在了/home/zntzhang/tomcat7中

## 2.解压压缩包

```java
tar -zxvf apache-tomcat-7.0.92.tar.gz
```


![img](https://img-blog.csdnimg.cn/2019021315105469.png)

## 3.启动tomcat

进入到tomcat的bin目录下，执行 ./start.sh 即可启动tomcat，这时候可以通过curl 127.0.0.1:8080来测试是否成功

## 4.配置安全组

阿里云服务器需要配置安全组，否则外部无法访问到对应的端口，这也是阿里云的安全措施  [阿里云安全组策略如何配置](https://jingyan.baidu.com/article/fea4511a2e387cf7bb912532.html)

## 5.防火墙设置

centos7默认使用的是新的防火墙体系firewalld，默认是关闭状态，会造成安全隐患，首先我们要开启防火墙并且设置成开机自启

```java
systemctl start firewalld 启动防火墙
systemctl enable firewalld 开机自启防火墙
```

注意：这时候需要切换root用户

这时候发现发现无法再连接上tomcat，原因是防火墙没有开放对应的端口

### 添加常用的端口

```java
firewall-cmd --zone=public --add-port=8080/tcp --permanent
firewall-cmd --zone=public --add-port=80/tcp --permanent
firewall-cmd --zone=public --add-port=3306/tcp --permanent
```

…

–zone #作用域

–add-port=8080/tcp #添加端口，格式为：端口/通讯协议

–permanent #永久生效，没有此参数重启后失效

之后重启防火墙：firewall-cmd --reload

再次尝试，发现并没有什么软用，原来firewall还有一个坑就是光打开端口没有用，还需要开启对应的服务才行 - -

### 开启端口对应的服务

```java
firewall-cmd --zone=public --list-ports   查看所有打开的端口
```


通过firewall-cmd --list-services这个命令我们查看当前打开了那些服务,比如下面这个 ![img](https://img-blog.csdnimg.cn/20190213153639682.png) 一开始是没有 http和ftp的服务，需要自己添加，可以通过下面的命令添加一个服务到firewalld

```java
firewall-cmd --permanent --add-service=http
firewall-cmd --permanent --add-service=ftp
firewall-cmd --permanent --add-service=mysql
```

… 然后通过systemctl restart firewalld.service 重启防火墙就生效了

