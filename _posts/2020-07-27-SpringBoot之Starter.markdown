---
layout:  post
title:   SpringBoot之Starter
date:   2020-07-27 09:57:14
author:  'zhangtao'
image: '/img/post-bg-unix-linux.jpg'
catalog: [ WORK ]
tags:
- java
- spring

---


 *SpringBoot之所以流行，是因为Spring Starter模式的提出。Spring Starter的出现，可以让模块开发更加独立化，相互间依赖更加松散以及可以更加方便地集成。* 


SpringBoot中的starter是一种非常重要的机制，能够抛弃以前繁杂的配置，将其统一集成进starter，应用者只需要在maven中引入starter依赖，SpringBoot就能自动扫描到要加载的信息并启动相应的默认配置，发现需要的Bean，并注册进IOC容器。

SpringBoot提供了针对日常企业应用研发各种场景的spring-boot-starter依赖模块


Starter极大的简化我们的工程配置，我们可以跟以前的SSM框架进行一个对比。

比如我们需要集成RocketMq框架，按照以前的做法我们需要做到下面几步。

- 引入相关的Maven依赖（可能不止一个依赖） 
- 在配置文件xml中，添加相关的bean以便加入容器中，并且配置mq需要的配置信息（如ip、端口、组名等）

并且，当我们重新搭建一个项目时，这些配置又得重新拷一遍过去。


当我们用了Spring Starter之后会有什么不同呢？

- 引入Mq的Starter依赖 
- 在配置文件里添加我们需要自定义的配置信息（有默认值）

跟上面比是不是简单了很多，Maven上就有现成的Starter我们可以直接使用，或者我们可以自定义一些Starter来给其他工程使用，非常的方便。


虽然不同的starter实现起来各有差异，但是他们基本上都会使用到两个相同的内容：

-  ConfigurationProperties 因为Spring Boot坚信约定大于配置这一理念，所以我们使用ConfigurationProperties来保存我们的配置，并且这些配置都可以有一个默认值，即在我们没有主动覆写原始配置的情况下，默认值就会生效，这在很多情况下是非常有用的。除此之外，starter的ConfigurationProperties还使得所有的配置属性被聚集到一个文件中（一般在resources目录下的application.properties），这样我们就告别了Spring项目中XML地狱  
-  AutoConfiguration AutoConfiguration中我们实现了自己要求：创建我们需要的bean，并且我们把properties中参数赋给了该bean。 


![img](https://imgconvert.csdnimg.cn/aHR0cHM6Ly9naXRlZS5jb20venQxOTk0MTIxNC9pbWFnZUJlZC9yYXcvbWFzdGVyL3VQaWMvZFcwVUs0LnBuZw?x-oss-process=image/format,png)

我们可以给任意一个现有的组件创建一个starter来让别人在使用这个组件的时候更加的简单方便。


比如我们要集成一个RocketMq框架，我们发现网上没有现成可用的Starter项目，所以我们就把这个封装成一个Starter项目，来给其他工程使用。

## a. 创建Starter工程，并且引入Maven依赖

```java
<!-- RocketMQ的dependency -->
<dependency>
	<groupId>org.apache.rocketmq</groupId>
	<artifactId>rocketmq-client</artifactId>
	<version>4.3.0</version>
</dependency>
```

## b. 创建一个ConfigurationProperties用于保存你的配置信息

```java
// 自动获取配置文件中前缀为rocketmq的属性，把值传入对象参数
@ConfigurationProperties("rocketmq")
@Data
public class RocketMqConfigProperties {
   
    
    // 消费者GROUP
    private String consumerGroup = "pushConsumer";// 如果配置文件中配置了下面属性，则该默认属性会被覆盖
    // NameServer 地址
    private String namesrvAddr = "127.0.0.1:9876";
    // topic
    private String topic = "default";
    // 生产者的组名
    private String producerGroup = "default";


}
```

## c. 创建一个AutoConfiguration，引用定义好的配置信息；在AutoConfiguration中实现所有starter应该完成的操作

```java
@Configuration
// 使用RocketMqConfigProperties配置
@EnableConfigurationProperties(RocketMqConfigProperties.class)
public class RocketMqConfig {
   

    @Autowired
    private RocketMqConfigProperties rocketMqConfigProperties;

    @Bean
    // 在该bean不存在的情况下此方法才会执行
    @ConditionalOnMissingBean
    DefaultMQPushConsumer getConsumer() {
   
        // 创建bean
        // 赋值...
        return consumer;
    }

    @Bean
    // 在该bean不存在的情况下此方法才会执行
    @ConditionalOnMissingBean
    MyMqProducer getProducer() {
   
        // 创建bean
        // 赋值...
        return producer;
    }
}
```

## d. 暴露Configuration

这里有两种方式，一种是自动暴露，当其他工程集成该Starter后，可以直接使用Starter里的bean；还有一种是手动暴露，我们需要添加注解才能使用Starter里的bean

### spring.factories 自动暴露

在 resources 文件夹下新建目录 META-INF，在目录中新建 spring.factories 文件，并且在 spring.factories 中配置AutoConfiguration：

```java
org.springframework.boot.autoconfigure.EnableAutoConfiguration=com.springboot.conf.RocketMqConfig
```

用到了SPI机制，SpringBoot的SpringFactoriesLoader会加载spring.factories文件来发现对应的Configuration这些类注入到 IOC 容器中。

使用这种方式，当其他工程集成该Starter后，可以直接使用Starter里的bean

### @Import 手动暴露

```java
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import(RocketMqConfig.class)
public @interface EnableRocketMq {
   
}
```

@Import注解用来导入一个或多个class，这些类会注入到spring容器中，或者配置类，配置类里面定义的bean都会被spring容器托管。

使用这种方式，当其他工程集成该Starter后，需要在启动时添加@EnableRocketMq，才能够使用Starter里的bean


1. Spring Boot在启动时扫描项目所依赖的JAR包， 
2. 寻找包含spring.factories文件的JAR包 
3. 根据spring.factories配置加载AutoConfigure类 
4. 自动配置并将Bean注入容器

 *代码地址：* 

https://github.com/zntzhang/rocketmq-spring-boot-starter.git

 *参考文章：* 

 [springboot之自定义starter](https://zhuanlan.zhihu.com/p/150839717)

 [springboot之自定义Enable注解](https://zhuanlan.zhihu.com/p/151410665)

 [Spring Boot Starters是什么？](https://www.cnblogs.com/tjudzj/p/8758391.html)

