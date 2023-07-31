---
layout:  post
title:   超详细，从零开始搭建阿里云服务器（centos7）第五章 安装mysql
date:   2019-02-17 15:21:25
author:  'zhangtao'
image: '/img/post-bg-unix-linux.jpg'
catalog: [ WORK ]
tags:
- linux
- 数据库

---


## 1.下载MySQL源安装包

```java
wget http://dev.mysql.com/get/mysql57-community-release-el7-11.noarch.rpm
```

安装MySql源

```java
yum -y install mysql57-community-release-el7-11.noarch.rpm
```

查看一下安装效果

```java
yum repolist enabled | grep mysql.*
```

## 2.安装MySQL服务器

```java
yum install mysql-community-server
```

中间会弹出是与否的选择，选择y即可，然后耐心等待吧。。。。。。。

## 3.启动MySQL服务

```java
systemctl start  mysqld.service
```

运行一下命令查看一下运行状态

```java
systemctl status mysqld.service
```

## 4.初始化数据库密码

先不设置密码，然后vim /etc/my.cnf在里面找到 [mysqld] 这一项，然后在该配置项下添加 skip-grant-tables 这个配置，然后保存文件。


![img](https://img-blog.csdnimg.cn/20190217150038833.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L3FxXzM3MjIxOTkx,size_16,color_FFFFFF,t_70)

登录

```java
mysql -uroot -p
```

不需要输入密码直接回车即可登录mysql

修改密码

```java
update mysql.user set authentication_string=password('123456') where user='root';
```

mysql默认安装了密码安全检查插件（validate_password），默认密码检查策略要求密码必须包含：大小写字母、数字和特殊符号，并且长度不能少于8位。否则会提示ERROR 1819 (HY000): Your password does not satisfy the current policy requirements错误

exit退出mysql后 service mysqld restart 重启mysql

vim /etc/my.cnf 把skip-grant-tables这句话删掉，顺便加上utf-8编码

```java
character_set_server=utf8
```


![img](https://img-blog.csdnimg.cn/20190217151552283.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L3FxXzM3MjIxOTkx,size_16,color_FFFFFF,t_70)

## 5.数据库授权

数据库没有授权，只支持localhost本地访问

```java
mysql>GRANT ALL PRIVILEGES ON *.* TO 'root'@'%' IDENTIFIED BY '123456' WITH GRANT OPTION;
```

我在执行这句指令时，遇到了

```java
ERROR 1820 (HY000): You must reset your password using ALTER USER statement before executing this statement.
```

**这里翻阅了一下资料，mysql的密码会自动过期，需要重新设置密码，当然我们刚刚安装数据库不存在这么快过期。查了下发现是自己之前修改的密码太简单需要重新设置密码即可**

//远程连接数据库的时候需要输入用户名和密码 用户名：root 密码:123456 指点ip:%代表所有Ip,此处也可以输入Ip来指定Ip 输入后使修改生效还需要下面的语句

```java
mysql>FLUSH PRIVILEGES;
```

## 6.设置自动启动

```java
systemctl enable mysqld

systemctl daemon-reload
```

## 7.远程连接mysql


这里我使用了Navicat 来远程连接mysql ![img](https://img-blog.csdnimg.cn/20190217152048616.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L3FxXzM3MjIxOTkx,size_16,color_FFFFFF,t_70) 连接成功！

