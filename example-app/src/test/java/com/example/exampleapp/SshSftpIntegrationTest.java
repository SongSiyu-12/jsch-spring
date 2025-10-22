package com.example.exampleapp;

import com.yu.jsch.AuthType;
import com.yu.jsch.client.SftpClient;
import com.yu.jsch.client.SshClient;
import com.yu.jsch.host.HostConfig;
import com.yu.jsch.host.HostResolver;
import com.yu.jsch.sftp.SftpFileInfo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;

import static com.yu.jsch.KnownHostsMode.OFF;

@SpringBootTest
@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SshSftpIntegrationTest {

//    @Container
//    static GenericContainer<?> sshd = new GenericContainer<>("lscr.io/linuxserver/openssh-server:latest")
//            .withExposedPorts(2222)
//            .withEnv("PUID", "1000")
//            .withEnv("PGID", "1000")
//            .withEnv("TZ", "UTC")
//            .withEnv("PASSWORD_ACCESS", "true")
//            .withEnv("USER_NAME", "test")
//            .withEnv("USER_PASSWORD", "testpass")
//            .waitingFor(Wait.forListeningPort())
//            .withStartupAttempts(3)
//            .withStartupTimeout(Duration.ofMinutes(3));
//
//    @DynamicPropertySource
//    static void registerProps(DynamicPropertyRegistry registry) {
//        registry.add("ssh.observability.enabled", () -> "false");
//        registry.add("ssh.defaults.known-hosts.mode", () -> "off");
//        registry.add("ssh.hosts.test.host", () -> sshd.getHost());
//        registry.add("ssh.hosts.test.port", () -> sshd.getMappedPort(2222));
//        registry.add("ssh.hosts.test.username", () -> "test");
//        registry.add("ssh.hosts.test.authentication.type", () -> "password");
//        registry.add("ssh.hosts.test.authentication.password", () -> "testpass");
//    }

    @Autowired
    SshClient ssh;

    @Autowired
    SftpClient sftp;

    @Autowired
    HostResolver hostResolver;
    @Autowired
    SftpClient stagingSftpClient;

    @Test
    void canExecCommandAndTransferFile() throws Exception {
//        String marker = "hello-" + UUID.randomUUID();
//        ExecResult res = ssh.exec("test", "echo " + marker);
//        assertThat(res.getExitCode()).isZero();
//        assertThat(res.getStdout().trim()).isEqualTo(marker);
//
//        String remotePath = "it-" + System.currentTimeMillis() + ".txt";
//        byte[] payload = ("payload-" + UUID.randomUUID()).getBytes(StandardCharsets.UTF_8);
//        sftp.upload("test", payload, remotePath, TransferOptions.defaults());
//        byte[] roundTrip = sftp.download("test", remotePath);
//        assertThat(roundTrip).isEqualTo(payload);
        HostConfig cfg = HostConfig.builder()
                .host("192.168.101.71")
                .port(22)
                .username("tinkers")
                .auth(HostConfig.Auth.builder().type(AuthType.PASSWORD).password("tinkers".toCharArray()).build())
                .connectTimeoutMillis(3000)
                .knownHosts(HostConfig.KnownHosts.builder().mode(OFF).build())
                .version(2L)
                .build();

        hostResolver.resolve("test");
        List<SftpFileInfo> list = sftp.list("test", "/D:");
        // byte[] tests = sftp.download("test", "/");
//        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(tests);
        for (SftpFileInfo sftpFileInfo : list) {
            System.out.println(sftpFileInfo);
        }
//        ExecResult ls = ssh.exec(cfg, "dir");
//        System.out.println(ls.getStdout());

//        List<SftpFileInfo> list = stagingSftpClient.list("/");
//        System.out.println(list);
    }
}
