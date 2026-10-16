# Mars Printer Hub - 拓竹3D打印机管理系统

本仓库为无需 License 激活的版本。VIP 激活页面输入小写 `mars`，即可将当前用户的 VIP 有效期设为 `9999-12-31 23:59:59`；该激活码支持重复使用。原有登录鉴权、Open API 密钥校验和普通 VIP 激活码仍然有效。

## 📋 项目概述

**Mars Printer Hub** 是一个专为拓竹（Bambu Lab）3D打印机设计的综合管理系统，提供设备监控、耗材管理、打印任务跟踪等功能。系统采用现代化的微服务架构，支持多设备管理、实时状态监控和智能通知推送。

### 🎯 功能定位

- **设备管理中心**：统一管理多台拓竹3D打印机
- **耗材管理系统**：精确跟踪耗材使用情况和成本
- **实时监控平台**：提供打印进度、设备状态的实时监控
- **智能通知服务**：支持多种通知方式的任务完成提醒

### 🛠 技术栈

| 技术分类      | 技术选型          | 版本                 | 说明      |
|-----------|---------------|--------------------|---------|
| **后端框架**  | Spring Boot   | 3.4.6              | 主框架     |
| **开发语言**  | Java          | 21                 | 编程语言    |
| **数据库**   | MySQL         | 8.0+               | 关系型数据库  |
| **缓存**    | Redis         | 7.0+               | 缓存和会话存储 |
| **ORM框架** | MyBatis Plus  | 3.5.12             | 数据访问层   |
| **权限认证**  | Sa-Token      | 1.43.0             | 认证授权框架  |
| **实时通信**  | SSE           | -                  | 实时数据通知  |
| **消息队列**  | MQTT          | Eclipse Paho 1.2.5 | 设备通信    |
| **API文档** | Knife4j       | 4.6.0              | 接口文档生成  |
| **对象存储**  | 腾讯云COS/S3     | -                  | 文件存储    |
| **通知服务**  | Bark/Telegram/Feishu | -             | 消息推送    |
| **容器化**   | Docker        | -                  | 应用部署    |

---

## 🏗 核心模块说明

### 1. 用户认证模块

**功能**：用户注册、登录、权限管理

- 支持用户注册和登录验证
- 基于Sa-Token的权限认证
- 用户信息管理和密码修改

### 2. 拓竹云服务模块

**功能**：与拓竹云平台集成，获取设备信息和访问凭证

- 拓竹云账户登录（支持邮箱/手机号+验证码）
- 获取用户绑定的打印机设备列表
- 获取设备访问码用于本地MQTT连接
- 设备在线状态和打印状态查询

### 3. 设备管理模块

**功能**：设备状态监控、打印进度跟踪

- 实时获取设备状态（温度、进度、层数等）
- 打印任务进度监控和剩余时间计算
- AMS（自动换料系统）状态查询
- 设备在线状态检测和缓存管理

### 4. MQTT通信模块

**功能**：与打印机建立MQTT连接，实现实时数据交换

- 支持本地网络和云端MQTT连接
- 智能连接策略：优先本地连接，失败时自动切换云端
- 实时接收打印机状态数据和事件通知
- 连接状态管理和自动重连机制

### 5. 耗材管理模块

**功能**：耗材库存管理、使用记录跟踪

- 耗材信息录入（材料类型、颜色、品牌、重量、价格）
- 耗材使用记录和成本统计
- AMS槽位与耗材绑定管理
- 耗材使用情况分析和汇总报告

### 6. SSE实时通信模块

**功能**：向前端推送实时设备状态变更通知

- 建立SSE长连接
- 推送设备状态变化通知
- 前端收到通知后重新拉取详情接口
- 连接管理和保活心跳

### 7. 通知服务模块

**功能**：支持多种通知方式的消息推送

- **Bark通知**：适用于iOS设备的轻量级推送
- **Telegram通知**：支持代理的跨平台消息推送
- **飞书通知**：企业团队常用的统一消息推送通道
- 打印完成、设备异常等事件通知
- 可配置的通知模板和推送策略

### 8. 事件驱动模块

**功能**：解耦业务逻辑，实现异步事件处理

- 设备连接/断开事件处理
- 打印任务状态变化事件
- 设备快照请求事件
- SSE通知事件分发

---

## 🚀 部署运行指南

### 环境要求

