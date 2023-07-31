---
layout:  post
title:   Spring AOP控制事务
date:   2017-12-23 09:25:11
author:  'zhangtao'
image: '/img/post-bg-unix-linux.jpg'
catalog: [ WORK ]
tags:
- java
- spring
- aop
- 事务
- java
- 异常

---


## 一. spring事务管理的两种方式

## 1. 编程式事务

自己写事务处理的类，然后调用(较少使用)

## 2. 声明式事务

### 2.1 使用基于注解的AOP事务管理

@Transactional注解：

@Transactional注解的属性:

- propagation：指定事务定义中使用的传播 
- isolation：设定事务的隔离级别 
- timeout：指定事务的超市（秒） 
- readOnly：指定事务的超时 
- noRollbackFor：目标方法可抛出的异常所构成的数组，但通知仍会提交事务 
- rollbackFor：异常所构成的数组，如果目标方法抛出了这些异常，通知就会回滚事务

<tx:annotation-driven/> 标签是注解驱动的事务管理支持的核心。

### 2.2 使用XML AOP事务管理

<tx:advice/>标签，该标签会创建一个事务处理通知。

还可以通过<tx:attributes>标签定制<tx:advice>标签所创建的通知的行为。

<tx:method/>标签的属性：

- PROPAGATION_REQUIRED：支持当前事务，如果当前没有事务，就新建一个事务。这是最常见的选择。 
- PROPAGATION_SUPPORTS：支持当前事务，如果当前没有事务，就以非事务方式执行。 
- PROPAGATION_MANDATORY：支持当前事务，如果当前没有事务，就抛出异常。 
- PROPAGATION_REQUIRES_NEW：新建事务，如果当前存在事务，把当前事务挂起。 
- PROPAGATION_NOT_SUPPORTED：以非事务方式执行操作，如果当前存在事务，就把当前事务挂起。 
- PROPAGATION_NEVER：以非事务方式执行，如果当前存在事务，则抛出异常。 
- PROPAGATION_NESTED：支持当前事务，新增Savepoint点，与当前事务同步提交或回滚。

## 二. spring AOP异常捕获原理

被拦截的方法需显式抛出异常，并不能经任何处理，这样aop代理才能捕获到方法的异常，才能进行回滚.

## 1. 默认情况下aop只捕获runtimeexception的异常

(1). 在catch中最后加上throw new runtimeexcetpion（），这样程序异常时才能被aop捕获进而回滚.

(2). 在service层方法的catch语句中增加：TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();语句，**手动回滚(也可以不发生异常,手动回滚)**，这样上层就无需去处理异常.

## 2. 通过spring配置来捕获特定的异常并回滚

<tx:method name="upd*" propagation="REQUIRED" rollback for="**java.lang.Exception**"/> 这样就可以在service的方法中不使用try catch来捕获异常来控制事务回滚提交

