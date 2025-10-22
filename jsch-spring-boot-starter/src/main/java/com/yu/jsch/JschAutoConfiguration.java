package com.yu.jsch;

import com.jcraft.jsch.JSch;
import com.yu.jsch.channel.ExecChannelFactory;
import com.yu.jsch.channel.SftpChannelFactory;
import com.yu.jsch.client.ResolverBackedSftpClient;
import com.yu.jsch.client.ResolverBackedSshClient;
import com.yu.jsch.client.SftpClient;
import com.yu.jsch.client.SshClient;
import com.yu.jsch.host.HostResolver;
import com.yu.jsch.host.PropertiesHostResolver;
import com.yu.jsch.observability.ObservabilityConfig;
import com.yu.jsch.strategy.NoRetryStrategy;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
@ConditionalOnClass(JSch.class)
@EnableConfigurationProperties({SshProperties.class, SessionPoolProperties.class})
public class JschAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public JSch jsch() {
        return new JSch();
    }

    @Bean
    @ConditionalOnMissingBean
    public ExecChannelFactory execChannelFactory() {
        return new ExecChannelFactory();
    }

    @Bean
    @ConditionalOnMissingBean
    public SftpChannelFactory sftpChannelFactory() {
        return new SftpChannelFactory();
    }

    @Bean
    @ConditionalOnMissingBean
    public HostResolver hostResolver(SshProperties properties) {
        return new PropertiesHostResolver(properties);
    }

    @Bean
    @ConditionalOnBean(JSch.class)
    @ConditionalOnMissingBean(JschSessionManager.class)
    public JschSessionManager jschSessionManager(JSch jsch, SessionPoolProperties poolProps) {
        return new HostConfigJschSessionManager(() -> jsch, poolProps);
    }

    @Bean
    @ConditionalOnBean(JSch.class)
    @ConditionalOnMissingBean(SshClient.class)
    public SshClient sshClient(SshProperties properties, JschSessionManager jschSessionManager,
                               HostResolver resolver, ExecChannelFactory execChannelFactory) {
        ObservabilityConfig observability = ObservabilityConfig.fromProperties(properties.getObservability());
        SshTemplate template = new SshTemplate(jschSessionManager, execChannelFactory, new NoRetryStrategy(), observability, null);
        String defaultAlias = properties.getHosts().size() == 1 ? properties.getHosts().keySet().iterator().next() : null;
        return new ResolverBackedSshClient(resolver, template, defaultAlias);
    }

    @Bean
    @ConditionalOnBean(JSch.class)
    @ConditionalOnMissingBean(SftpClient.class)
    public SftpClient sftpClient(SshProperties properties, JschSessionManager jschSessionManager,
                                 HostResolver resolver, SftpChannelFactory sftpChannelFactory) {
        ObservabilityConfig observability = ObservabilityConfig.fromProperties(properties.getObservability());
        SftpTemplate template = new SftpTemplate(jschSessionManager, sftpChannelFactory, new NoRetryStrategy(), observability, null);
        String defaultAlias = properties.getHosts().size() == 1 ? properties.getHosts().keySet().iterator().next() : null;
        return new ResolverBackedSftpClient(resolver, template, defaultAlias);
    }
}
