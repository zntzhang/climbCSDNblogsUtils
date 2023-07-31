---
layout:  post
title:   简单介绍AQS、ReetrantLock、CountDownLatch、CyclicBarrier、Semaphore
date:   2020-07-04 15:29:16
author:  'zhangtao'
image: '/img/post-bg-unix-linux.jpg'
catalog: [ WORK ]
tags:
- 并发
- java

---


AQS，既AbstractQueuedSynchronizer, 是JUC包实现同步的基础工具，是一个抽象类。

在AQS中，定义了一个volatile int state变量作为共享资源，并且内置自旋锁实现的同步队列，封装入队和出队的操作，提供独占、共享、中断等特性的方法。

**如果线程获取资源失败，则进入同步FIFO（先进先出）队列中等待（入队）； 如果成功获取资源就执行临界区代码。 执行完释放资源时，通知队列中的等待线程来获取资源，然后出队。**

AQS的子类可以定义不停的资源实现不同性质的方法


可重入锁。定义state为0时可以获取资源并置为1.若已获得资源，通过CAS的方式state+1，在释放资源时state-1，直至为0


CountDownLatch也是juc包中的一个类，类似倒计时计数器，初始时定义了资源总量state=count，调用await()方法则处于等待状态。countDown()不断地将state-1，当state=0时才能获得锁，所有线程都不会等待。

使用场景： 将主线程阻塞，等异步的多线程全部执行完毕并返回结果后，再继续执行主线程。

```java
public static List<String> getExecutorService() throws InterruptedException{
   
		System.out.println("开始执行多线程...");
		long startTime = System.currentTimeMillis();
		List<String> list = new CopyOnWriteArrayList<>();//存放返回结果
		CountDownLatch countDownLatch = new CountDownLatch(10);
		ExecutorService executorService = Executors.newFixedThreadPool(10);
		for (int i = 0; i < 10; i++) {
   
			Runnable runnable = new Runnable(){
   
 
				@Override
				public void run() {
   
					 try {
   
						Thread.sleep(3000);
                        list.add(UUID.randomUUID().toString());
                        System.out.println("当前线程name : "+Thread.currentThread().getName());
                        countDownLatch.countDown();
					} catch (InterruptedException e) {
   
						e.printStackTrace();
					}
				}
				
			};
			executorService.execute(runnable);
		}
		countDownLatch.await();
		System.out.println("submit总共cost 时间：" + (System.currentTimeMillis()-startTime)/1000 + "秒");
		executorService.shutdown();
		return list;
	}
```

CountDownLatch是一次性的，state减到0后就释放锁，如果再想用就只能重新创建一个，如果希望循环使用，推荐使用**CyclicBarrier**.


信号量。与CountDownLatch不同的是，同样也是定义了资源总量state=permits，当state&gt;0时就能获得锁，并将state-1，当state=0时只能等待其他线程释放锁，当其他线程释放锁时state+1，这样等待的线程又能获得这个锁

当Semaphore的permits定义为1时，就是互斥锁，当permits&gt;1时就是共享锁

