package cn.wj.spring.cashbook

import com.alicp.jetcache.anno.config.EnableMethodCache
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.builder.SpringApplicationBuilder
import org.springframework.boot.runApplication
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer

// 开启在方法上使用缓存注解
@EnableMethodCache(basePackages = ["com.daily.applet"])

@SpringBootApplication
class CashbookApplication : SpringBootServletInitializer() {
    override fun configure(builder: SpringApplicationBuilder): SpringApplicationBuilder {
        return builder.sources(CashbookApplication::class.java)
    }
}

fun main(args: Array<String>) {
    runApplication<CashbookApplication>(*args)
}
