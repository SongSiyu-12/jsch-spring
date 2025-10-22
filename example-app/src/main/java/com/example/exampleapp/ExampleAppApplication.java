package com.example.exampleapp;

import com.yu.jsch.client.SftpClient;
import com.yu.jsch.client.SshClient;
import com.yu.jsch.exec.ExecResult;
import com.yu.jsch.sftp.TransferOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

import java.nio.charset.StandardCharsets;

@SpringBootApplication
@EnableConfigurationProperties(ExampleProperties.class)
public class ExampleAppApplication {

    private static final Logger log = LoggerFactory.getLogger(ExampleAppApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(ExampleAppApplication.class, args);
    }

    /**
     * YAML 配置方式的示例运行器
     */
    @Bean
    @ConditionalOnProperty(prefix = "example", name = "run-on-startup", havingValue = "true")
    CommandLineRunner yamlConfigRunner(ExampleProperties props, SshClient ssh, SftpClient sftp) {
        return args -> {
            log.info("=== YAML Configuration Example ===");
            log.info("Example runner starting using host '{}'", props.getHost());
            ExecResult res = ssh.exec(props.getHost(), props.getCommand());
            log.info("SSH exec exit={}, stdout='{}'", res.getExitCode(), res.getStdout().trim());

            byte[] data = ("Example file uploaded by YAML config at " + java.time.Instant.now()).getBytes(StandardCharsets.UTF_8);
            String remotePath = props.getRemotePath();
            sftp.upload(props.getHost(), data, remotePath, TransferOptions.defaults());
            byte[] downloaded = sftp.download(props.getHost(), remotePath);
            log.info("SFTP round-trip success: {} bytes", downloaded.length);
        };
    }
}
