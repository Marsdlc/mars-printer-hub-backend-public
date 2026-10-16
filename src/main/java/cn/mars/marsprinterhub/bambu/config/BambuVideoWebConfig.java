package cn.mars.marsprinterhub.bambu.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 对外暴露 HLS 资源目录
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class BambuVideoWebConfig implements WebMvcConfigurer {

    private final BambuVideoConfig videoConfig;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        if (registry.hasMappingForPattern("/hls/**")) {
            return;
        }

        Path rootPath = Paths.get(videoConfig.getHlsRootDir()).toAbsolutePath().normalize();
        String location = rootPath.toUri().toString();
        if (!StringUtils.hasText(location)) {
            log.warn("HLS 资源目录未配置，跳过静态资源映射");
            return;
        }

        registry.addResourceHandler("/hls/**")
                .addResourceLocations(location.endsWith("/") ? location : location + "/")
                .setCachePeriod(0);
    }
}
