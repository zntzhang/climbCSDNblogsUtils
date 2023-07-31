---
layout:  post
title:   单元测试--Mockito+JUnit
date:   2021-12-21 00:00:26
author:  'zhangtao'
image: '/img/post-bg-unix-linux.jpg'
catalog: [ WORK ]
tags:
- java
- 单元测试

---


最近领导开始要求写代码需要写单元测试了。于是花了点时间研究下单元测试。

## 一、目的

单元测试适合一些项目复杂，启动一次项目要花很大成本的项目，这样我们可以通过单元测试去很快的测试它，而不需要启动项目来调试。

## 二、测试范围

单元测试最适合测试一些util方法，以及一些判断逻辑。单元测试不适合对sql进行测试，因为这需要加载spring上下文，并产生测试数据，对数据库造成污染，这属于集成测试的范畴。如果碰到一些比较难写单测的方法，可能需要我们把方法进一步拆解后再进行测试。

## 三、怎么写单元测试

方法里每个if逻辑的正反两面都可以测试，以及一些边界值（测试覆盖率，测试用例对代码的覆盖程度）。同一个测试方法，可以通过传入参数的不同，测试不同的逻辑。

如果执行过程中遇到一些我们不关心的方法，但是需要依赖这些方法返回的结果，才能验证自己的代码逻辑。那么可以通过mock工具来返回结果。最后通过assert或者Verify来验证是否符合自己的想法

我们公司使用的是Mockito+JUnit这两个框架来构建测试用例的。

### 1、相关注解

- @RunWith(MockitoJUnitRunner.class)：测试类上的注解，启动注解 
- @InjectMocks：将mock对象注入 
- @Mock ：mock对象，打桩方法，返回打桩值。其余方法不调用，返回空 
- @Spy ：mock对象，打桩方法，返回打桩值。其余方法调用真实方法

### 2、打桩方法

- when…thenReturn 当调用指定方法时，返回指定结果 
- doAnswer…when 配合InvocationOnMock 当调用指定方法时，动态获取参数和结果

### 3、验证

- Assert 判断逻辑 
- Verify 验证是否调用某个方法（几次）

## 四、例子

```java
@RunWith(MockitoJUnitRunner.class)
public class Test {
   
    @InjectMocks
    @Spy
    OrderUniqueCodeService orderUniqueCodeService = new OrderUniqueCodeService();
    @Mock
    WaveUniqueCodeDao waveUniqueCodeDao;
    @Mock
    Staff staff;

 

    @Before
    public void setUp() {
   
    }

    /**
     * 测试大于
     */
    @Test
    public void testBuildCodes1() {
   
        List<WaveUniqueCode> codes = buildCodes(2, 1);
        assertEquals(1, codes.size());
    }

    /**
     * 测试等于
     */
    @Test
    public void testBuildCodes2() {
   
        List<WaveUniqueCode> codes = buildCodes(1, 1);
        assertEquals(0, codes.size());
    }

    /**
     * 测试小于
     */
    @Test
    public void testBuildCodes3() {
   
        List<WaveUniqueCode> codes = buildCodes(1, 2);
        assertEquals(0, codes.size());
    }

    /**
     * 测试invocationOnMock
     */
    @Test
    public void print() {
   
        List<WaveUniqueCode> codes = buildCodes(5, 2);
    
        List<WaveUniqueCode> updateCodes = Lists.newArrayList();
        doAnswer(invocationOnMock -> updateCodes.addAll((ArrayList<WaveUniqueCode>) invocationOnMock.getArguments()[1])).when(orderUniqueCodeService).uniqueCodePrintUpdate(any(), anyList(), anyList());
        orderUniqueCodeService.print(staff, codes);
        Assert.assertTrue(updateCodes.stream().map(WaveUniqueCode::getStatus).allMatch(data -> OrderUniqueCodeStatusEnum.WAIT_RECIVE.getType().equals(data)));
    }


    public List<WaveUniqueCode> buildCodes(Integer printNum, Integer countNum) {
   
      

        when(waveUniqueCodeDao.count(any(Staff.class), any(OrderUniqueCodeQueryParams.class))).thenReturn(Long.valueOf(countNum));
        
        return orderUniqueCodeService.buildCodes(staff);

    }
```

参考链接：  [https://blog.csdn.net/zhangxin09/article/details/42422643](https://blog.csdn.net/zhangxin09/article/details/42422643)

