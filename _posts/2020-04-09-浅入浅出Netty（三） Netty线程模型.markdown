---
layout:  post
title:   浅入浅出Netty（三） Netty线程模型
date:   2020-04-09 22:33:20
author:  'zhangtao'
image: '/img/post-bg-unix-linux.jpg'
catalog: [ WORK ]
tags:
- netty

---


实际上Netty线程模型就是Reactor模式的一个实现，而Reactor模式又是什么呢？

## Reactor模型

Reactor模式是基于事件驱动开发的，核心组成部分包括Reactor和线程池，其中Reactor负责监听和分配事件，线程池负责处理事件，而根据Reactor的数量和线程池的数量，又将Reactor分为三种模型:

- 单线程模型 (单Reactor单线程) 
- 多线程模型 (单Reactor多线程) 
- 主从多线程模型 (多Reactor多线程)

### 单Reactor单线程模型


![img](https://imgconvert.csdnimg.cn/aHR0cHM6Ly9naXRlZS5jb20venQxOTk0MTIxNC9pbWFnZUJlZC9yYXcvbWFzdGVyL3VQaWMvbTBkQUVvLmpwZw?x-oss-process=image/format,png)

从图中可以看出：

它是由一个线程来接收客户端的连接，并将该请求分发到对应的事件处理 handler 中

这种模型好处是简单，坏处却很明显，当某个Handler阻塞时，会导致其他客户端的handler和accpetor都得不到执行，无法做到高性能，只适用于业务处理非常快速的场景

### 单Reactor多线程模型


![img](https://imgconvert.csdnimg.cn/aHR0cHM6Ly9naXRlZS5jb20venQxOTk0MTIxNC9pbWFnZUJlZC9yYXcvbWFzdGVyL3VQaWMvaFBwQldFLmpwZw?x-oss-process=image/format,png)

该模型在事件处理器（Handler）部分采用了多线程（线程池）

相对于第一种模型来说，在处理业务逻辑，也就是获取到IO的读写事件之后，交由线程池来处理，handler收到响应后通过send将响应结果返回给客户端。这样可以降低Reactor的性能开销，从而更专注的做事件分发工作了，提升整个应用的吞吐。

存在的问题： Reactor承担所有事件的监听和响应，只在主线程中运行，可能会存在性能问题。例如并发百万客户端连接，或者服务端需要对客户端握手进行安全认证，但是认证本身非常损耗性能。

于是又有了下面的线程模型。

### 主从Reactor多线程模型


![img](https://imgconvert.csdnimg.cn/aHR0cHM6Ly9naXRlZS5jb20venQxOTk0MTIxNC9pbWFnZUJlZC9yYXcvbWFzdGVyL3VQaWMvRms0MENwLmpwZw?x-oss-process=image/format,png) 比起第二种模型，它是将Reactor分成两部分：

1. mainReactor负责监听server socket，用来处理网络IO连接建立操作，将建立的socketChannel指定注册给subReactor。 
2. subReactor主要做和建立起来的socket做数据交互和事件业务处理操作。通常，subReactor个数上可与CPU个数等同。

## Netty的线程模型

通过配置boss和worker线程池的线程个数以及是否共享线程池等方式，Netty的线程模型可以在以上三种Reactor模型之间进行切换

```java
/**
 * 单Reactor单线程模型
 */
public class SingleReactorDemo {
   
    public static void main(String[] args) {
   
        NioEventLoopGroup group = new NioEventLoopGroup(1);
        ServerBootstrap serverBootstrap = new ServerBootstrap();
        serverBootstrap.group(group)//单线程处理
                .channel(NioServerSocketChannel.class)
                .childHandler(new ServerHandler());
        serverBootstrap.bind(8000);
    }
}
```

```java
/**
 * 单Reactor多线程模型
 */
public class SingleReactorMoreThreadDemo {
   
    public static void main(String[] args) {
   
        NioEventLoopGroup group = new NioEventLoopGroup(1);
        ServerBootstrap serverBootstrap = new ServerBootstrap();
        serverBootstrap.group(group)//单线程处理
                .channel(NioServerSocketChannel.class)
                .childHandler(new MoreThreadHandler());
        serverBootstrap.bind(8000);
    }
}

/**
 * 多线程Handler
 */
public class MoreThreadHandler extends ChannelInboundHandlerAdapter {
   

    private static ExecutorService executors = Executors.newScheduledThreadPool(200);

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
   
        //读取数据
        ByteBuf buf = (ByteBuf) msg;
        byte[] req = new byte[buf.readableBytes()];
        buf.readBytes(req);
        executors.submit(()->{
   
            //向客户端写数据
            String currentTime = new Date(System.currentTimeMillis()).toString();
            ByteBuf resp = Unpooled.copiedBuffer(currentTime.getBytes());
            ctx.write(resp);
            ctx.flush();//刷新后才将数据发出到SocketChannel
        });
    }
}
```

```java
/**
 * 主从Reactor多线程模型
 */
public class MoreReactorMoreThreadDemo {
    public static void main(String[] args) {
        // bossGroup表示监听端口，accept 新连接的线程组，workerGroup表示处理每一条连接的数据读写的线程组
        NioEventLoopGroup bossGroup = new NioEventLoopGroup();
        NioEventLoopGroup workerGroup = new NioEventLoopGroup();
        ServerBootstrap serverBootstrap = new ServerBootstrap();
        serverBootstrap.group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .childHandler(new MoreThreadHandler());
        serverBootstrap.bind(8000);
    }
}
```

