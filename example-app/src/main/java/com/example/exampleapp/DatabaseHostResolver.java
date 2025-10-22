package com.example.exampleapp;

import com.yu.jsch.host.HostConfig;
import com.yu.jsch.host.HostConfigRepository;
import com.yu.jsch.host.HostResolver;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@Primary
class DatabaseHostResolver implements HostResolver {
    private final HostConfigRepository repo;

    public DatabaseHostResolver(HostConfigRepository repo) {
        this.repo = repo;
    }

    @Override
    public Optional<HostConfig> resolve(String hostId) {
        return repo.findById(hostId);
    }
}