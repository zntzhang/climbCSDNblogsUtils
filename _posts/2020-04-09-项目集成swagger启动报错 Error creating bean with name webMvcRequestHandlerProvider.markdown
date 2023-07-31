---
layout:  post
title:   项目集成swagger启动报错 Error creating bean with name webMvcRequestHandlerProvider
date:   2020-04-09 22:34:11
author:  'zhangtao'
image: '/img/post-bg-unix-linux.jpg'
catalog: [ WORK ]
tags:
- java

---


最近项目集成了swagger以后启动一直报错

```java
org.springframework.beans.factory.UnsatisfiedDependencyException: Error creating bean with name 'webMvcRequestHandlerProvider' defined in URL [jar:file:/Users/jasonfeng/.m2/repository/io/springfox/springfox-spring-web/2.2.2/springfox-spring-web-2.2.2.jar!/springfox/documentation/spring/web/plugins/WebMvcRequestHandlerProvider.class]: Unsatisfied dependency expressed through constructor argument with index 0 of type [java.util.List]: : No qualifying bean of type [org.springframework.web.servlet.mvc.method.RequestMappingInfoHandlerMapping] found for dependency [collection of org.springframework.web.servlet.mvc.method.RequestMappingInfoHandlerMapping]: expected at least 1 bean which qualifies as autowire candidate for this dependency. Dependency annotations: {}; nested exception is org.springframework.beans.factory.NoSuchBeanDefinitionException: No qualifying bean of type [org.springframework.web.servlet.mvc.method.RequestMappingInfoHandlerMapping] found for dependency [collection of org.springframework.web.servlet.mvc.method.RequestMappingInfoHandlerMapping]: expected at least 1 bean which qualifies as autowire candidate for this dependency. Dependency annotations: {}
```

网上研究了半天找到了解决方案。

首先要从spring的启动顺序讲起，spring是先加载applicationContext.xml然后再执行spring-servlet.xml（web.xml里配置）。

然后spring（applicationContext.xml）和spring-mvc（spring-servlet.xml）是两个容器。父容器（spring）读不到子容器的bean  [Spring 和SpringMVC 的父子容器关系](https://www.cnblogs.com/zyzcj/p/5286190.html)

而swagger的加载是在spring-servlet.xml配置加载的。如果我们在applicationContext.xml里也加载了swagger的话，那么会读不到一些配置在子容器的配置

例如我applicationContext.xml和spring-servlet.xml都组件扫描了swaggerConfig，

spring-servlet.xml

```java
<context:component-scan base-package="com.olymtech.fms.air.config.swagger"></context:component-scan>
```

applicationContext.xml

```java
<context:component-scan base-package="com.olymtech.fms"/>
```

**解决办法：**

applicationContext.xml去除扫描swaggerConfig

```java
<context:component-scan base-package="com.olymtech.fms">
        <context:exclude-filter type="assignable" expression="com.olymtech.fms.air.config.swagger.SwaggerConfig"/>
    </context:component-scan>
```

