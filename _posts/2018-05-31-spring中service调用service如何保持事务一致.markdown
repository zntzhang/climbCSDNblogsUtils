---
layout:  post
title:   spring中service调用service如何保持事务一致
date:   2018-05-31 20:21:51
author:  'zhangtao'
image: '/img/post-bg-unix-linux.jpg'
catalog: [ WORK ]
tags:
- java

---


**在使用SPRING的事务控制时，事务一般都是加在SERVICE层的，这个时候如果一个SERVICE调用另一个SERVICE时如何保持事务一致？比如第二个SERVICE抛出了异常，第一个SERVICE回滚。**


这就要先介绍spring的7种类型的事务传播行为  ![img](https://img-blog.csdn.net/20180531200737238?watermark/2/text/aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L3FxXzM3MjIxOTkx/font/5a6L5L2T/fontsize/400/fill/I0JBQkFCMA==/dissolve/70)

其中我们要用到第一种，

```java
<tx:method name="add*" propagation="REQUIRED"/>
```

测试代码

```java
@Test  
public void testAddUser() throws Exception {  
    UserServiceImpl service = (UserServiceImpl) context.getBean("userServiceImpl", UserServiceImpl.class);  
    USER u = new USER();  
    u.setCreated("2015-05-05");  
    u.setCreator(123);  
    u.setName("test");  
    u.setPassword("test");  
    service.addUser(u);  
}
```

SERVICE层：

```java
public void addUser(USER user) throws Exception {  
        userDaoImpl.saveUser(user);  
        delByUsername(user.getName());  
    }  

    public void delByUsername(String name) throws Exception {  
//        String s = null;  
//        s.length();  
        throw new RuntimeException("runtime e");  
    }
```

让第二个SERVICE抛出运行时异常，测试会发现这个时候第一个SERVICE的事务也回滚了，USER没有插入数据库中。  在事务传播为propagation=”REQUIRED”的时候，如果SERVICE抛出运行时异常，则所有的SERVICE共享同一事务。  如果想要SERVICE抛出所有异常都能回滚，那么就要在propagation="REQUIRED"后面加上rollback-for="Exception"

那么如何在远程调用别人接口（例如dubbo调用）时，保持事务一致性呢？那么就要用到分布式事务了。

