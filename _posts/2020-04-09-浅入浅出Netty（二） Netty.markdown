---
layout:  post
title:   浅入浅出Netty（二） Netty
date:   2020-04-09 22:32:38
author:  'zhangtao'
image: '/img/post-bg-unix-linux.jpg'
catalog: [ WORK ]
tags:
- netty

---


这一遍先简单的讲一些netty是如何实现nio的代码的，后面会详细讲述netty原理


用一句简单的话来说就是：Netty 封装了 JDK 的 NIO，让你用得更爽，你不用再写一大堆复杂的代码了。

用官方正式的话来说就是：Netty 是一个异步事件驱动的网络应用框架，用于快速开发可维护的高性能服务器和客户端。


- 使用 JDK 自带的NIO需要了解太多的概念，编程复杂，一不小心 bug 横飞 
- Netty 底层 IO 模型随意切换，而这一切只需要做微小的改动，改改参数，Netty可以直接从 NIO 模型变身为 IO 模型 
- Netty 自带的拆包解包，异常检测等机制让你从NIO的繁重细节中脱离出来，让你只需要关心业务逻辑 
- Netty 解决了 JDK 的很多包括空轮询在内的 Bug 
- Netty 底层对线程，selector 做了很多细小的优化，精心设计的 reactor 线程模型做到非常高效的并发处理 
- 自带各种协议栈让你处理任何一种通用协议都几乎不用亲自动手 
- Netty 社区活跃，遇到问题随时邮件列表或者 issue 
- Netty 已经历各大 RPC框架，消息中间件，分布式通信中间件线上的广泛验证，健壮性无比强大


首先添加pom依赖

```java
<dependencies>
        <dependency>
            <groupId>io.netty</groupId>
            <artifactId>netty-all</artifactId>
            <version>4.1.6.Final</version>
        </dependency>
    </dependencies>
```

然后是代码部分

```java
public class Client {
   

    public static void main(String[] args) {
   
        EventLoopGroup nioEventLoopGroup = new NioEventLoopGroup();
        Bootstrap bootstrap = new Bootstrap();//客户端引导类
        //EventLoopGroup可以理解为是一个线程池，这个线程池用来处理连接、接受数据、发送数据
        bootstrap.group(nioEventLoopGroup)//多线程处理
                .channel(NioSocketChannel.class)//制定通道类型为NioSocketChannel
                .handler(new ChannelInitializer<SocketChannel>() {
   //业务处理类
                    @Override
                    protected void initChannel(SocketChannel ch) throws Exception {
   
                        ch.pipeline().addLast(new ClientHandler());//注册handler
                    }
                });
        // 4.建立连接
        bootstrap.connect("127.0.0.1", 8000).addListener(future -> {
   
            if (future.isSuccess()) {
   
                System.out.println("连接成功!");
            } else {
   
                System.err.println("连接失败!");
            }
        });

    }
}

public class ClientHandler extends SimpleChannelInboundHandler<ByteBuf> {
   

    //客户端连接服务器后被调用
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
   
        System.out.println("客户端连接服务器，开始发送数据......");
        byte[] req = "QUERY TIME ORDER".getBytes();//请求消息
        ByteBuf firstMessage = Unpooled.buffer(req.length);//发送类
        firstMessage.writeBytes(req);//发送
        ctx.writeAndFlush(firstMessage);//flush
    }

    //从服务器收到数据后调用
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ByteBuf buf) throws Exception {
   
        System.out.println("client 读取server数据..");
        //服务器返回消息后
        byte[] req = new byte[buf.readableBytes()];//创建一个存储信息的byte数组
        buf.readBytes(req);//将buffer中的数据读到byte数组中
        String body = new String(req, "UTF-8");//将byte数组转换为String(并转码)
        System.out.println("服务端数据为：" + body);//打印服务端反馈的信息
    }

    //发生异常时调用
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
   
        System.out.println("client exceptionCaught...");
        //释放资源
        ctx.close();
    }
}

public class Server {
   

    public static void main(String[] args) {
   
        // bossGroup表示监听端口，accept 新连接的线程组，workerGroup表示处理每一条连接的数据读写的线程组
        NioEventLoopGroup bossGroup = new NioEventLoopGroup();
        NioEventLoopGroup workerGroup = new NioEventLoopGroup();
        //server端引导类，来引导绑定和启动服务器；
        ServerBootstrap serverBootstrap = new ServerBootstrap();
        //装配ServerBootstrap
        serverBootstrap.group(bossGroup, workerGroup)//多线程处理
                //制定通道类型为NioServerSocketChannel，一种异步模式的可以监听新进来的TCP连接的通道
                .channel(NioServerSocketChannel.class)
                //设置childHandler执行所有的连接请求
                //注册handler
                .childHandler(new ChannelInitializer<Channel>() {
   
                    @Override
                    protected void initChannel(Channel ch) throws Exception {
   
                        ch.pipeline().addLast(new ServerHandler());
                    }
                });

        serverBootstrap.bind(8000);
    }

}

public class ServerHandler extends ChannelInboundHandlerAdapter {
   

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
   
        System.out.println("server 读取数据......");
        //读取数据
        ByteBuf buf = (ByteBuf) msg;
        byte[] req = new byte[buf.readableBytes()];
        buf.readBytes(req);
        String body = new String(req, "UTF-8");
        System.out.println("接收客户端数据：" + body);
        //向客户端写数据
        System.out.println("server向client发送数据");
        String currentTime = new Date(System.currentTimeMillis()).toString();
        ByteBuf resp = Unpooled.copiedBuffer(currentTime.getBytes());
        ctx.write(resp);
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
   
        System.out.println("server 读取数据完毕...");
        ctx.flush();//刷新后才将数据发出到SocketChannel
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
   
        cause.printStackTrace();
        ctx.close();
    }

}
```

可以先尝试一下，后面会详细讲netty的原理

