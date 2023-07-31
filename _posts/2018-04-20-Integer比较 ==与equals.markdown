---
layout:  post
title:   Integer比较 ==与equals
date:   2018-04-20 11:30:18
author:  'zhangtao'
image: '/img/post-bg-unix-linux.jpg'
catalog: [ WORK ]
tags:
- java

---


```java
class Test {
   

    public static void main(String[] args) {
   

        Integer i1 = new Integer(5);

        Integer i2 = new Integer(5);

        System.out.println(i1 == i2); //false (情况，即new的id，而不是=的id赋值)

         

        Integer i3 = 5;

        Integer i4 = 5;

        System.out.println(i3 == i4); //true

    }

}
```

所以判断Integer相等的值最好不要用==

下面为更为详细的解析：

```java
public static void main(String[] args) {
    
		// 实例一 
        Integer a1 = Integer.valueOf(60);
        Integer b1 = 60;    
        System.out.println("1:="+(a1 == b1));     
          
  		// 实例二
        Integer a2 = 60;    
        Integer b2 = 60;    
        System.out.println("2:="+(a2 == b2));    
          
  		// 实例三
        Integer a3 = new Integer(60);    
        Integer b3 = 60;    
        System.out.println("3:="+(a3 == b3));    
        
        // 实例四 
        Integer a4 = 129;    
        Integer b4 = 129;    
        System.out.println("4:="+(a4 == b4));    
    }
```

答案

1:=true 2:=true 3:=false 4:=false

上述代码的答案，涉及到自动装箱的原理。

Integer b3 = 60, 这一步会触发自动装箱，也就是Integer b3 = Integer.valueOf(60) 查看Integer装箱的源码会发现**Integer在装箱时会调用 IntegerCache**缓存池，

```java
public static Integer valueOf(String s) throws NumberFormatException {
   
        return Integer.valueOf(parseInt(s, 10));
    }

public static Integer valueOf(int i) {
   
        if (i >= IntegerCache.low && i <= IntegerCache.high)
            return IntegerCache.cache[i + (-IntegerCache.low)];
        return new Integer(i);
    }
```

而IntegerCache会在类加载时，把-128-127之间的数全部创建一遍对象，放到Cache中。

所以实例一和实例二先后两次其实都是从缓冲区取的，都是同一个对象。 而实例三中a3是自己new出来的，没有走装箱方法，所以跟b3不是一个对象。 实例四超出了缓冲区范围，所以也不相等。

而Integer重写了equals方法，比较的是Intege的值，所以建议以后Integer比较直接用equals比较，或者先intValue()转成int后用==比较，

