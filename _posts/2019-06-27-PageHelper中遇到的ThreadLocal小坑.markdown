---
layout:  post
title:   PageHelper中遇到的ThreadLocal小坑
date:   2019-06-27 22:43:11
author:  'zhangtao'
image: '/img/post-bg-unix-linux.jpg'
catalog: [ WORK ]
tags:
- java
- 并发

---


最近写代码刚好碰到ThreadLocal的小坑，顺便学习了一波ThreadLocal，拿出来分享一下

ThreadLocal什么时候会出现线程不安全的情况呢？

我总结了两种情况

## 1.记录在 ThreadLocal 中的是一个线程共享的外部对象

 [https://www.cnblogs.com/qilong853/p/5982878.html](https://www.cnblogs.com/qilong853/p/5982878.html)

这边文章讲的很好,我就不复制黏贴了，**ThreadLocal中保存的是Object对象的一个引用，这样的话，当有其他线程对这个引用指向的对象做修改时，当前线程Thread对象中保存的值也会发生变化**

```java
public class ThreadLocalTest implements Runnable {
   
	    private Integer count = 0;
		// 一个普通的对象
	    private NumberClass numberClass = new NumberClass();
	    private ThreadLocal<NumberClass> threadLocal = new ThreadLocal();
	    @Override
	    public void run() {
   
	        numberClass.setNum(++count);
	        threadLocal.set(numberClass);
	        try {
   
	            Thread.sleep(1000);
	        } catch (InterruptedException e) {
   
	            e.printStackTrace();
	        }
	        System.out.println("[Thread-" + Thread.currentThread().getId() + "]"+threadLocal.get().getNum());
	
	    }
	}

	 public static void main(String[] args){
   
	        ExecutorService fixedThreadPool = Executors.newFixedThreadPool(5);
	       ThreadLocalTest thread = new ThreadLocalTest();
	        for (int i =0;i<5;i++ ){
   
	            fixedThreadPool.execute(thread);
	        }

	//                [Thread-12]5
	//                [Thread-15]5
	//                [Thread-14]5
	//                [Thread-11]5
	//                [Thread-13]5

	// 执行完5个线程中的threadLocal中的值相同
	
	    }
```

## 2.引入线程池，线程复用

**如果在一个线程被使用完准备回放到线程池中之前，我们没有对记录在数据库中的数据执行清理，那么这部分数据就会被下一个复用该线程的业务看到**

```java
public class ThreadLocalTest2 implements Runnable {
   
	//    private Integer count = 0;
	    private ThreadLocal<List> threadLocal = new ThreadLocal();
	    @Override
	    public void run() {
   
	//        Integer count = 0;
	        List<Long> list = threadLocal.get();
	        if (list == null){
   
	            list = new ArrayList<>();
	            System.out.println("[Thread-" + Thread.currentThread().getId() + "]  init threadLocal");
	        }
	        list.add((long) (1+Math.random()*(10)));
	        threadLocal.set(list);
	        List list2 = threadLocal.get();
	        System.out.println("[Thread-" + Thread.currentThread().getId() + "]"+list);
	
	    }
	}

	  public static void main(String[] args){
   
	        ExecutorService fixedThreadPool = Executors.newFixedThreadPool(2);
	        ThreadLocalTest2 thread = new ThreadLocalTest2();
	        for (int i =0;i<5;i++ ){
   
	            fixedThreadPool.execute(thread);
	        }
	
	// 执行结果
	//                [Thread-11]  init threadLocal
	//                [Thread-12]  init threadLocal
	//                [Thread-11][9]
	//                [Thread-12][5]
	//                [Thread-11][9, 1]
	//                [Thread-12][5, 9]
	//                [Thread-11][9, 1, 4]
	
	    }
```

## 3.自己踩到的坑

首先碰到的坑是这样的，首先这个selectList是mybatisPlus的方法，执行到这个方法的时无缘无故调用了分页的相关sql，而这个方法里并没有进行分页（pageHelper）的配置。当时我们就想到是线程串了，当前线程使用了其他线程的分页配置。但是我们通过观察PageHelper的源码发现，

```java
protected static final ThreadLocal<Page> LOCAL_PAGE = new ThreadLocal<Page>();
```

配置是放在ThreadLocal里面的，而且分页查询执行完毕，会调用销毁ThreadLocal的方法，不会出现第二种问题，

```java
public static void clearLocalPage() {
   
            LOCAL_PAGE.remove();
    }
```

并且存的Page对象是new的变量，也不会出现第一种问题。 正常情况不会出现线程不安全的情况。

```java
public static <E> Page<E> startPage(int pageNum, int pageSize, boolean count, Boolean reasonable, Boolean pageSizeZero) {
   
	      Page<E> page = new Page<E>(pageNum, pageSize, count);
        	 ...
    }
```


坑在这里 ![img](https://img-blog.csdnimg.cn/20190627224218738.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L3FxXzM3MjIxOTkx,size_16,color_FFFFFF,t_70) 写代码的时候一开始设置了pageHelper，后面进行了if语句，没有执行下面的sql，导致page信息没有被销毁，当这个线程被复用的时候，就会以为要分页了

