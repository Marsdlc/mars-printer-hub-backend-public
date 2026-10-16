package cn.mars.marsprinterhub.utils;

import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

/**
 * 图片格式转换工具
 * 依赖系统 ffmpeg
 */
@Slf4j
public final class ImageConvertUtils {

    private ImageConvertUtils() {
    }

    public static File convertToWebp(File sourceFile) {
        return convert(sourceFile, ".webp");
    }

    public static File writeBytesToTempFile(byte[] bytes, String suffix) {
        if (bytes == null || bytes.length == 0) {
            return null;
        }
        try {
            Path tempFile = Files.createTempFile("mars-image-", normalizeSuffix(suffix));
            Files.write(tempFile, bytes);
            return tempFile.toFile();
        } catch (IOException e) {
            log.error("写入临时图片文件失败", e);
            return null;
        }
    }

    public static void deleteQuietly(File file) {
        if (file != null && file.exists() && !file.delete()) {
            log.warn("临时文件删除失败: {}", file.getAbsolutePath());
        }
    }

    private static File convert(File sourceFile, String targetSuffix) {
        if (sourceFile == null || !sourceFile.exists()) {
            return null;
        }
        Path targetPath = null;
        try {
            targetPath = Files.createTempFile("mars-image-", targetSuffix);
            ProcessBuilder processBuilder = new ProcessBuilder(
                    "ffmpeg",
                    "-y",
                    "-i", sourceFile.getAbsolutePath(),
                    targetPath.toString()
            );
            processBuilder.redirectErrorStream(true);
            Process process = processBuilder.start();
            if (!process.waitFor(20, TimeUnit.SECONDS)) {
                process.destroyForcibly();
                log.error("ffmpeg 转换超时: {}", sourceFile.getAbsolutePath());
                deleteIfExists(targetPath);
                return null;
            }
            File targetFile = targetPath.toFile();
            if (!targetFile.exists() || targetFile.length() == 0) {
                log.error("ffmpeg 未生成有效图片: {}", sourceFile.getAbsolutePath());
                deleteIfExists(targetPath);
                return null;
            }
            return targetFile;
        } catch (Exception e) {
            log.error("转换图片失败: {}", sourceFile.getAbsolutePath(), e);
            deleteIfExists(targetPath);
            return null;
        }
    }

    private static String normalizeSuffix(String suffix) {
        if (suffix == null || suffix.isBlank()) {
            return ".img";
        }
        return suffix.startsWith(".") ? suffix : "." + suffix;
    }

    private static void deleteIfExists(Path path) {
        if (path == null) {
            return;
        }
        try {
            Files.deleteIfExists(path);
        } catch (IOException e) {
            log.warn("删除临时文件失败: {}", path, e);
        }
    }
}
