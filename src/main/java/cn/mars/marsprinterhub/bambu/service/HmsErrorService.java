package cn.mars.marsprinterhub.bambu.service;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.resource.ResourceUtil;
import cn.hutool.core.util.StrUtil;
import cn.mars.marsprinterhub.bambu.model.HmsError;
import cn.mars.marsprinterhub.exception.BusinessException;
import cn.mars.marsprinterhub.exception.ErrorCode;
import cn.mars.marsprinterhub.model.enums.PrinterModelEnum;
import cn.mars.marsprinterhub.model.vo.HmsErrorVO;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * HMS 错误码解析服务。
 */
@Service
@Slf4j
public class HmsErrorService {

    private static final Map<Integer, String> HMS_SEVERITY_LEVELS = Map.of(
            1, "fatal",
            2, "serious",
            3, "common",
            4, "info"
    );
    private static final Map<Integer, String> HMS_MODULES = Map.of(
            0x05, "mainboard",
            0x0C, "xcam",
            0x07, "ams",
            0x08, "toolhead",
            0x03, "mc"
    );

    private final ObjectMapper objectMapper;
    private Map<String, Map<String, List<String>>> deviceHmsMessages = Collections.emptyMap();

    public HmsErrorService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    public void init() {
        loadHmsData();
    }

    public List<HmsErrorVO> resolveErrors(List<HmsError> errors, String printerModel) {
        if (CollUtil.isEmpty(errors)) {
            return Collections.emptyList();
        }

        String model = normalizeModel(printerModel);
        return errors.stream()
                .map(error -> buildHmsErrorVO(error, model))
                .filter(java.util.Objects::nonNull)
                .toList();
    }

    private void loadHmsData() {
        try {
            String json = ResourceUtil.readUtf8Str("hms_zh_cn.json");
            Map<String, Map<String, Map<String, List<String>>>> data = objectMapper.readValue(
                    json,
                    new TypeReference<>() {
                    }
            );
            deviceHmsMessages = data.getOrDefault("device_hms", Collections.emptyMap());
        } catch (Exception e) {
            log.error("加载 HMS 错误码数据失败", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "HMS 错误码数据加载失败: " + e.getMessage());
        }
    }

    private HmsErrorVO buildHmsErrorVO(HmsError error, String printerModel) {
        if (error == null || error.getAttr() == null || error.getCode() == null) {
            return null;
        }

        long attr = error.getAttr();
        long rawCode = error.getCode();
        if (attr <= 0 || rawCode <= 0) {
            return null;
        }

        String formattedCode = formatHmsCode(attr, rawCode);
        String message = lookupMessage(formattedCode, printerModel);
        if (StrUtil.isBlank(message)) {
            return null;
        }

        HmsErrorVO vo = new HmsErrorVO();
        vo.setCode("HMS_" + formattedCode);
        vo.setMessage(message);
        vo.setSeverity(resolveSeverity(rawCode));
        vo.setModule(resolveModule(attr));
        return vo;
    }

    private String formatHmsCode(long attr, long rawCode) {
        return String.format(
                "%04X_%04X_%04X_%04X",
                attr >> 16,
                attr & 0xFFFF,
                rawCode >> 16,
                rawCode & 0xFFFF
        );
    }

    private String lookupMessage(String formattedCode, String printerModel) {
        Map<String, List<String>> codeEntry = deviceHmsMessages.get(formattedCode.replace("_", ""));
        if (codeEntry == null || codeEntry.isEmpty()) {
            return null;
        }

        if (StrUtil.isNotBlank(printerModel)) {
            for (Map.Entry<String, List<String>> entry : codeEntry.entrySet()) {
                if (StrUtil.isNotBlank(entry.getKey()) && containsModel(entry.getValue(), printerModel)) {
                    return entry.getKey();
                }
            }
        }

        for (Map.Entry<String, List<String>> entry : codeEntry.entrySet()) {
            List<String> models = entry.getValue();
            if (StrUtil.isNotBlank(entry.getKey()) && (models == null || models.isEmpty())) {
                return entry.getKey();
            }
        }

        return codeEntry.keySet().stream()
                .filter(StrUtil::isNotBlank)
                .findFirst()
                .orElse(null);
    }

    private boolean containsModel(List<String> models, String printerModel) {
        if (CollUtil.isEmpty(models)) {
            return false;
        }
        return models.stream().anyMatch(model -> printerModel.equalsIgnoreCase(model));
    }

    private String normalizeModel(String printerModel) {
        if (StrUtil.isBlank(printerModel)) {
            return null;
        }

        PrinterModelEnum modelEnum = PrinterModelEnum.getEnumByValue(printerModel);
        if (modelEnum != null) {
            return modelEnum.getText();
        }

        return printerModel.trim().replace(" ", "").toUpperCase();
    }

    private String resolveSeverity(long rawCode) {
        int level = (int) (rawCode >> 16);
        return HMS_SEVERITY_LEVELS.getOrDefault(level, "unknown");
    }

    private String resolveModule(long attr) {
        int module = (int) ((attr >> 24) & 0xFF);
        return HMS_MODULES.getOrDefault(module, "unknown");
    }
}
