---
layout:  post
title:   OOM、CPU飙升、Java进程被杀 线上故障分析
date:   2020-07-27 09:55:11
author:  'zhangtao'
image: '/img/post-bg-unix-linux.jpg'
catalog: [ WORK ]
tags:
- linux

---



关于OOM出现的情况，一般可以猜想是内存泄露，或者是加载了过多class或者创建了过多对象，给JVM分配的内存不够导致

**dump分析**

1.  首先登陆机器 jmap -histo pid 可以快速查看jvm内存class使用情况  
2.  如果我们想继续查看这个对象具体是被谁引用的，那可以使用jmap -dump命令生成内存dump文件  
3.  通过工具MAT查看导致OOM的对象是否都是必要的，如果出现了异常的对象一般都是内存泄漏（比如出现了大量的我们自己工程的对象）  
4.  如果是内存泄漏，可进一步通过工具查看泄漏对象到GC Roots的引用链，找到泄漏对象是通过怎样的引用路径、与哪些GC Roots相关联，才导致垃圾收集器无法回收它们，根据引用链的信息，一般可以比较准确地定位到这些对象创建的位置，进而找出产生内存泄漏的代码的具体位置  
5.  都没有问题的话考虑调大JVM内存配置(-XMX 最大堆大小与-XMS 初始堆大小)。 

例子： 导出时，因为耗时过长，所以采取异步的方式，使用线程池管理的线程来处理list，list很大，然后方法结束时未清空list，导致线程被回收后对象无法被释放，内存泄漏


关于CPU 100%出现的情况，一般可以猜想是程序陷入死循环，或者是线程死锁，相互等待，导致假死状态，不停地消耗CPU，或者是不断地GC等原因

1.  首先用top查看占比最高的进程  
2.  top -Hp pid 查看进程中资源占用最高的线程  
3.  printf ‘%x\n’ [线程id] 将10进制的线程转换为16进制  
4.  jstack [进程pid] | grep [线程16进制值] 来查看线程栈信息 


Linux 内核有个机制叫OOM killer，该机制会监控那些占用内存过大，尤其是瞬间很快消耗大量内存的进程，为了防止内存耗尽而内核会把该进程杀掉。

因此，你发现java进程突然没了，大概率是被linux的OOM killer给干掉了。

执行dmesg |grep java 查看内核对进程做对操作

例子： 1、容器配置为1g1核。使用cglib产生大量的动态代理类，导致元空间增大，而jvm配置的元空间参数设置过大 (512m)，导致还没被jvm的垃圾回收，就已经被操作系统kill掉了。解决方法是调整jvm的元空间参数

2、**堆外内存**是相对于堆内内存的一个概念。堆内内存是由JVM所管控的Java进程内存，我们平时在Java中创建的对象都处于堆内内存中，并且它们遵循JVM的内存管理机制，JVM会采用垃圾回收机制统一管理它们的内存。那么堆外内存就是存在于JVM管控之外的一块内存区域，因此它是不受JVM的管控(但是还是在java进程内的)。 如果堆外内存过大，也会导致java进程被杀

 *参考资料：* 

 [记一次线上服务CPU 100%的处理过程](https://segmentfault.com/a/1190000023160245?utm_source=tag-newest)

