//package cn.wj.spring.cashbook.config
//
//import org.springframework.cache.CacheManager
//import org.springframework.cache.annotation.EnableCaching
//import org.springframework.context.annotation.Bean
//import org.springframework.context.annotation.Configuration
//import org.springframework.data.redis.cache.RedisCacheConfiguration
//import org.springframework.data.redis.cache.RedisCacheManager
//import org.springframework.data.redis.connection.RedisConnectionFactory
//import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory
//import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer
//import org.springframework.data.redis.serializer.RedisSerializationContext
//
//@Configuration
//@EnableCaching
//class CacheConfig {
//
//    @Bean
//    fun redisConnectionFactory(): RedisConnectionFactory {
//        return LettuceConnectionFactory()
//    }
//
//    @Bean
//    fun cacheManager(): CacheManager {
//        val cacheConfig = RedisCacheConfiguration.defaultCacheConfig()
//            .disableCachingNullValues()
//            .serializeValuesWith(
//                RedisSerializationContext.SerializationPair.fromSerializer(
//                    GenericJackson2JsonRedisSerializer()
//                )
//            )
//        return RedisCacheManager.builder(redisConnectionFactory())
//            .cacheDefaults(cacheConfig)
//            .build()
//    }
//}