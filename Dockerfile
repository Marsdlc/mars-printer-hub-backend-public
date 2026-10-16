# 1. 基础镜像
FROM marsss816/jdk21-ffmpeg:latest

# 2. 设置工作目录
WORKDIR /app

# 3. 复制 Jar 包
COPY target/mars-printer-hub-*.jar /app/app.jar

# 4. 【核心】给 NAS 用的默认参数写在这里
# 只要启动时不传 JAVA_OPTS，就会默认使用这一行，完美保持你以前 NAS 上的性能
ENV JAVA_OPTS="-Xms512m -Xmx2g -XX:+UseG1GC -XX:MaxGCPauseMillis=200 -XX:+UseContainerSupport"

# 5. 启动命令（最标准、最严谨的写法）
ENTRYPOINT ["sh", "-c", "exec java ${JAVA_OPTS} -jar /app/app.jar"]