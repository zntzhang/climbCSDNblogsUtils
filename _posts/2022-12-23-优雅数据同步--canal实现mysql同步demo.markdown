---
layout:  post
title:   优雅数据同步--canal实现mysql同步demo
date:   2022-12-23 17:56:17
author:  'zhangtao'
image: '/img/post-bg-unix-linux.jpg'
catalog: [ WORK ]
tags:
- 数据库
- mysql
- docker
- canal

---


当需要两张表数据同步的时候，我们会想到几种方案？ 最简单的一种方式就是触发器的方式。例如A同步到B,可以通过下面的sql来添加触发器

```java
create trigger tri_trade_update 
after UPDATE 
on `A`
for each row
begin 
update `B` 
set 
company_id = new.`company_id`,
supplier_id =new.`supplier_id`
WHERE id=old.`id`;   
end;
```

但是这种方式有一定的弊端和局限性，首先就是只局限同库，并且会增大数据库开销，以及无法实现一些自定义的逻辑。

canal是阿里推出的一个开源的中间件，它的原理类似于mysql主从的原理，它把自己伪装成mysql的从库，这样就可以从主库中获取binLog来复制数据

接下来我们自己搭建一个canal同步mysql的一个demo。我们都通过docker来安装mysql和canal,这样比较方便


1、首先在docker搜索mysql的官方镜像。我们选择最新的版本latest然后拉取下来 2、运行这个镜像 3、使用命令把mysql容器内部存储数据文件拷贝到外部目录存储。

```java
docker cp mysql:/etc/mysql/my.cnf /Users/admin/WORK/docker/mysql/config
    docker cp mysql:/var/lib/mysql /Users/admin/WORK/docker/mysql/data
```

4、 删除运行的mysql容器 5、 重新启动mysql容器 -v 挂载容器文件到外部目录，这样我们就可以持久化mysql的数据不会随着容器关闭而消失。 -p 将容器内mysql的端口映射到外部端口上 -e 设置环境变量

```java
docker run -d \
    --name mysql \
    -p 3306:3306 \
    -v /Users/admin/WORK/docker/mysql/config/my.cnf:/etc/mysql/my.cnf \
    -v /Users/admin/WORK/docker/mysql/data/mysql:/var/lib/mysql \
    -e MYSQL_ROOT_PASSWORD=123456 \
    mysql:latest
```


1、首先在docker搜索canal-server的官方镜像。选择最新的版本latest然后拉取下来 2、运行这个镜像 3、使用命令把mysql容器内部存储数据文件拷贝到外部目录存储。

```java
docker cp canal:/home/admin/canal-server/conf/example/instance.properties /Users/admin/WORK/docker/cancal/conf
```

4、 删除运行的canal容器 5、 重新启动canal容器 <em>-v 挂载容器文件到外部目录，这样我们就可以持久化mysql的数据不会随着容器关闭而消失。 -p 将容器内mysql的端口映射到外部端口上 -e 设置环境变量</em>

```java
docker run -d \
    --name canal \
    -p 11111:11111 \
    -v /Users/admin/WORK/docker/cancal/conf/instance.properties:/home/admin/canal-server/conf/example/instance.properties \
    canal/canal-server:latest
```


## 修改本地my.cnf文件。将canal同步需要的配置添加上

```java
log-bin=mysql-bin #开启binlog
binlog-format=ROW #row格式
server_id=1 #主从标识
```

创建canal账号，并赋予同步权限

```java
# 创建账号
CREATE USER canal IDENTIFIED BY 'canal'; 
# 授予权限
GRANT SELECT, REPLICATION SLAVE, REPLICATION CLIENT ON *.* TO 'canal'@'%';
# 刷新并应用
FLUSH PRIVILEGES;
# 建库
create database canal;
# 查看设置binlog是否生效
show variables like 'log_bin';
```

## 修改instance.properties文件

