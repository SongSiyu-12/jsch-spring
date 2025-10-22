# JSCH Spring Boot Starter

[![Java](https://img.shields.io/badge/Java-17+-blue.svg)](https://openjdk.java.net/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.9-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Maven](https://img.shields.io/badge/Maven-3.6+-red.svg)](https://maven.apache.org/)
[![License](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

一个功能强大、易于使用的 Spring Boot Starter，用于 SSH 和 SFTP 操作。基于 JSch 库构建，提供了连接池、重试机制、可观测性等企业级特性。

## 🚀 特性

### 核心功能

- **SSH 命令执行** - 支持远程命令执行和结果获取
- **SFTP 文件操作** - 文件上传、下载、列表、删除等完整操作
- **多主机支持** - 同时管理多个 SSH 连接
- **连接池** - 基于 Apache Commons Pool2 的高性能连接池
- **重试机制** - 指数退避重试策略，提高连接稳定性
- **认证方式** - 支持密码和公钥认证
- **已知主机管理** - 灵活的 known_hosts 验证策略

### 企业级特性

- **Spring Boot 自动配置** - 零配置开箱即用
- **配置属性绑定** - 类型安全的配置属性
- **可观测性** - 结构化日志和指标支持
- **健康检查** - Spring Boot Actuator 集成
- **测试支持** - Testcontainers 集成测试
- **多种配置方式** - YAML 配置 + Java Bean 配置

## 📦 项目结构

```
ssh_file/
├── jsch-spring-boot-starter/     # Spring Boot Starter 核心库
│   ├── src/main/java/
│   │   └── com/example/jsch/
│   │       ├── auth/             # 认证策略
│   │       ├── channel/          # 通道工厂
│   │       ├── client/           # 客户端接口和实现
│   │       ├── config/           # 配置类
│   │       ├── exec/             # SSH 执行相关
│   │       ├── observability/    # 可观测性
│   │       ├── sftp/             # SFTP 操作
│   │       ├── strategy/         # 重试策略
│   │       └── util/             # 工具类
│   └── src/test/                 # 单元测试和集成测试
├── example-app/                  # 示例应用
│   ├── src/main/java/
│   │   └── com/example/exampleapp/
│   │       ├── ExampleAppApplication.java
│   │       └── SshBeanConfiguration.java  # Java Bean 配置示例
│   └── src/main/resources/
│       └── application.yml       # 配置示例
└── pom.xml                       # 父 POM
```

## 🛠️ 快速开始

### 1. 环境要求

- **Java 17+**
- **Spring Boot 3.2+**
- **Maven 3.6+**

### 2. 添加依赖

在您的 `pom.xml` 中添加：

```xml

<repositories>
    <repository>
        <id>mvn-repo</id>
        <url>https://raw.githubusercontent.com/SongSiyu-12/mvn-repo/master/</url>
        <snapshots>
            <enabled>true</enabled>
            <updatePolicy>always</updatePolicy>
        </snapshots>
    </repository>
</repositories>
<dependencies>
<dependency>
    <groupId>com.yu</groupId>
    <artifactId>jsch-spring-boot-starter</artifactId>
    <version>0.0.1-SNAPSHOT</version>
</dependency>
</dependencies>
```

### 3. 配置连接

在 `application.yml` 中配置 SSH 连接：

```yaml
ssh:
  hosts:
    staging:
      host: 192.168.1.100
      username: deploy
      authentication:
        type: password
        password: your-password
    production:
      host: prod.example.com
      username: deploy
      authentication:
        type: public_key
        private-key-path: /path/to/private/key
        passphrase: key-passphrase
  defaults:
    port: 22
    pool:
      enabled: true
      max-total: 8
    retry:
      enabled: true
      max-attempts: 3
```

### 4. 使用客户端

```java

@Service
public class DeploymentService {

    @Autowired
    private SshClient sshClient;

    @Autowired
    private SftpClient sftpClient;

    public void deploy() {
        // 执行远程命令
        ExecResult result = sshClient.exec("staging", "systemctl status nginx");
        log.info("Command output: {}", result.getStdout());

        // 上传文件
        byte[] configData = loadConfigFile();
        sftpClient.upload("staging", configData, "/etc/nginx/nginx.conf",
                TransferOptions.defaults());

        // 重启服务
        sshClient.exec("staging", "systemctl restart nginx");
    }
}
```

## 📋 配置选项

### 主机配置

```yaml
ssh:
  hosts:
    myhost:
      host: example.com              # 主机地址
      port: 22                       # SSH 端口
      username: user                 # 用户名
      authentication:
        type: password               # 认证类型: password | public_key
        password: secret             # 密码（密码认证）
        private-key-path: /path/key  # 私钥路径（公钥认证）
        private-key: "-----BEGIN..." # 私钥内容（内联）
        passphrase: key-pass         # 私钥密码
      timeouts:
        connect: 5s                  # 连接超时
        authentication: 10s          # 认证超时
        session: 10m                 # 会话超时
        read: 30s                    # 读取超时
      compression: true              # 启用压缩
      compression-level: 6           # 压缩级别 (1-9)
      server-alive-interval: 15s     # 心跳间隔
      server-alive-count-max: 3      # 最大心跳失败次数
      known-hosts:
        mode: strict                 # 验证模式: strict | off | accept_new
        path: ~/.ssh/known_hosts     # known_hosts 文件路径
```

### 连接池配置

```yaml
ssh:
  defaults:
    pool:
      enabled: true                  # 启用连接池
      max-total: 16                  # 最大连接数
      max-idle: 8                    # 最大空闲连接数
      min-idle: 2                    # 最小空闲连接数
      max-wait: 30s                  # 最大等待时间
      validate-on-borrow: true       # 借用时验证
      validate-on-return: false      # 归还时验证
      test-while-idle: true          # 空闲时测试
      time-between-eviction: 30s     # 清理间隔
```

### 重试配置

```yaml
ssh:
  defaults:
    retry:
      enabled: true                  # 启用重试
      max-attempts: 3                # 最大重试次数
      delay: 200ms                   # 重试延迟
      multiplier: 2.0                # 延迟倍数（指数退避）
      max-delay: 5s                  # 最大延迟
```

### 可观测性配置

```yaml
ssh:
  observability:
    enabled: true                    # 启用可观测性
    metric-names:
      session-connect: ssh.session.connect
      ssh-exec: ssh.exec
      sftp-operation: ssh.sftp
```

## 🔧 高级用法

### Java Bean 配置

除了 YAML 配置，还支持纯 Java 代码配置：

```java

@Configuration
@ConditionalOnProperty(prefix = "ssh.bean", name = "enabled", havingValue = "true")
public class SshBeanConfiguration {

    @Bean("stagingSshClient")
    public SshClient stagingSshClient(JSch jsch, ObservabilityConfig observabilityConfig) {
        // 创建 SessionFactory
        JschSessionFactory sessionFactory = JschSessionFactory.builder()
                .jsch(() -> jsch)
                .host("192.168.1.100")
                .port(22)
                .username("deploy")
                .authStrategy(new PasswordAuthStrategy("password"))
                .connectTimeoutMillis(3000)
                .knownHostsMode(KnownHostsMode.OFF)
                .build();

        // 创建连接池
        SessionPoolProperties poolProps = new SessionPoolProperties();
        poolProps.setEnabled(true);
        poolProps.setMaxTotal(8);

        // 创建 SessionManager
        SessionManager jschSessionManager = new PooledSessionManager(sessionFactory, poolProps);

        // 创建重试策略
        RetryStrategy retryStrategy = ExponentialBackoffRetryStrategy.builder()
                .maxAttempts(2)
                .baseDelayMillis(200)
                .build();

        // 创建 SshTemplate
        SshTemplate sshTemplate = new SshTemplate(jschSessionManager, new ExecChannelFactory(),
                retryStrategy, observabilityConfig, "staging");

        // 创建客户端
        Map<String, DefaultSshClient.HostContext> hostMap = new HashMap<>();
        hostMap.put("staging", new DefaultSshClient.HostContext(sshTemplate, 3000));

        return new DefaultSshClient(hostMap, "staging");
    }
}
```

### 运行时动态主机/凭据配置（DB/Vault）

在保持原有基于 hostId 的解析与调用逻辑不变的前提下，支持两种使用方式：

- 通过 hostId 调用（默认逻辑，使用 HostResolver 解析）
- 直接传入 HostConfig 调用（绕过解析器，便于 DB/Vault 场景）

增强点：

- 新增接口 HostResolver：Optional<HostConfig> resolve(String hostId)
- 默认实现 PropertiesHostResolver：基于 ssh.hosts.* 配置；如用户提供 @Primary HostResolver，则优先使用用户实现
- 新增 SshClient/SftpClient 直传 HostConfig 重载：
    - SshClient.exec(HostConfig, SshCommandRequest)
    - SftpClient.list(HostConfig, path)、upload(HostConfig, ...)、download(HostConfig, ...)、delete(HostConfig, path)
- 连接池以稳定键进行复用，并提供 SessionManager.invalidate(hostKey)/invalidateAll()
    - 稳定键格式：host:port:username（不含敏感信息）
    - HostConfig 可选 version 字段；借用连接前会比对，版本不一致将自动重建连接池

使用示例：

```java
// 1) 自定义 HostResolver（例如从数据库读取），标记为 @Primary 可覆盖默认解析器
@Primary
@Bean
HostResolver dbHostResolver(HostConfigRepository repo) {
    return new DatabaseHostResolver(repo);
}

// 2) 直接使用 HostConfig 调用（覆盖 hostId）
HostConfig cfg = HostConfig.builder()
        .host("10.0.0.10")
        .port(22)
        .username("deploy")
        .auth(HostConfig.Auth.builder().type(AuthType.PASSWORD).password(secret.toCharArray()).build())
        .connectTimeoutMillis(3000)
        .version(42L) // 可选：用于强制轮换连接池
        .build();

// SSH 命令执行（直传 HostConfig）
ExecResult r = sshClient.exec(cfg, SshCommandRequest.builder("whoami").build());

// SFTP 操作（直传 HostConfig）
List<SftpFileInfo> files = sftpClient.list(cfg, "/var/log");
sftpClient.

upload(cfg, dataBytes, "/tmp/app.conf",TransferOptions.defaults());
byte[] downloaded = sftpClient.download(cfg, "/tmp/app.conf");
sftpClient.

delete(cfg, "/tmp/app.conf");

// 3) 主动使某主机的连接池失效（凭据或版本变更后）
String hostKey = cfg.stableKey(); // 形如：host:22:username
jschSessionManager.

invalidate(hostKey);   // 仅清理该主机
jschSessionManager.

invalidateAll();       // 清理所有主机

// 注：可通过 Spring 注入 SessionManager Bean
// @Autowired private SessionManager jschSessionManager;
```

注意事项：

- 密码与密钥口令以 char[] 持有，并在建立会话工厂后立即清零，避免长时间驻留内存。
- 日志仅输出 host/port/username 等非敏感字段，敏感信息已脱敏处理。
- 默认仍兼容 ssh.hosts.* 配置；如果提供自定义 HostResolver Bean（建议 @Primary），将自动接管解析。

启用 Java Bean 配置：

```yaml
ssh:
  bean:
    enabled: true
```

### 常见问题排查（FAQ）

- StrictHostKey（已知主机验证）
    - 生产环境建议使用 strict 模式，并指定 known_hosts 路径
    - YAML 示例：
      ```yaml
      ssh:
        hosts:
          prod:
            known-hosts:
              mode: strict
              path: /etc/ssh/known_hosts
      ```

- 压缩（compression）
    - JSch 支持 zlib 压缩，但需要额外引入 jzlib 依赖：
      ```xml
      <dependency>
        <groupId>com.jcraft</groupId>
        <artifactId>jzlib</artifactId>
        <version>1.1.3</version>
      </dependency>
      ```
    - 在本 Starter 中可通过 Java Bean 方式启用压缩：
      ```java
      JschSessionFactory sf = JschSessionFactory.builder()
          .jsch(() -> jsch)
          .host("host")
          .port(22)
          .username("user")
          .enableCompression(true)
          .compressionLevel(6)
          .build();
      ```
    - 目前未提供 YAML 开关；如需全局开启，可自定义 Bean 覆盖默认客户端

- 认证优先级
    - 公钥认证优先（PreferredAuthentications: publickey,keyboard-interactive,password）
    - 使用密码认证时为（password,keyboard-interactive,publickey）

- 连接池键与版本
    - 连接池键：host:port:username（HostConfig.stableKey()）
    - HostConfig.version 可用于触发会话池重建（版本不一致自动重建）
    - 也可在凭据变更后手动调用 SessionManager.invalidate(hostKey) 或 invalidateAll()

### 自定义认证策略

```java
public class CustomAuthStrategy implements AuthStrategy {
    @Override
    public void configure(JSch jsch, Session session, HostDefinition host) throws JSchException {
        // 实现自定义认证逻辑
        // 例如：多因素认证、动态令牌等
    }
}
```

### 自定义重试策略

```java
public class CustomRetryStrategy implements RetryStrategy {
    @Override
    public void execute(String operation, Supplier<Void> action) throws Exception {
        // 实现自定义重试逻辑
    }
}
```

## 🧪 测试

### 运行测试

```bash
# 运行所有测试
mvn test

# 运行集成测试
mvn verify

# 跳过测试构建
mvn install -DskipTests
```

### 使用 Testcontainers

项目包含基于 Testcontainers 的集成测试：

```java

@SpringBootTest
@Testcontainers
class SshIntegrationTest {

    @Container
    static GenericContainer<?> sshContainer = new GenericContainer<>("linuxserver/openssh-server")
            .withExposedPorts(2222)
            .withEnv("PUID", "1000")
            .withEnv("PGID", "1000")
            .withEnv("PASSWORD_ACCESS", "true")
            .withEnv("USER_PASSWORD", "password")
            .withEnv("USER_NAME", "testuser");

    @Test
    void testSshConnection() {
        // 测试 SSH 连接
    }
}
```

## 📊 监控和可观测性

### 指标

Starter 提供以下指标：

- `ssh.session.connect` - 会话连接指标
- `ssh.exec` - SSH 命令执行指标
- `ssh.sftp` - SFTP 操作指标

### 日志

启用结构化日志记录：

```yaml
logging:
  level:
    com.example.jsch: DEBUG
```

### 健康检查

添加 Actuator 依赖启用健康检查：

```xml

<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
```

访问健康检查端点：

```
GET /actuator/health/ssh
```

## 🔒 安全最佳实践

### 1. 凭据管理

**❌ 不要在配置文件中硬编码密码：**

```yaml
ssh:
  hosts:
    prod:
      password: hardcoded-password  # 不安全！
```

**✅ 使用环境变量：**

```yaml
ssh:
  hosts:
    prod:
      password: ${SSH_PASSWORD}
```

**✅ 使用 Spring Boot 配置加密：**

```yaml
ssh:
  hosts:
    prod:
      password: '{cipher}AQA...'  # 加密后的密码
```

### 2. 私钥管理

**✅ 使用安全的私钥路径：**

```yaml
ssh:
  hosts:
    prod:
      authentication:
        type: public_key
        private-key-path: /secure/path/to/key
        passphrase: ${KEY_PASSPHRASE}
```

### 3. Known Hosts 验证

**✅ 生产环境使用严格验证：**

```yaml
ssh:
  hosts:
    prod:
      known-hosts:
        mode: strict
        path: /etc/ssh/known_hosts
```

### 4. 网络安全

- 使用 VPN 或专用网络
- 限制 SSH 访问的源 IP
- 定期轮换密钥和密码
- 启用 SSH 密钥代理转发保护

## 🚀 示例应用

### 运行示例

```bash
# 构建项目
mvn clean install

# 运行示例应用
cd example-app
mvn spring-boot:run

# 或者启用示例运行器
mvn spring-boot:run -Dspring-boot.run.arguments="--example.run-on-startup=true"
```

### YAML 配置示例

```bash
# 使用 YAML 配置
mvn spring-boot:run -Dspring-boot.run.arguments="--example.run-on-startup=true"
```

### Java Bean 配置示例

```bash
# 使用 Java Bean 配置
mvn spring-boot:run -Dspring-boot.run.arguments="--ssh.bean.enabled=true --example.bean.run-on-startup=true"
```

## 🤝 贡献

欢迎贡献代码！请遵循以下步骤：

1. Fork 项目
2. 创建特性分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 开启 Pull Request

### 开发指南

- 遵循 Java 代码规范
- 添加单元测试
- 更新文档
- 确保所有测试通过

## 📄 许可证

本项目采用 MIT 许可证 - 查看 [LICENSE](LICENSE) 文件了解详情。

## 🆘 支持

### 常见问题

**Q: 如何处理连接超时？**

```yaml
ssh:
  defaults:
    timeouts:
      connect: 10s      # 增加连接超时
      read: 60s         # 增加读取超时
```

**Q: 如何启用调试日志？**

```yaml
logging:
  level:
    com.example.jsch: DEBUG
    com.jcraft.jsch: DEBUG
```

**Q: 如何配置多个环境？**

```yaml
spring:
  profiles:
    active: staging

---
spring:
  config:
    activate:
      on-profile: staging
ssh:
  hosts:
    default:
      host: staging.example.com

---
spring:
  config:
    activate:
      on-profile: production
ssh:
  hosts:
    default:
      host: prod.example.com
```

## 🔄 版本历史

### v0.0.1-SNAPSHOT (当前版本)

- ✨ 初始版本发布
- 🚀 SSH 和 SFTP 基础功能
- 🔧 Spring Boot 自动配置
- 🏊 连接池支持
- 🔄 重试机制
- 📊 可观测性支持
- ❌ Testcontainers 集成测试
- ⚙️ Java Bean 配置支持

---

