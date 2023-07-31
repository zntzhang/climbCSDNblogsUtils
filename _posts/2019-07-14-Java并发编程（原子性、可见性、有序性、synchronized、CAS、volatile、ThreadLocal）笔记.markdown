---
layout:  post
title:   Java并发编程（原子性、可见性、有序性、synchronized、CAS、volatile、ThreadLocal）笔记
date:   2019-07-14 18:02:13
author:  'zhangtao'
image: '/img/post-bg-unix-linux.jpg'
catalog: [ WORK ]
tags:
- 并发
- java
- 锁
- 并发
- 多线程
- Java

---


 *自己网上总结了一些多线程并发的一些文章，如有错误请指教！* 

## 多线程的三大特性

#### 一、原子性

原子是世界上的最小单位，具有不可分割性。比如 a=0；（a非long和double类型） 这个操作是不可分割的，那么我们说这个操作时原子操作。

##### 线程切换带来的原子性问题

Java中的一条语句，在翻译为机器码之后，可能对应的是多个指令。

比如：**i++**这个操作至少需要3条指令；

- 把 i 的值从内存=加载到寄存器； 
- 执行+1操作； 
- 把值写入内存；

假如 i=0，两个线程同时执行该操作，可能线程1执行完第一步，就切换到线程2执行，本来两个线程各执行一次后 i 的值应该为 2 ，此时就出现 两次递增操作后值为 1 的现象；

在 Java 中 **synchronized** 和在 **lock、unlock** 和一些concurrent包下提供了一些**原子类**（AtomicInteger、AtomicLong、AtomicReference等）中操作保证原子性。

#### 二、可见性