修改canal.instance.master.address配置，ip改成mysql容器的内部ip。注意容器跟容器访问不能使用127.0.0.1

## 重启mysql容器，重启canal容器

进入canal容器中，观察日志more canal-server/logs/example/example.log 查看是否报错


```java
package logistics.canal;


import com.alibaba.otter.canal.client.CanalConnector;
import com.alibaba.otter.canal.client.CanalConnectors;
import com.alibaba.otter.canal.protocol.CanalEntry.Entry;
import com.alibaba.otter.canal.protocol.CanalEntry.EntryType;
import com.alibaba.otter.canal.protocol.CanalEntry.RowChange;
import com.alibaba.otter.canal.protocol.CanalEntry.RowData;
import com.alibaba.otter.canal.protocol.Message;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Canal测试
 *
 * @author admin
 * @date 2022/12/23
 */
public class CanalTest {
   

    public static void main(String[] args) {
   
        String ip = "127.0.0.1";
        String destination = "example";
        //创建连接对象
        CanalConnector canalConnector = CanalConnectors.newSingleConnector(
                new InetSocketAddress(ip, 11111), destination, "", ""
        );

        //进行连接
        canalConnector.connect();
        //进行订阅
        canalConnector.subscribe();

        int batchSize = 5 * 1024;
        //使用死循环不断的获取canal信息
        while (true) {
   
            //获取Message对象
            Message message = canalConnector.getWithoutAck(batchSize);
            long id = message.getId();
            int size = message.getEntries().size();

            System.out.println("当前监控到的binLog消息数量是：" + size);

            //判断是否有数据
            if (id == -1 || size == 0) {
   
                //如果没有数据，等待1秒
                try {
   
                    TimeUnit.SECONDS.sleep(1);
                } catch (InterruptedException e) {
   
                    e.printStackTrace();
                }
            } else {
   
                //如果有数据，进行数据解析
                List<Entry> entries = message.getEntries();

                //遍历获取到的Entry集合
                for (Entry entry : entries) {
   
                    System.out.println("----------------------------------------");
                    System.out.println("当前的二进制日志的条目（entry）类型是：" + entry.getEntryType());

                    //如果属于原始数据ROWDATA，进行打印内容
                    if (entry.getEntryType() == EntryType.ROWDATA) {
   
                        try {
   
                            //获取存储的内容
                            RowChange rowChange = RowChange.parseFrom(entry.getStoreValue());

                            //打印事件的类型，增删改查哪种 eventType
                            System.out.println("事件类型是：" + rowChange.getEventType());

                            //打印改变的内容(增量数据)
                            for (RowData rowData : rowChange.getRowDatasList()) {
   
                                System.out.println("改变前的数据：" + rowData.getBeforeColumnsList());
                                System.out.println("改变后的数据：" + rowData.getAfterColumnsList());
                            }

                        } catch (Exception e) {
   
                            e.printStackTrace();
                        }
                    }
                }
                //消息确认已经处理了
                canalConnector.ack(id);
            }
        }
    }
}
```



开启客户端，mysql执行sql语句。我们可以看到客户端日志变化，说明成功了 ![img](https://img-blog.csdnimg.cn/eecfbff41a0c43fd91c468fd5619c0b5.png)


## 1、mysql启动时报错。Different lower_case_table_names settings for server (‘1‘) and data dictionary (‘0‘)

解决方法: my.cnf里面添加lower_case_table_names = 1。同时删除data里面的内容，去掉data目录的挂载，重新启动容器再复制一份到data中。这样再次启动就不会报错了

## 2、canal日志报错MySQL8.0 caching_sha2_password Auth failed，无法连接mysql

解决方法: mysql执行下面语句

```java
ALTER USER 'canal'@'%' IDENTIFIED BY 'canal' PASSWORD EXPIRE NEVER;
ALTER USER 'canal'@'%' IDENTIFIED WITH mysql_native_password BY 'canal';
FLUSH PRIVILEGES;
```

