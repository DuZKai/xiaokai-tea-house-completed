# 自定义派生@Cacheable注解实现原理
## 设置过期时间
- 首先自定义注解ExpirableCacheable，继承@Cacheable所有属性外，新增expiredTimeSecond缓存过期时间以及缓存自动刷新时间preLoadTimeSecond，指定了自定义的 cacheManager 和 keyGenerator
- 自定义缓存管理器CustomizedRedisCacheManager，继承RedisCacheManager
  - 重写loadCaches，用于加载所有缓存到List中
  - 重写createRedisCache，根据传入的 cacheConfig创建 RedisCache 对象，如果为空则使用默认的 defaultCacheConfiguration
- 在CacheExpireTimeInit使用afterSingletonsInstantiated在Spring Bean初始化完成后，使用反射遍历每个 Bean 的方法，查找是否存在 ExpirableCacheable 注解。 设置缓存过期时间，并重新初始化缓存管理器（主要为了设置之前未设置的过期时间）
- 配置CustomizedRedisAutoConfiguration配置类，使用将默认的缓存管理器改成自定义的缓存管理器。此时会使用之前在 CacheExpireTimeInit 中设置的过期时间。

# 设置缓存刷新
- 总体过程为：获取缓存时会检测是否需要刷新缓存，如果需要则异步刷新缓存
- 当再次接受到请求后先进入ExpirableCacheablePreLoadAspect，使用@Aspect注解，定义获取即将到期时间参数切面，反射获取目标方法信息，再构建缓存调用的元数据信息并发布缓存调用元数据信息
- 自定义缓存管理器，重写get方法，获取当前缓存调用的元数据信息
- 判断当前时间是否小于刷新时间，如果是的话，将缓存刷新任务提交到线程池中执行，并加锁
- 从自定义的 RedisCacheManager 中获取当前保存的 CachedInvocation 对象，封装反射调用先前存储方法调用相关的信息，例如方法的参数、结果
- 最后将新结果存入缓存