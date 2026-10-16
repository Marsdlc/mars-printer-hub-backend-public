package cn.mars.marsprinterhub.license;

import cn.hutool.crypto.digest.DigestUtil;
import cn.mars.marsprinterhub.mapper.SysAppConfigMapper;
import cn.mars.marsprinterhub.model.entity.SysAppConfig;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.UUID;
import java.util.stream.Stream;

@Service
public class LicenseService extends ServiceImpl<SysAppConfigMapper, SysAppConfig> {

    /* ================= 常量定义 ================= */

    private static final String KEY_INSTALL_UUID = "install_uuid";
    /**
     * MachineCode 在运行期应当是稳定的
     */
    private volatile String cachedMachineCode;

    /**
     * 兼容旧版前端的激活状态查询，系统已无需 License。
     */
    public boolean isActivated() {
        return true;
    }

    /**
     * 对外获取机器码
     */
    public String getMachineCode() {
        if (cachedMachineCode != null) {
            return cachedMachineCode;
        }

        synchronized (this) {
            if (cachedMachineCode != null) {
                return cachedMachineCode;
            }

            String installUuid = getOrInitInstallUuid();
            String cpuModel = readCpuModel();
            int cpuCores = Runtime.getRuntime().availableProcessors();
            long memoryGb = readMemoryGbFloored();

            String raw = String.format(
                    "%s|%s|%d|%d",
                    installUuid, cpuModel, cpuCores, memoryGb
            );

            cachedMachineCode = DigestUtil.md5Hex(raw).toUpperCase();
            return cachedMachineCode;
        }
    }

    /* ================= install_uuid 管理 ================= */

    private synchronized String getOrInitInstallUuid() {
        SysAppConfig config = this.getById(KEY_INSTALL_UUID);
        if (config != null) {
            return config.getConfigValue();
        }

        String uuid = UUID.randomUUID().toString();
        SysAppConfig newConfig = new SysAppConfig();
        newConfig.setConfigKey(KEY_INSTALL_UUID);
        newConfig.setConfigValue(uuid);
        this.save(newConfig);

        System.err.println(
                "install_uuid 被重新生成，可能是数据库被重置"
        );

        return uuid;
    }

    /* ================= 硬件指纹 ================= */

    private String readCpuModel() {
        try (Stream<String> lines =
                     Files.lines(Paths.get("/proc/cpuinfo"), StandardCharsets.UTF_8)) {

            return lines.filter(l -> l.startsWith("model name"))
                    .findFirst()
                    .map(l -> l.split(":", 2)[1].trim())
                    .orElse("GENERIC_CPU");

        } catch (IOException e) {
            return "GENERIC_CPU";
        }
    }

    /**
     * 内存模糊化：向下取整到 GB
     */
    private long readMemoryGbFloored() {
        try (Stream<String> lines =
                     Files.lines(Paths.get("/proc/meminfo"), StandardCharsets.UTF_8)) {

            long memKb = lines.filter(l -> l.startsWith("MemTotal"))
                    .findFirst()
                    .map(l -> l.replaceAll("\\D+", ""))
                    .map(Long::parseLong)
                    .orElse(0L);

            long memGb = memKb / 1024 / 1024;
            return Math.max(memGb, 1);

        } catch (IOException e) {
            return 1;
        }
    }
}
