package cn.mars.marsprinterhub.config;

import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import redis.clients.jedis.DefaultJedisClientConfig;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import java.time.Duration;

@Configuration
public class Bucket4jConfig {

    @Bean(destroyMethod = "close")
    public JedisPool bucket4jJedisPool(RedisProperties properties) {
        Duration timeout = properties.getTimeout() == null ? Duration.ofSeconds(2) : properties.getTimeout();
        Duration connectTimeout = properties.getConnectTimeout() == null ? timeout : properties.getConnectTimeout();
        DefaultJedisClientConfig.Builder clientConfig = DefaultJedisClientConfig.builder()
                .database(properties.getDatabase())
                .connectionTimeoutMillis((int) connectTimeout.toMillis())
                .socketTimeoutMillis((int) timeout.toMillis())
                .ssl(properties.getSsl().isEnabled());

        if (StringUtils.hasText(properties.getUsername())) {
            clientConfig.user(properties.getUsername());
        }
        if (StringUtils.hasText(properties.getPassword())) {
            clientConfig.password(properties.getPassword());
        }
        if (StringUtils.hasText(properties.getClientName())) {
            clientConfig.clientName(properties.getClientName());
        }

        JedisPoolConfig poolConfig = new JedisPoolConfig();
        poolConfig.setJmxEnabled(false);

        return new JedisPool(
                poolConfig,
                new HostAndPort(properties.getHost(), properties.getPort()),
                clientConfig.build()
        );
    }
}
