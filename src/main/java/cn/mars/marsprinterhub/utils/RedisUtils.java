package cn.mars.marsprinterhub.utils;

import jakarta.annotation.Resource;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Redis工具类
 */
@Component
public class RedisUtils {

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    /**
     * 设置缓存
     *
     * @param key   键
     * @param value 值
     */
    public void set(String key, Object value) {
        redisTemplate.opsForValue().set(key, value);
    }

    /**
     * 设置缓存并设置过期时间
     *
     * @param key     键
     * @param value   值
     * @param timeout 过期时间（秒）
     */
    public void set(String key, Object value, long timeout) {
        redisTemplate.opsForValue().set(key, value, timeout, TimeUnit.SECONDS);
    }

    /**
     * 获取缓存
     *
     * @param key 键
     * @return 值
     */
    public Object get(String key) {
        ValueOperations<String, Object> valueOperations = redisTemplate.opsForValue();
        return valueOperations.get(key);
    }

    /**
     * 删除缓存
     *
     * @param key 键
     */
    public void delete(String key) {
        redisTemplate.delete(key);
    }

    /**
     * 判断key是否存在
     *
     * @param key 键
     * @return true存在 false不存在
     */
    public Boolean hasKey(String key) {
        return redisTemplate.hasKey(key);
    }

    /**
     * 设置过期时间
     *
     * @param key     键
     * @param timeout 过期时间（秒）
     * @return true成功 false失败
     */
    public Boolean expire(String key, long timeout) {
        return redisTemplate.expire(key, timeout, TimeUnit.SECONDS);
    }

    /**
     * 获取过期时间
     *
     * @param key 键
     * @return 时间（秒）返回0代表永久有效
     */
    public Long getExpire(String key) {
        return redisTemplate.getExpire(key, TimeUnit.SECONDS);
    }

    /**
     * 递增
     *
     * @param key   键
     * @param delta 递增因子
     * @return 递增后的值
     */
    public Long increment(String key, long delta) {
        return redisTemplate.opsForValue().increment(key, delta);
    }

    /**
     * 递减
     *
     * @param key   键
     * @param delta 递减因子
     * @return 递减后的值
     */
    public Long decrement(String key, long delta) {
        return redisTemplate.opsForValue().decrement(key, delta);
    }

    /**
     * 获取匹配模式的所有key
     *
     * @param pattern 匹配模式
     * @return 匹配的key集合
     */
    public Set<String> keys(String pattern) {
        return redisTemplate.keys(pattern);
    }

    // ==================== 列表操作 ====================

    /**
     * 向列表左侧插入元素
     */
    public void lPush(String key, Object value) {
        redisTemplate.opsForList().leftPush(key, value);
    }

    /**
     * 向列表右侧插入元素
     */
    public void rPush(String key, Object value) {
        redisTemplate.opsForList().rightPush(key, value);
    }

    /**
     * 获取列表范围
     */
    public List<Object> lRange(String key, long start, long end) {
        return redisTemplate.opsForList().range(key, start, end);
    }

    /**
     * 获取列表长度
     */
    public Long lLen(String key) {
        return redisTemplate.opsForList().size(key);
    }

    // ==================== Set 操作 ====================

    /**
     * 添加 Set 值
     */
    public void sSet(String key, Object... values) {
        redisTemplate.opsForSet().add(key, values);
    }

    /**
     * 获取 Set 值
     */
    public Set<Object> sGet(String key) {
        return redisTemplate.opsForSet().members(key);
    }

    // ===================== ZSet 操作 ====================

    /**
     * 添加元素到 ZSet
     *
     * @param key   Redis key
     * @param value 元素
     * @param score 分数，用于排序
     */
    public void zSet(String key, Object value, double score) {
        if (key == null || value == null) return;
        redisTemplate.opsForZSet().add(key, value, score);
    }

    public void zSet(String key, Object value) {
        this.zSet(key, value, System.currentTimeMillis());
    }

    /**
     * 获取 ZSet 所有元素（按 score 从小到大）
     *
     * @param key Redis key
     * @return 元素集合
     */
    public Set<Object> zGetAll(String key) {
        if (key == null) return Collections.emptySet();
        Set<Object> members = redisTemplate.opsForZSet().range(key, 0, -1);
        return members != null ? members : Collections.emptySet();
    }

    /**
     * 获取 ZSet 元素（按 score 从大到小）
     */
    public Set<Object> zReverseRange(String key, long start, long end) {
        if (key == null) return Collections.emptySet();
        Set<Object> members = redisTemplate.opsForZSet().reverseRange(key, start, end);
        return members != null ? members : Collections.emptySet();
    }

    /**
     * 删除 ZSet 中的元素
     */
    public Long zRemove(String key, Object... values) {
        if (key == null || values == null || values.length == 0) return 0L;
        Long removed = redisTemplate.opsForZSet().remove(key, values);
        return removed != null ? removed : 0L;
    }

    /**
     * 按排名删除 ZSet 元素（score 从小到大）
     */
    public Long zRemoveRange(String key, long start, long end) {
        if (key == null) return 0L;
        Long removed = redisTemplate.opsForZSet().removeRange(key, start, end);
        return removed != null ? removed : 0L;
    }

    // ==================== Hash 操作 ====================

    /**
     * 设置 Hash 值
     */
    public void hSet(String key, String field, Object value) {
        redisTemplate.opsForHash().put(key, field, value);
    }

    /**
     * 获取 Hash 值
     */
    public Object hGet(String key, String field) {
        return redisTemplate.opsForHash().get(key, field);
    }

    /**
     * 获取整个 Hash
     */
    public Map<Object, Object> hGetAll(String key) {
        return redisTemplate.opsForHash().entries(key);
    }

    /**
     * 删除 Hash 中的字段
     */
    public void hDel(String key, Object... fields) {
        redisTemplate.opsForHash().delete(key, fields);
    }

    /**
     * 判断 Hash 中是否存在某个字段
     */
    public boolean hExists(String key, String field) {
        return redisTemplate.opsForHash().hasKey(key, field);
    }

    /**
     * 转化为long
     */
    public Long getLong(String key) {
        Object obj = get(key);
        return obj != null ? Long.valueOf(obj.toString()) : null;
    }
}
