package com.example.exampleapp;

import com.jcraft.jsch.JSch;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class ExampleAppApplicationTests {

    @Autowired
    private ApplicationContext context;

    @Test
    void contextLoadsAndJschBeanPresent() {
        assertThat(context).isNotNull();
        assertThat(context.getBean(JSch.class)).isNotNull();
    }
}
