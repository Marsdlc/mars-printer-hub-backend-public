package cn.mars.marsprinterhub.utils;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;

public class SafeLongToStringSerializer extends JsonSerializer<Long> {

    private static final long MAX_SAFE_INTEGER = 9007199254740991L; // 2^53-1

    @Override
    public void serialize(Long value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        if (value == null) {
            gen.writeNull();
        } else if (Math.abs(value) > MAX_SAFE_INTEGER) {
            gen.writeString(value.toString()); // 超过 JS 安全整数范围，转为字符串
        } else {
            gen.writeNumber(value); // 安全范围内，直接输出数字
        }
    }
}