| 组件         | 版本要求  | 说明        |
|------------|-------|-----------|
| **Java**   | 21+   | 运行环境      |
| **Maven**  | 3.8+  | 构建工具      |
| **MySQL**  | 8.0+  | 数据库       |
| **Redis**  | 7.0+  | 缓存服务      |
| **Docker** | 20.0+ | 容器化部署（可选） |

### 安装步骤

#### 1. 克隆项目

```bash
git clone https://github.com/Marsdlc/mars-printer-hub-backend-unlocked.git
cd mars-printer-hub-backend-unlocked
```

#### 2. 数据库初始化

```bash
# 创建数据库
mysql -u root -p
CREATE DATABASE mars_printer_hub CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;

# 导入数据表结构
mysql -u root -p mars_printer_hub < sql/mars_printer_hub.sql
```

#### 3. 配置文件修改

编辑 `src/main/resources/application-local.yml`：

```yaml
spring:
  datasource:
    driver-class-name: com.mysql.cj.jdbc.Driver
    url: jdbc:mysql://localhost:3306/mars_printer_hub
    username:
    password:
  data:
    redis:
      # Redis数据库索引（默认为0）
      database: 1
      # Redis服务器地址
      host: localhost
      # Redis服务器连接端口
      port: 6379

# 拓竹云服务配置
bambu:
  intranet: true
  # MQTT连接配置
  mqtt:
  # 是否初始化所有设备
#    init-all: true

# RustFS 对象存储配置（兼容 S3 API）
rustfs:
  client:
    # 是否启用 RustFS 存储
    enabled: true
    # RustFS 服务端点地址
    endpoint: localhost:9000
    # 区域设置（RustFS 通常不严格校验，可使用默认值）
    region: us-east-1
    # 访问密钥
    accessKey:
    # 私有密钥
    secretKey:
    # 默认存储桶名称
    defaultBucket: printer
    # 域名
    domain:

# 通知服务配置
notification:
  # 总开关：是否启用通知功能
  enabled: true
  # 默认通知渠道（bark / telegram / feishu）
  default-channel: telegram
  # Bark通知配置（iOS推送）
  bark:
    server-url:
    device-key:

  # Telegram通知配置
  telegram:
    bot-token:
    chat-id:
    proxy:
      enabled: true
      host: localhost
      port: 7890

  # Feishu通知配置
  feishu:
    webhook-url:
    secret:
```

#### 4. 本地开发启动

```bash
# 使用Maven启动
./mvnw spring-boot:run

# 或者使用开发配置
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

#### 5. Docker容器化部署

```bash
# 构建镜像
docker build -t mars-printer-hub:latest .

# 运行容器
docker run -d \
  --name mars-printer-hub \
  -p 8123:8123 \
  -e SPRING_PROFILES_ACTIVE=prod \
  -e SPRING_DATASOURCE_URL=jdbc:mysql://host.docker.internal:3306/mars_printer_hub \
  -e SPRING_DATASOURCE_USERNAME=root \
  -e SPRING_DATASOURCE_PASSWORD=your_password \
  mars-printer-hub:latest
```

### 启动命令

| 环境       | 命令                                                                      | 说明     |
|----------|-------------------------------------------------------------------------|--------|
| **开发环境** | `./mvnw spring-boot:run -Dspring-boot.run.profiles=dev`                 | 启用详细日志 |
| **本地环境** | `./mvnw spring-boot:run -Dspring-boot.run.profiles=local`               | 默认配置   |
| **生产环境** | `java -jar target/mars-printer-hub-*.jar --spring.profiles.active=prod` | 生产部署   |

### 验证部署

- **健康检查**：`GET http://localhost:8123/api/health`
- **API文档**：`http://localhost:8123/api/swagger-ui.html`
- **应用监控**：`http://localhost:8123/api/actuator/health`

---

## 📞 技术支持

### 常见问题

**Q: MQTT连接失败怎么办？**
A: 检查网络连接，确认设备访问码正确，优先尝试本地连接

**Q: 如何添加新的通知方式？**
A: 继承`AbstractNotifier`类，实现`doSend`方法，并在配置中注册

**Q: 数据库连接池配置建议？**
A: 生产环境建议使用HikariCP，最大连接数设置为CPU核心数的2倍

### 联系方式

- **项目地址**：[GitHub Repository]
- **问题反馈**：[GitHub Issues]
- **技术交流**：[Discussion Forum]
