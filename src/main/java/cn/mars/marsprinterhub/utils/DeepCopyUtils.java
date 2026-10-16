package cn.mars.marsprinterhub.utils;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;

public class DeepCopyUtils {
    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static <T> T deepCopy(T src, Class<T> clazz) {
        try {
            String json = objectMapper.writeValueAsString(src);
            return objectMapper.readValue(json, clazz);
        } catch (IOException e) {
            throw new RuntimeException("Deep copy failed", e);
        }
    }
}