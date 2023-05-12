博客地址: [SpringBoot配合Drools workbench(BusinessCentral)和maven配置实现动态规则更新](http://www.ltang.me/2021/09/08/springboot-with-drools-workbench/)

## 遇到的困境

如前面的博文[《Docker安装drools workbench和kie-server，使用http调用kie-server rest接口传json参数跑规则》](http://www.ltang.me/2020/10/06/install-drools-workbench-request-server-with-json/) 所述，折腾了老半天，装好了workbench，终于可以编辑规则了。也配置好了kie server，终于可以动态发布规则了。然而还是遇到点问题：


1. 引入一个kieServer又增加了开发和运维的负担，一个workbench已经够复杂了；
2. 使用Kie server提供的rest api，功能限制很多，比如我最需要的，过滤指定规则名, 就无法支持；
3. 若使用java api，规则名到是可以过滤了，那么就会遇到另一个问题：规则中使用到的数据对象，实际上是一个java bean, 在insert之前，是需要我们自己初始化的。比如，我们定义这么一个简单规则
```
import com.myspace.INPUTDATA;
import com.myspace.OUTPUTDATA;

//节点1
rule "DD_1"
	dialect "mvel"
	lock-on-active true
	when
		$p:INPUTDATA(currentNode=="1")
	$rsp: OUTPUTDATA()
	then
		$rsp.nodeCode = "2.1";
end
```
规则中，会对插入对象进行判断，那么我们要如何触发这个规则呢，如下：
```
com.myspace.INPUTDATA obj = new com.myspace.INPUTDATA();
com.myspace.OUTPUTDATA rsp = new com.myspace.OUTPUTDATA();
//...赋值
session.insert(obj);
session.insert(rsp);
// 执行规则
session.fireAllRules();
// session释放资源
session.dispose();
```
看到问题了吗，**我们在java代码里面，需要初始化数据对象**，而这个数据对象，在使用场景中，很可能是由业务人员在workbench中自己定义的，并且可能随时变化。

在网上大多数例子中，作者会告诉大家，需要在你的工程本地按照相同的包名、类名、属性新建一个对应的java类，只有这样才能正确地调用规则。

我也和部分使用者进行了讨论，并得出一个结论-**“数据对象应该是不易变化的，变化的是对数据对象进行判断和操作对规则”**。所以这种使用模式并不应该成为困扰。

但我并不满足，只怕业务也很难接受，他需要提前定义好可能用到的数据对象，若需要增改属性，还得重新发布服务。而在我们的实际使用场景中，属性的变化是比较频繁的。

## 解决的思路

再梳理一下，我们的需求其实很简单，就是监听workbench发布的新规则包（业务强烈要求可视化配置页面，否则workbench都可以省掉了），然后加载到内存，对外提供http json接口调用即可。至于额外的什么日志、负载...用SpringBoot不香吗？

于是打算这么做：

1. 使用SpringBoot搭建一个架子
2. 启动时通过配置文件去拉取指定的kjar文件(使用workbench发布)，并加载成规则包
3. 监听规则包变化，若有新的规则包，自动更新
4. 将json请求反射构造成数据对象，转为对drools的java api调用，再将规则执行结果序列化成json字符串返回调用端


## 关键的节点

### 拉取

怎么用SpringBoot搭个架子就不赘述了，我们需要做的是，启动的时候去拉取指定的kjar文件，并保持监听。

而这实际上是drools早就支持的功能，只需要简单几行代码如下:

```
KieServices kieServices = KieServices.Factory.get();

//url是在workbench上artifacts管理页面拷贝出来的jar包地址；
UrlResource resource = (UrlResource) kieServices.getResources().newUrlResource(url);

//访问workbench所需要的登录账户信息,实际上需要workbench的admin角色用户才有权限下载jar包
resource.setUsername(workBenchName);
resource.setPassword(workBenchPassword);
resource.setBasicAuthentication("enabled");

InputStream inputStream = resource.getInputStream();

KieRepository repository = kieServices.getRepository();
KieModule kieModule = repository.addKieModule(kieServices.getResources().newInputStreamResource(inputStream));
this.kieContainer = kieServices.newKieContainer(kieModule.getReleaseId());

this.kieBase = kieContainer.getKieBase();
```
其中url即为在workbench上发布的规则包下载地址。在上面的代码里面，启动时，会根据配置的URL去拉取jar包，并初始化kieContainer，后续对规则包的调用都可以通过kieBase进行。

### 监听

上述代码只是在启动时拉取指定url的jar包，那么如何自动更新呢，需要添加如下代码
```
 /**
  * 每45s 更新一次规则jar，保证jar为最新
  */
 this.kieScanner = kieServices.newKieScanner(kieContainer);
 kieScanner.start(45000L);
```
kieScanner是个异步线程，按照上边的配置，会每隔45秒去判断一下jar包是否有更新，如果有，会自动拉取并更新。

### 版本

这里需要注意的是，如果你想要你的规则jar包可以自动更新，那么需要在workbench上打包规则时设置版本为SNAPSHOT，与maven的机制是一样的，SNAPSHOT才会允许更新相同版本号的包，也才会自动去拉取，这个具体的可以学习maven的相关知识，不多解释。

### 反射

对于业务系统来说，它期望的是每次传入一个json格式的报文，由我们的规则服务跑业务人员（策略编辑人员）在workbench上编辑的规则，最后返回一个json格式的返回报文。

然而，如前文所说，使用java api调用drools时，是需要先实例化一个输入类对象的。而这个输入对象的类显然不能在我们自己的工程里面定义和获取，否则，每次变更业务参数，都需要同步变更我们的服务代码并发版本。很显然，我们只能通过规则jar包里面定义的“实体”来初始化我们想要的对象。

怕没有阐述清楚，再啰嗦一下整个过程：

1. 规则的编辑人员在workbench上定义一个fact（输入类），这个类里面定义了业务规则需要用到的字段属性；
2. 规则被打成一个jar包
3. 这个jar包被我们的scanner自动拉取并更新到jvm了
4. 业务系统接口调用规则服务，使用json报文阐述了它要输入的fact类型（全路径名）和字段名、字段值，并阐述了它期望返回的fact类型（全路径名）

那么，很显然，我们规则服务要做的，就是在获取到规则输入和输出对象的全路径名后，通过该规则包的类加载器反射构造这么一个对象，这样就可以 insert到kieSession里面并触发规则了，也可以通过接口和类信息从kieSession拿到返回的结果对象，再通过json序列化工具将结果对象转成json报文返回给调用端。

这里要说到反射生成输入和输出对象的两种方式。

### 方式1:
```
    /**
     * 根据包名和类名构建一个类
     *
     * @param base
     * @param packageName
     * @param className
     * @return
     */
    protected static FactType initfactType(KieBase base, String packageName, String className) {
        FactType factType = base.getFactType(packageName, className);
        return factType;
    }

    /**
     * 使用请求参数对实体赋值，目前仅支持简单Map结构
     *
     * @param factType
     * @param params
     * @return
     */
    protected static Object fillFactType(FactType factType, Map<String, Object> params) throws IllegalAccessException, InstantiationException {
        Object obj = factType.newInstance();
        factType.setFromMap(obj, params);
        return obj;
    }
```
这种方式使用了包名和类名实例化对象，看起来没有任何问题（实际上也不应该有问题）。但是在实际使用中遇到了个问题：使用FactType只能实例化在drools脚本中使用declare定义的类，比如：
```
declare Person
    name : String
    age : int
end
```
对于直接在页面配置的“数据对象”（实际上就是一个java类）这种类是无法支持的，会直接抛异常。因此采用另一种反射api：

### 方式2
```
 Sting reqFactClassName = "com.*.*.Person";
 Class<?> reqClazz = ((KnowledgeBaseImpl) kieBase).getClassFieldAccessorCache().getClassLoader().loadClass(reqFactClassName);
 Object reqObj = params.toJavaObject(reqClazz);
 
 FactHandle reqFactHandler = session.insert(reqObj);
 ...
```
在上面的代码中，插入的请求参数全路径名可以从业务系统请求参数中获取（由业务在业务系统中配置），然后初始化这个对象，把请求的json参数赋值该对象，再插入kieSession，然后再fire触发规则，这时候可以使用drools中的各种规则过滤api了，如：
```
AgendaFilter nameFilter = new RuleNameStartsWithAgendaFilter(ruleName);
int times = session.fireAllRules(nameFilter);
```

最后，如果你也期望从规则执行结果中取出对应的返回实体，可以参考如下代码：
```
String rspFactClassName = "com.*.*.RuleResult";
rspClazz = ((KnowledgeBaseImpl) kieBase).getClassFieldAccessorCache().getClassLoader().loadClass(rspFactClassName);
Collection<?> results = session.getObjects(new ClassObjectFilter(rspClazz));
if (results != null && results.size() > 0) {
    for (Object result : results) {
        logger.info("返回体：" + JSON.toJSONString(result));
    }
    return results.toArray()[0];
} else {
    ...
}
```
在上述代码中，返回的实体在执行完规则后，从kieSession中获取，最后转为json格式返回给业务系统。

## 坑爹的细节

### 没有外网的环境下部署
### Maven配置导致的401错误

## 优化

## 尾声