JMM规定多线程之间的共享变量存储在主存中，每个线程单独拥有一个本地内存（逻辑概念），本地内存存储线程操作的共享变量副本； ![img](https://img-blog.csdnimg.cn/20190714173838746.jpeg?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L3FxXzM3MjIxOTkx,size_16,color_FFFFFF,t_70) 可见性，是指线程之间的可见性，一个线程修改的状态对另一个线程是可见的。也就是一个线程修改的结果。另一个线程马上就能看到。当线程1对共享变量A进行修改之后，线程2的工作内存中A可能还不是最新的值。这时候线程1的操作对线程2就不具有可见性。

##### 缓存导致的可见性问题

>Java内存模型规定所有的变量存储在主内存中。每个线程都有自己的工作内存，线程在工作内存中保存了使用到的主内存中变量的副本拷贝，线程对变量的操作必须在工作内存中进行，不能直接读写主内存中的变量。不同线程之间无法访问对方工作内存的变量。线程之间共享变量值的传递均需要通过主内存来完成。通常，<strong>我们无法确保执行读操作的线程能适时地看到其他线程写入的值</strong>，有时甚至是根本不可能的事情。为了确保多个线程之间对内存写入操作的可见性，必须使用同步机制。

在 Java 中 **volatile、synchronized 和 final** 实现可见性。

#### 三、有序性

Java程序中，如果在本线程中观察，所有的操作都是有序的；如果在另一个线程观察，所有的操作都是无序的。前半句指的是线程内表现为串行的语义，后半句指的是指令重排序和主内存和工作内存同步延迟的问题。

##### 编译优化带来的有序性问题

为了充分利用处理器的性能，处理器会对输入的代码进行乱序执行。在计算之后将乱序执行的结果重组，并保证该结果和顺序执行的结果一致，但是并不保证程序中各个语句的计算顺序和输入代码的顺序一致。Java虚拟机也有类似的指令重排序优化。

比如：Object obj = new Object()，

这条语句对应的指令为：

-  分配一块内存M；  
-  在M上初始化 Object 对象；  
-  将M的地址赋值给 obj； 

计算机经过优化后可能先执行第三步，再第二步，如果执行完第三步后切换到别的线程，若此时访问该变量则会发生空指针异常；

Java 语言提供了 volatile 和 synchronized 两个关键字来保证线程之间操作的有序性。


## 一、 如何保证操作的原子性

内置锁(同步关键字)：synchronized;

显示锁：Lock;

阻塞的策略，**性能不太好**，但是由于操作上的优势，只需要简单的声明一下即可，而且被它声明的代码块也是具有操作的原子性。 最后需要注意的是**synchronized是同步机制中最安全的一种方式**，其他的任何方式都是有风险的，当然付出的代价也是最大的。

自旋锁：CAS（ABA问题）；

Atomic

具体原理见  [并发编程之ThreadLocal、Volatile、Synchronized、Atomic关键字](https://www.jianshu.com/p/aaf3e933dbeb) 。

## 二、如何保证操作的可见性

#### 1.volatile

```java
class Example {
   
    private boolean stop = false;
    public void execute() {
   
        int i = 0;
        System.out.println("thread1 start loop.");
        while(!getStop()) {
   
            i++;
        }
        System.out.println("thread1 finish loop,i=" + i);
    }
    public boolean getStop() {
   
        return stop; // 对普通变量的读
    }
    public void setStop(boolean flag) {
   
        this.stop = flag; // 对普通变量的写
    }
}
public class VolatileExample {
   
    public static void main(String[] args) throws Exception {
   
        final Example example = new Example();
        Thread t1 = new Thread(new Runnable() {
   
            @Override
            public void run() {
   
                example.execute();
            }
        });
        t1.start();

        Thread.sleep(1000);
        System.out.println("主线程即将置stop值为true...");
        example.setStop(true);
        System.out.println("主线程已将stop值为：" + example.getStop());
        System.out.println("主线程等待线程1执行完...");

        t1.join();
        System.out.println("线程1已执行完毕，整个流程结束...");
    }
}
```



上面程序的意思是：让线程1先执行然后主（main）线程修改标志看是否能让子线程跳出循环。执行程序后发现程序并没有执行完，而是在等待线程1执行完毕。这就说明主线程修改stop变量并不对线程1可见，所以普通变量是不保证可见性的。 ![img](https://img-blog.csdnimg.cn/20190714170418260.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L3FxXzM3MjIxOTkx,size_16,color_FFFFFF,t_70) 当你把变量stop用volatile修饰时，主线程修改stop变量会立马对线程1可见并终止程序，这就证明volatile变量是具有可见性特性的。下面修改后的结果。 ![img](https://img-blog.csdnimg.cn/20190714170442897.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L3FxXzM3MjIxOTkx,size_16,color_FFFFFF,t_70) 当写一个 volatile 变量时，JMM 会把该线程对应的本地内存中的共享变量值刷新到主内存。

当读一个 volatile 变量时，JMM 会把该线程对应的本地内存置为无效。线程接下来将从主内存中读取共享变量。

以上面VolatileExample程序为例进行简单说明，当主线程对stop进行修改后且子线程尚未对stop进行读时，主线程已经把stop的值刷新到了主内存。其示意图如下：



![img](https://img-blog.csdnimg.cn/20190714170928127.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L3FxXzM3MjIxOTkx,size_16,color_FFFFFF,t_70) 当子线程进行读取时，会把本地内存置为无效直接去主内存中读取。（这里的主线程和子线程可以了解为两个普通线程没有父子关系）其示意图如下： ![img](https://img-blog.csdnimg.cn/20190714170942945.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L3FxXzM3MjIxOTkx,size_16,color_FFFFFF,t_70)

#### 2.synchronized（加各种锁）实现可见性

JMM关于synchronized的两条规定：

1）线程解锁前，必须把共享变量的最新值刷新到主内存中

2）线程加锁时，将清空工作内存中共享变量的值，从而使用共享变量时需要从主内存中重新获取最新的值

（注意：加锁与解锁需要是同一把锁） 通过以上两点，可以看到synchronized能够实现可见性。同时，由于synchronized具有同步锁，所以它也具有原子性

## 三、如何保证操作的有序性

#### 1.volatile 是因为其本身包含“禁止指令重排序”的语义

#### 2.synchronized 是由“一个变量在同一个时刻只允许一条线程对其进行 lock 操作”这条规则获得的，此规则决定了持有同一个对象锁的两个同步块只能串行执行。

## 四、ThreadLocal

ThreadLocal是一个工具类，它的作用是操作每个线程特有的一个ThreadLocalMap。 当我们把对象存到ThreadLocalMap，那么我们就可以在这个线程中不管在哪个方法都能获取到这个对象。

>ThreadLocal的设计，并不是解决资源共享的问题，而是用来提供线程内的局部变量，这样每个线程都自己管理自己的局部变量，别的线程操作的数据不会对我产生影响，互不影响，所以不存在解决资源共享这么一说，如果是解决资源共享，那么其它线程操作的结果必然我需要获取到，而ThreadLocal则是自己管理自己的，相当于封装在Thread内部了，供线程自己管理，这样做其实就是以空间换时间的方式(与synchronized相反)，以耗费内存为代价，单大大减少了线程同步(如synchronized)所带来性能消耗以及减少了线程并发控制的复杂度。

ThreadLocal实例通常来说都是private static类型的，用于关联线程和线程的上下文 一般使用ThreadLocal，官方建议我们定义为private static ，至于为什么要定义成静态的，这和内存泄露有关，以后再讨论。 它有三个暴露的方法，set、get、remove。

```java
public class TestThreadLocal {
   
	    private static final ThreadLocal<Integer> value = new ThreadLocal<Integer>() {
   
	        @Override
	        protected Integer initialValue() {
   
	            return 0;
	        }
	    };
	
	    public static void main(String[] args) {
   
	        for (int i = 0; i < 5; i++) {
   
	            new Thread(new MyThread(i)).start();
	        }
	    }
	
	    static class MyThread implements Runnable {
   
	        private int index;
	
	        public MyThread(int index) {
   
	            this.index = index;
	        }
	
	        public void run() {
   
	            System.out.println("线程" + index + "的初始value:" + value.get());
	            for (int i = 0; i < 10; i++) {
   
	                value.set(value.get() + i);
	            }
	            System.out.println("线程" + index + "的累加value:" + value.get());
	        }
	    }
	}
```

运行结果如下，这些ThreadLocal变量属于线程内部管理的，互不影响：

```java
线程0的初始value:0
线程3的初始value:0
线程2的初始value:0
线程2的累加value:45
线程1的初始value:0
线程3的累加value:45
线程0的累加value:45
线程1的累加value:45
线程4的初始value:0
线程4的累加value:45
```

我在ThreadLocal上遇到的坑  [ThreadLocal笔记](https://blog.csdn.net/qq_37221991/article/details/93922136)

