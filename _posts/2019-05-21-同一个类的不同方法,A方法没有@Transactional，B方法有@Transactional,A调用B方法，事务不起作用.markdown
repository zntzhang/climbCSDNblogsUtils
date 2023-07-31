---
layout:  post
title:   同一个类的不同方法,A方法没有@Transactional，B方法有@Transactional,A调用B方法，事务不起作用
date:   2019-05-21 16:09:36
author:  'zhangtao'
image: '/img/post-bg-unix-linux.jpg'
catalog: [ WORK ]
tags:
- java

---


## 问题：

同一个类的不同方法,A方法没有@Transactional，B方法有@Transactional,A调用B方法，事务不起作用

## 原理解析：

spring 在扫描bean的时候会扫描方法上是否包含@Transactional注解，如果包含，spring会为这个bean动态地生成一个子类（即代理类，proxy），代理类是继承原来那个bean的。 此时，当这个有注解的方法被调用的时候，实际上是由代理类来调用的，代理类在调用之前就会启动transaction。然而，如果这个有注解的方法是被同一个类中的其他方法调用的，那么该方法的调用并没有通过代理类，而是直接通过原来的那个bean，所以就不会启动transaction，我们看到的现象就是@Transactional注解无效。

```java
//接口
    interface Service {
   
        void A();
    
        void B();
    }
    
    //目标类，实现接口
    class ServiceImpl implements Service {
   
    
        //no annotation here
        @Override
        public void A() {
   
            this.B();
        }
        
		@Transactional
        @Override
        public void B() {
   
            System.out.println("execute doNeedTx in ServiceImpl");
        }
    }
    
    //代理类，也要实现相同的接口
    class ProxyByJdkDynamic implements Service {
   
    
        //包含目标对象
        private Service target;
    
        public ProxyByJdkDynamic(Service target) {
   
            this.target = target;
        }
    
        //目标类中此方法带注解，进行特殊处理
        @Override
        public void B() {
   
            //开启事务
            System.out.println("-> create Tx here in Proxy");
            //调用目标对象的方法，该方法已在事务中了
            target.B();
            //提交事务
            System.out.println("<- commit Tx here in Proxy");
        }
    
        //目标类中此方法没有注解，只做简单的调用
        @Override
        public void A() {
   
            //直接调用目标对象方法
            target.A();
        }
    }
```

那回到一开始的问题，我们调用的方法A不带注解，因此代理类不开事务，而是直接调用目标对象的方法。当进入目标对象的方法后，执行的上下文已经变成目标对象本身了，因为目标对象的代码是我们自己写的，和事务没有半毛钱关系，此时你再调用带注解的方法，照样没有事务，只是一个普通的方法调用而已。 **简单来说，内部调用本类方法，不会再走代理了，所以B的事务不起作用**

