package com.example.exampleapp;

import com.jcraft.jsch.JSch;
import com.yu.jsch.*;
import com.yu.jsch.auth.PasswordAuthStrategy;
import com.yu.jsch.channel.ExecChannelFactory;
import com.yu.jsch.channel.SftpChannelFactory;
import com.yu.jsch.client.DefaultSftpClient;
import com.yu.jsch.client.DefaultSshClient;
import com.yu.jsch.client.SftpClient;
import com.yu.jsch.client.SshClient;
import com.yu.jsch.observability.ObservabilityConfig;
import com.yu.jsch.strategy.ExponentialBackoffRetryStrategy;
import com.yu.jsch.strategy.RetryStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

/**
 * SSH Bean 配置类
 * 完全通过 Java 代码配置 SSH 客户端，无需配置文件
 */
@Configuration
public class SshBeanConfiguration {


    /**
     * 创建 JSch 实例
     */
    @Bean
    public JSch jsch() {
        return new JSch();
    }

    /**
     * 创建可观测性配置
     */
    @Bean
    public ObservabilityConfig observabilityConfig() {
        return ObservabilityConfig.defaults(); // 启用日志记录和默认指标名称
    }

    /**
     * 创建 staging 环境的 SSH 客户端
     */
    //@Bean("stagingSshClient")
    public SshClient stagingSshClient(JSch jsch, ObservabilityConfig observabilityConfig) {
        // 创建 SessionFactory
        JschSessionFactory sessionFactory = JschSessionFactory.builder()
                .jsch(() -> jsch)
                .host("192.168.101.146")
                .port(22)
                .username("tinkers")
                .authStrategy(new PasswordAuthStrategy("tinkers"))
                .connectTimeoutMillis(3000)
                .socketTimeoutMillis(30000)
                .enableCompression(false)
                .knownHostsMode(KnownHostsMode.OFF)
                .build();

        // 创建连接池配置
        SessionPoolProperties poolProps = new SessionPoolProperties();
        poolProps.setEnabled(true);
        poolProps.setMaxTotal(8);
        poolProps.setMaxIdle(4);
        poolProps.setMinIdle(0);
        poolProps.setValidateOnBorrow(true);

        // 创建 SessionManager
        SessionManager sessionManager = new PooledSessionManager(sessionFactory, poolProps);

        // 创建重试策略
        RetryStrategy retryStrategy = ExponentialBackoffRetryStrategy.builder()
                .maxAttempts(2) // 3次尝试 = 1次初始 + 2次重试
                .baseDelayMillis(200)
                .multiplier(2.0)
                .build();

        // 创建 SshTemplate
        SshTemplate sshTemplate = new SshTemplate(sessionManager, new ExecChannelFactory(), retryStrategy, observabilityConfig, "staging");

        // 创建 HostContext Map
        Map<String, DefaultSshClient.HostContext> hostMap = new HashMap<>();
        hostMap.put("staging", new DefaultSshClient.HostContext(sshTemplate, 3000));

        return new DefaultSshClient(hostMap, "staging");
    }

    /**
     * 创建 staging 环境的 SFTP 客户端
     */
    // @Bean("stagingSftpClient")
    public SftpClient stagingSftpClient(JSch jsch, ObservabilityConfig observabilityConfig) {
        // 创建 SessionFactory
        JschSessionFactory sessionFactory = JschSessionFactory.builder()
                .jsch(() -> jsch)
                .host("192.168.101.146")
                .port(22)
                .username("tinkers")
                .authStrategy(new PasswordAuthStrategy("tinkers"))
                .connectTimeoutMillis(3000)
                .socketTimeoutMillis(30000)
                .enableCompression(false)
                .knownHostsMode(KnownHostsMode.OFF)
                .build();

        // 创建连接池配置
        SessionPoolProperties poolProps = new SessionPoolProperties();
        poolProps.setEnabled(true);
        poolProps.setMaxTotal(8);
        poolProps.setMaxIdle(4);
        poolProps.setMinIdle(0);
        poolProps.setValidateOnBorrow(true);

        // 创建 SessionManager
        SessionManager sessionManager = new PooledSessionManager(sessionFactory, poolProps);

        // 创建重试策略
        RetryStrategy retryStrategy = ExponentialBackoffRetryStrategy.builder()
                .maxAttempts(2)
                .baseDelayMillis(200)
                .multiplier(2.0)
                .build();

        // 创建 SftpTemplate
        SftpTemplate sftpTemplate = new SftpTemplate(sessionManager, new SftpChannelFactory(), retryStrategy, observabilityConfig, "staging");

        // 创建 HostContext Map
        Map<String, DefaultSftpClient.HostContext> hostMap = new HashMap<>();
        hostMap.put("staging", new DefaultSftpClient.HostContext(sftpTemplate, 3000));

        return new DefaultSftpClient(hostMap, "staging");
    }

}
