---
layout:  post
title:   总结自己常用的JDK8写法（stream,function,optional）
date:   2020-07-27 09:56:44
author:  'zhangtao'
image: '/img/post-bg-unix-linux.jpg'
catalog: [ WORK ]
tags:
- java

---



特别要注意的是list集合，我们sql查询返回的list或者stream处理完的list都不可能是null，所以不需要判断是否为null

- 遍历列表处理

```java
Optional.ofNullable(list)
        .orElseGet(Array::new)
        .forEach(...);
```

- 获取对象属性

```java
String value = Optional.ofNullable(data)
                        .map(data::name)
                        .orElse("");
```


- 取列表中对象的某个属性组成新的列表

```java
list2 = list.stream().map(data::name).collect(Collectors.toList());
```

- 遍历列表，赋值

```java
list2 = list.stream()
            .map(data -> {
   
                data.setName("xx");
                return data;
            })
            .collect(Collectors.toList());
```

- 过滤，只要&gt;2的

```java
list2 = list.stream().filter(data->data.count>2).collect(Collectors.toList());
```

- list转map

```java
// 一对多。根据id为key，data为value
Map<Long, List<data>> map1 = list.stream()
                                 .collect(Collectors
                                 .groupingBy(data::getId));
// 一对多。根据id为key，data的name为value
Map<Long, List<String>> map1 = list.stream()
                                 .collect(Collectors
                                 .groupingBy(data::getId, Collectors.mapping(data::getName,Collectors.toList())));
                                 
// 一对一。根据id为key，data的name为value
Map<Long, String> mapLevel12 = list.stream()
                                    .collect(Collectors
                                    .toMap(data::getId, data::getName, (key1, key2) -> key2));
```

- list 属性拼接成string

```java
String collect = orders.stream()
                .map(Order::getOrderNo).collect(Collectors.joining("，"));
```

- parallelStream使用自定义fork/join池（默认是共用的线程池），分治思想

```java
ForkJoinPool forkJoinPool = new ForkJoinPool(2);
List<Long> longList = forkJoinPool.submit(() -> addLevel3ColumnVos.parallelStream()
                                            .map(ComPortletSetColumnReport::getComPortletSetId)
                                            .collect(Collectors.toList()))
                                            .join();
```


函数式编程跟lamdba搭配使用，在参数定义时，我们可以用函数定义，真正调用需要传参时，使用lamdba表达式来传入。

新版策略模式,利用map+函数式编程取代了大量if/else逻辑

```java
@Service
public class BizService {
   
    @Autowired
    private BizUnitService bizUnitService;

    private Map<String, Function<String, String>> checkResultDispatcherComX = new HashMap<>();

    /**
     * 初始化 业务逻辑分派Map 其中value 存放的是 lambda表达式
     */
    @PostConstruct
    public void checkResultDispatcherComXInit() {
   
        checkResultDispatcherComX.put("key_订单1", order -> bizUnitService.bizOne(order));
        checkResultDispatcherComX.put("key_订单1_订单2", order -> bizUnitService.bizTwo(order));
        checkResultDispatcherComX.put("key_订单1_订单2_订单3", order -> bizUnitService.bizThree(order));
    }
    
    public String getCheckResultComX(String order, int level) {
   
        //写一段生成key的逻辑：
        String ley = getDispatcherComXKey(order, level);

        Function<String, String> result = checkResultDispatcherComX.get(ley);
        if (result != null) {
   
            //执行这段表达式获得String类型的结果
            return result.apply(order);
        }
        return "不在处理的逻辑中返回业务错误";
    }
   
}

@Service
public class BizUnitService {
   

    public String bizOne(String order) {
   
        return order + "各种花式操作1";
    }
    public String bizTwo(String order) {
   
        return order + "各种花式操作2";
    }
    public String bizThree(String order) {
   
        return order + "各种花式操作3";
    }
}
```

