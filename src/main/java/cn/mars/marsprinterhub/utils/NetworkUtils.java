package cn.mars.marsprinterhub.utils;

import lombok.extern.slf4j.Slf4j;

/**
 * 网络工具类
 * 提供IP地址转换等网络相关的工具方法
 */
@Slf4j
public class NetworkUtils {
    
    /**
     * 将整数格式的IP地址转换为标准IP地址字符串
     * 使用小端字节序（little-endian）进行转换
     * 
     * @param ipInt 整数格式的IP地址
     * @return 标准IP地址字符串，如"192.168.1.100"
     */
    public static String getIpAddressFromInt(long ipInt) {
        if (ipInt == 0) {
            return null;
        }
        
        try {
            // 使用小端字节序转换（与Python代码保持一致）
            int byte1 = (int) (ipInt & 0xFF);
            int byte2 = (int) ((ipInt >> 8) & 0xFF);
            int byte3 = (int) ((ipInt >> 16) & 0xFF);
            int byte4 = (int) ((ipInt >> 24) & 0xFF);
            
            return String.format("%d.%d.%d.%d", byte1, byte2, byte3, byte4);
        } catch (Exception e) {
            log.warn("转换IP地址失败: ipInt={}", ipInt, e);
            return null;
        }
    }
    
    /**
     * 验证IP地址格式是否有效
     * 
     * @param ipAddress IP地址字符串
     * @return 是否为有效的IP地址
     */
    public static boolean isValidIpAddress(String ipAddress) {
        if (ipAddress == null || ipAddress.trim().isEmpty()) {
            return false;
        }
        
        String[] parts = ipAddress.split("\\.");
        if (parts.length != 4) {
            return false;
        }
        
        try {
            for (String part : parts) {
                int num = Integer.parseInt(part);
                if (num < 0 || num > 255) {
                    return false;
                }
            }
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }
    
    /**
     * 检查IP地址是否为局域网地址
     * 
     * @param ipAddress IP地址字符串
     * @return 是否为局域网地址
     */
    public static boolean isLocalNetworkAddress(String ipAddress) {
        if (!isValidIpAddress(ipAddress)) {
            return false;
        }
        
        String[] parts = ipAddress.split("\\.");
        int firstOctet = Integer.parseInt(parts[0]);
        int secondOctet = Integer.parseInt(parts[1]);
        
        // 检查常见的局域网地址段
        // 10.0.0.0/8
        if (firstOctet == 10) {
            return true;
        }
        
        // 172.16.0.0/12
        if (firstOctet == 172 && secondOctet >= 16 && secondOctet <= 31) {
            return true;
        }
        
        // 192.168.0.0/16
        if (firstOctet == 192 && secondOctet == 168) {
            return true;
        }

        return false;
    }
}