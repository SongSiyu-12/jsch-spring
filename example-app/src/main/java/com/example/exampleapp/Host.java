package com.example.exampleapp;

import com.yu.jsch.AuthType;
import com.yu.jsch.host.HostConfig;
import com.yu.jsch.host.HostConfigRepository;
import org.springframework.stereotype.Component;

import java.util.Optional;

import static com.yu.jsch.KnownHostsMode.OFF;

/**
 * @className: HostConfig
 * @author: songyiqiang
 * @date: 2025/10/21 15:57
 * @Version: 1.0
 * @description:
 */
@Component
public class Host implements HostConfigRepository {
    @Override
    public Optional<HostConfig> findById(String hostId) {

        HostConfig cfg = HostConfig.builder()
                .host("192.168.101.146")
                .port(22)
                .version(2L)
                .username("tinkers")
                .auth(HostConfig.Auth.builder().type(AuthType.PASSWORD).password("tinkers".toCharArray()).build())
                .connectTimeoutMillis(3000)
                .knownHosts(HostConfig.KnownHosts.builder().mode(OFF).build())
                .build();
        return Optional.of(cfg);
    }
}
