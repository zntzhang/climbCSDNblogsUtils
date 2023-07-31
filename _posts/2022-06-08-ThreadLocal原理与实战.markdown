---
layout:  post
title:   ThreadLocal原理与实战
date:   2022-06-08 17:06:07
author:  'zhangtao'
image: '/img/post-bg-unix-linux.jpg'
catalog: [ WORK ]
tags:
- java
- java

---


 *ThreadLocal我会将其解释为线程上下文变量，当我们想要在方法间传递参数，又不想很挫的将每个方法都参数列表都加上这个参数时，可以使用它来帮助我们隐式传递参数* 


每个线程Thread都有一个ThreadLocalMap，ThreadLocal是这个Map的工具类。当我们通过ThreadLocal存放数据时，这个Map会添加一条记录。这条记录的key存放的是这个ThreadLocal的引用,value存的是缓存的数据（多个ThreadLocal存储不同的数据，这样这个Map就会有很多记录）


![img](https://img-blog.csdnimg.cn/6f174e215d2e4931b8ccd2a3823d8695.jpeg#pic_center)

我们来看下ThreadLocal的源码

```java
public class ThreadLocal<T> {
   
        // ThreadLocal#set
        public void set(T value) {
   
            Thread t = Thread.currentThread();
            // 获取当前线程的ThreadLocalMap
            ThreadLocalMap map = getMap(t);
            if (map != null)
                // 调用ThreadLocalMap#set,如果ThreadLocalMap不会空，则设置值，key为当前的ThreadLocal对象
                map.set(this, value);
            else
                // ThreadLocalMap为空，则初始化
                createMap(t, value);
        }

         // ThreadLocal#get
        public T get() {
   
            Thread t = Thread.currentThread();
            // 获取当前线程的ThreadLocalMap
            ThreadLocalMap map = getMap(t);
            if (map != null) {
   
                // 调用ThreadLocalMap#getEntry,this即当前的ThreadLocal对象，返回vlue
                ThreadLocalMap.Entry e = map.getEntry(this);
                if (e != null) {
   
                    @SuppressWarnings("unchecked")
                    T result = (T)e.value;
                    return result;
                }
            }
            // 如果获取不到值，会调用初始化方法
            return setInitialValue();
        }

         

        // ThreadLocalMap 每个Thread对象中都包含了一个ThreadLocalMap 
         static class ThreadLocalMap {
   

            // key是弱引用（指向当前ThreadLocal对象）如果这个ThreadLocal对象除了entry中的这个弱引用之外，没有其他强引用的话(e.g threadLocal == null)，
            // 这个ThreadLocal对象仍然可以回收，避免了无法回收。回收之后这个entry中的key为null,后续通过调用get/remove方法清除entry
			// 但是基本上我们threadLocal都是静态变量修饰的，不会出现没有其他引用的情况，所以很鸡肋
            static class Entry extends WeakReference<ThreadLocal<?>> {
   
                Object value;

                Entry(ThreadLocal<?> k, Object v) {
   
                    super(k);
                    value = v;
                }
            }

            // 数组
            private Entry[] table;

            // ThreadLocalMap#set
            private void set(ThreadLocal<?> key, Object value) {
   

                Entry[] tab = table;
                int len = tab.length;
                // 计算hash值取模，既数组中的位置    & (table.length - 1) 相当于 %table.length 
                int i = key.threadLocalHashCode & (len-1);

                for (Entry e = tab[i];
                    e != null; // for循环，当数组这个位置没有entry值时，跳出循环
                    e = tab[i = nextIndex(i, len)]) {
    // 如果有值，说明可能出现两种情况（1、与entry中的key相同，直接覆盖 2、不相同，说明hash冲突了，则继续遍历下一个位置）
                    ThreadLocal<?> k = e.get();
                    // entry中的key相同，直接覆盖
                    if (k == key) {
   
                        e.value = value;
                        return;
                    }

                    if (k == null) {
   
                        replaceStaleEntry(key, value, i);
                        return;
                    }
                }
                // 当数组这个位置没有entry值时,把key,value封装成entry放入数组中
                tab[i] = new Entry(key, value);
                // 数组size+1
                int sz = ++size;
                if (!cleanSomeSlots(i, sz) && sz >= threshold)
                    rehash();
            }

            // ThreadLocalMap#getEntry
            private Entry getEntry(ThreadLocal<?> key) {
   
                // 计算hash值取模，既数组中的位置
                int i = key.threadLocalHashCode & (table.length - 1);
                Entry e = table[i];
                // 如果这个键值对的key不等于空（没有被回收）,则返回这个键值对
                if (e != null && e.get() == key)
                    return e;
                else
                    // key等于空(key被回收了)则会对entry进行回收
                    return getEntryAfterMiss(key, i, e);
            }
         }
    }
```


- 内存泄漏 使用线程池时，线程不会销毁，threadLocal中的值仍然存在 
- 数据污染 使用线程池时，这个线程被重复利用。上一次的值没有清，导致这一次会取到上一次的结果 
- 不支持父子线程之间使用


- 使用完调用remove方法，且尽量在try-finally块中回收 
- 支持父子线程的ThreadLocal InheritableThreadLocal 阿里ttl


- 用户信息 
- 链路id 
- 业务方法上下文共用的一些参数


下面我记录下我使用threadLocal用过的场景和碰到过的坑

##### 接口优化

我在公司项目中用threadLocal做过一个优化，在优化后置打印订单这个接口时发现，整个接口上下文有多次重复根据id去数据库查询订单的代码，于是我使用threadLocal缓存订单数据进行复用,跨方法进行传递这些对象会比较方便

我是这么设计的，首先我们来定义这个ThreadLocal对象具体存放的值，理论上这个值就是我们需要缓存的订单，但是为了避免出现ThreadLocal串数据的情况（双保险，为了避免没有remove的情况），我定义了一个map，key是订单的id。后续从threadlocal中拿订单时，是根据id来捞，如果串数据了，那么查到的数据则为空（空的话需要从数据库查一次），这样就能尽量避免使用threadLocal产生的风险。

```java
// 伪代码

private static final ThreadLocal<TradePostPrintContext> TRADE_POST_PRINT_CONTEXT = new ThreadLocal<>();

class TradePostPrintContext implements Serializable {
   

    /**
    * 订单
    */
    public Map<Long, Trade> sid2TradeMap;


}

main() {
   
    try {
   
        method1();
        ...
        ...
        method2();
    } finally {
   
        TRADE_POST_PRINT_CONTEXT.remove();
    }
}

method1() {
   
    Map<Long, Trade> sid2TradeMap = new HashMap();
    sid2TradeMap.put(1, new Trade())
    TRADE_POST_PRINT_CONTEXT.set(new TradePostPrintContext(sid2TradeMap));
}

method2() {
   
    TradePostPrintContext context = TRADE_POST_PRINT_CONTEXT.get();
     Map<Long, Trade> sid2TradeMap = context.sid2TradeMap;
    Trade = sid2TradeMap.get(1) != null ? sid2TradeMap.get(1) : queryDb();
}
```

##### mybatisPlus的pageHelper踩坑

首先碰到的坑是这样的，首先代码执行了一个查询数据库的方法，但是这个方法是不分页的，方法里并没有进行分页（pageHelper）的配置。执行到这个方法的时却无缘无故调用了分页的相关sql。 然后我开始观察PageHelper的源码发现，分页配置是放在ThreadLocal中的。于是我猜测是threadLocal使用完没有remove导致的。后面我看到当查询执行完毕，会调用销毁ThreadLocal的方法，照理说不会出现问题，但是会出现一种场景，就是当我们他通过pageHelper设置完page后，没有调用查询方法，方法直接结束了，这样ThreadLocal就不会进入销毁的逻辑。后面这个线程被重复利用。因为上一次分页没有清，导致这一次原本没有分页的查询会进行分页

```java
protected static final ThreadLocal<Page> LOCAL_PAGE = new ThreadLocal<Page>();



PageHelper.setPage(1,10);
if (param != null){
   
    list = userMapper.selectIf(param)
} else {
   
    // param为空，没有进行查询，分页信息不会清除
    list = new ArrayList<User>();
}
```

