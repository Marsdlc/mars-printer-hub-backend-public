package cn.mars.marsprinterhub.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

/**
 * 字段匹配校验注解
 * 用于校验两个字段的值是否相等
 * 
 * 使用示例：
 * @FieldMatch(first = "newPassword", second = "confirmPassword", message = "两次输入的密码不一致")
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = FieldMatchValidator.class)
@Documented
public @interface FieldMatch {
    
    /**
     * 错误消息
     */
    String message() default "字段值不匹配";
    
    /**
     * 分组
     */
    Class<?>[] groups() default {};
    
    /**
     * 负载
     */
    Class<? extends Payload>[] payload() default {};
    
    /**
     * 第一个字段名
     */
    String first();
    
    /**
     * 第二个字段名
     */
    String second();
    
    /**
     * 支持多个 @FieldMatch 注解
     */
    @Target({ElementType.TYPE})
    @Retention(RetentionPolicy.RUNTIME)
    @Documented
    @interface List {
        FieldMatch[] value();
    }
}
