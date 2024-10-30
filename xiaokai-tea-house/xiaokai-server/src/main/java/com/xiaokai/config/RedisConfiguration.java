package com.xiaokai.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
@Slf4j
public class RedisConfiguration {

    @Bean
    public RedisTemplate redisTemplate(RedisConnectionFactory redisConnectionFactory){
        log.info("初始化Redis模板对象...");
        RedisTemplate redisTemplate = new RedisTemplate();
        // 设置连接工厂对象
        redisTemplate.setConnectionFactory(redisConnectionFactory);
        // 设置key的序列化器，使得在图形化界面中可以直观的看到key的值，如果不配置则为乱码
        redisTemplate.setKeySerializer(new StringRedisSerializer());
        return redisTemplate;
    }

    // TODO: 序列化Redis，报错，修改
    // @Bean
    // public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory){
    //     log.info("初始化Redis模板对象和JSON序列化工具...");
    //     // 创建RedisTemplate对象
    //     RedisTemplate<String, Object> template = new RedisTemplate<>();
    //     // 设置连接工厂
    //     template.setConnectionFactory(connectionFactory);
    //     // 创建JSON序列化工具
    //     GenericJackson2JsonRedisSerializer jsonRedisSerializer =
    //             new GenericJackson2JsonRedisSerializer();
    //     // 设置Key的序列化
    //     template.setKeySerializer(RedisSerializer.string());
    //     template.setHashKeySerializer(RedisSerializer.string());
    //     // 设置Value的序列化
    //     template.setValueSerializer(jsonRedisSerializer);
    //     template.setHashValueSerializer(jsonRedisSerializer);
    //     // 返回
    //     return template;
    // }

    // 暂未尝试
    /**
     * @param redisConnectionFactory：配置不同的客户端，这里注入的redis连接工厂不同： JedisConnectionFactory、LettuceConnectionFactory
     * @功能描述 ：配置Redis序列化，原因如下：
     * （1） StringRedisTemplate的序列化方式为字符串序列化，
     * RedisTemplate的序列化方式默为jdk序列化（实现Serializable接口）
     * （2） RedisTemplate的jdk序列化方式在Redis的客户端中为乱码，不方便查看，
     * 因此一般修改RedisTemplate的序列化为方式为JSON方式【建议使用GenericJackson2JsonRedisSerializer】
     */
    // @Bean
    // public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory redisConnectionFactory) {
    //     GenericJackson2JsonRedisSerializer genericJackson2JsonRedisSerializer = new GenericJackson2JsonRedisSerializer();
    //     RedisTemplate<String, Object> redisTemplate = new RedisTemplate<>();
    //     // key采用String的序列化方式
    //     redisTemplate.setKeySerializer(StringRedisSerializer.UTF_8);
    //     // value序列化方式采用jackson
    //     redisTemplate.setValueSerializer(genericJackson2JsonRedisSerializer);
    //     // hash的key也采用String的序列化方式
    //     redisTemplate.setHashKeySerializer(StringRedisSerializer.UTF_8);
    //     //hash的value序列化方式采用jackson
    //     redisTemplate.setHashValueSerializer(genericJackson2JsonRedisSerializer);
    //     redisTemplate.setConnectionFactory(redisConnectionFactory);
    //     return redisTemplate;
    // }
}
