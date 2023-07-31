---
layout:  post
title:   浅入浅出Netty（一）BIO与NIO
date:   2020-03-28 21:52:48
author:  'zhangtao'
image: '/img/post-bg-unix-linux.jpg'
catalog: [ WORK ]
tags:
- netty

---



在开始了解 Netty 是什么之前，我们先来回顾一下，如果我们需要实现一个客户端与服务端通信的程序，使用传统的Socket通信，应该如何来实现？

```java
public class BIOServer {
   

    public static void main(String[] args) throws Exception {
   

        //创建Socket服务，监听8000端口
        ServerSocket server = new ServerSocket(8000);
        System.out.println("服务端启动！");
        while (true) {
   
            //获取一个套接字(阻塞)
            final Socket socket = server.accept();
            System.out.println("出现一个新客户端！");
            //业务处理
            handle(socket);
        }
    }

    /**
     * 处理数据
     * @param socket
     * @throws IOException
     */
    private static void handle(Socket socket) throws IOException {
   
        byte[] bytes = new byte[1024];
        InputStream input = socket.getInputStream();

        int read = 0;
        while (read != -1) {
   
            //读取数据(阻塞)
            read = input.read(bytes);
            System.out.println(new String(bytes, 0, read));
        }
    }
}
```

这段代码上面有两个阻塞点，一个是server.accept()（等待客户端连接），一个是input.read(bytes) （等待客户端发送信息），如果客户端一直不发数据，那么线程就一直会阻塞在input.read(bytes)。此时在阻塞过程中，意味着这条线程是被这个Socket一直占用着的，其它的Socket不能进来

想要服务端处理多个客户端的信息，就需要为每一个客户端分配一个线程。下面我们修改一下服务端：

```java
public class BIOServerV2 {
   

    public static void main(String[] args) throws Exception {
   
        //创建一个缓存线程池
        ExecutorService newCachedThreadPool = Executors.newCachedThreadPool();

        //创建Socket服务，监听8000端口
        ServerSocket server = new ServerSocket(8001);
        System.out.println("服务端启动！");
        while (true) {
   
            //获取一个套接字(阻塞)
            final Socket socket = server.accept();
            System.out.println("出现一个新客户端！");
            //在线程池为新客户端开一个线程
            newCachedThreadPool.execute(() -> handle(socket));
        }
    }

    /**
     * 处理数据
     *
     * @param socket
     * @throws IOException
     */
    private static void handle(Socket socket) {
   
        try {
   
            byte[] bytes = new byte[1024];
            InputStream input = socket.getInputStream();

            int read = 0;
            while (read != -1) {
   
                //读取数据(阻塞)
                read = input.read(bytes);
                System.out.println(new String(bytes, 0, read));
            }
        } catch (IOException e) {
   
            e.printStackTrace();
        } finally {
   
            try {
   
                socket.close();
            } catch (IOException e) {
   
                e.printStackTrace();
            }
        }

    }

}
```

可以看到我们创建了一个缓存线程池，当服务端新连接了一个客户端的时候，就创建一个新的线程为客户端进行服务

上面的 demo，从服务端代码中我们可以看到，在传统的 IO 模型中，每个连接创建成功之后都需要一个线程来维护。因为目前我们每个客户端都为其分配了一个线程去运行，如果有一万个客户端进来，我们就要分配一万个线程给客户端使用，这样的资源消耗是十分巨大的。


于是JDK 在 1.4 之后提出了 NIO，在 NIO 模型中，一条连接来了之后，直接把这条连接注册到 **selector** 上，然后，通过检查这个 selector，就可以批量监测出有数据可读的连接，进而读取数据


![img](https://imgconvert.csdnimg.cn/aHR0cHM6Ly9naXRlZS5jb20venQxOTk0MTIxNC9pbWFnZUJlZC9yYXcvbWFzdGVyL3VQaWMvT0poWWpILnBuZw?x-oss-process=image/format,png)

另外IO 读写是面向流的，一次性只能从流中读取一个或者多个字节，并且读完之后流无法再读取，你需要自己缓存数据。 而 NIO 的读写是面向 Buffer 的，你可以随意读取里面任何一个字节数据，不需要你自己缓存数据，这一切只需要移动读写指针即可。

核心代码

```java
/**
     * 采用轮询的方式监听selector是否有需要处理的事件，如果有，则进行处理
     *
     * @throws IOException
     */
    public void listen() throws IOException {
   
        System.out.println("服务端启动成功！");
        //轮询访问selector
        while (true) {
   
            //当注册的事件到达时，方法返回；否则，该方法会一直阻塞
            selector.select();
            //获得selector中选中的项的迭代器，选中的项为注册的事件
            Iterator<SelectionKey> ite = this.selector.selectedKeys().iterator();
            while (ite.hasNext()) {
   
                SelectionKey key = ite.next();
                //删除已选的key，以防重复处理
                ite.remove();
                if (key.isAcceptable()) {
   //客户端请求连接事件
                    handlerAccept(key);
                } else if (key.isReadable()) {
   //获得了可读的事件
                    handlerRead(key);
                }
            }
        }
    }
```

对于传统IO和NIO，网上有一对图片表达的非常好：


![img](https://imgconvert.csdnimg.cn/aHR0cHM6Ly9naXRlZS5jb20venQxOTk0MTIxNC9pbWFnZUJlZC9yYXcvbWFzdGVyL3VQaWMvT1k0c0FlLnBuZw?x-oss-process=image/format,png) 我们的系统就相当于一个餐厅，大门相当于ServerSocket，客人相当于socket客户端，服务员相当于每个socket客户端的处理线程。当在多线程的情况下处理客户端的时候，就相当于餐厅每一个客人都配备了一个专门的服务员，这不管对系统还是餐厅，都是很大的开销。

而对于NIO：


![img](https://imgconvert.csdnimg.cn/aHR0cHM6Ly9naXRlZS5jb20venQxOTk0MTIxNC9pbWFnZUJlZC9yYXcvbWFzdGVyL3VQaWMvZTYxZnA2LnBuZw?x-oss-process=image/format,png)

这里也是将系统比喻为一个餐厅，大门相当于serverChannel.socket().bind(new InetSocketAddress(10010))，客人相当于SocketChannel客户端，服务员相当于线程和selector，只需要一个服务员就可以服务所有的客人了，这对于系统或餐厅来说都是一个低开销的事情。

下表总结了Java IO和NIO之间的主要区别：


