/*
 * Copyright 2017-2020 original authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.micronaut.configuration.lettuce.test;

import io.lettuce.core.RedisURI;
import io.micronaut.configuration.lettuce.AbstractRedisConfiguration;
import io.micronaut.configuration.lettuce.RedisSetting;
import io.micronaut.context.annotation.ConfigurationBuilder;
import io.micronaut.context.annotation.ConfigurationProperties;
import io.micronaut.context.annotation.Factory;
import io.micronaut.context.annotation.Requires;
import io.micronaut.context.event.BeanCreatedEvent;
import io.micronaut.context.event.BeanCreatedEventListener;
import io.micronaut.core.io.socket.SocketUtils;
import io.micronaut.core.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.embedded.RedisServer;
import redis.embedded.RedisServerBuilder;
import redis.embedded.exceptions.RedisBuildingException;

import javax.annotation.PreDestroy;
import java.io.Closeable;
import java.io.IOException;
import java.util.Optional;

/**
 * A bean for an embedded Redis server.
 *
 * @author Graeme Rocher
 * @since 1.0
 *
 * @deprecated since 5.3.0, use TestContainers RedisContainer instead.
 */
@Requires(classes = RedisServer.class)
@Requires(beans = AbstractRedisConfiguration.class)
@Factory
@Deprecated
public class EmbeddedRedisServer implements BeanCreatedEventListener<AbstractRedisConfiguration>, Closeable {

    private static final Logger LOGGER = LoggerFactory.getLogger(EmbeddedRedisServer.class);

    private static final String DEFAULT_MAXMEMORY_SETTING = "maxmemory 256M";
    private static final String DEFAULT_BIND_SETTING = "bind 127.0.0.1 ::1";

    private final Configuration embeddedConfiguration;
    private RedisServer redisServer;

    /**
     * Constructor.
     *
     * @param embeddedConfiguration embeddedConfiguration
     */
    public EmbeddedRedisServer(Configuration embeddedConfiguration) {
        this.embeddedConfiguration = embeddedConfiguration;
    }

    @Override
    public AbstractRedisConfiguration onCreated(BeanCreatedEvent<AbstractRedisConfiguration> event) {
        AbstractRedisConfiguration configuration = event.getBean();
        Optional<RedisURI> uri = configuration.getUri();
        int port = configuration.getPort();
        String host = configuration.getHost();
        if (uri.isPresent()) {
            RedisURI redisURI = uri.get();
            port = redisURI.getPort();
            host = redisURI.getHost();
        }
        if (StringUtils.isNotEmpty(host) && host.equals("localhost") && SocketUtils.isTcpPortAvailable(port)) {
            RedisServerBuilder builder = embeddedConfiguration.builder;
            builder.port(port);
            try {
                builder.setting(DEFAULT_MAXMEMORY_SETTING);
                builder.setting(DEFAULT_BIND_SETTING);
            } catch (RedisBuildingException e) {
                LOGGER.debug("Embedded settings failed as config file is present");
            }
            redisServer = builder.build();
            redisServer.start();
        }
        return configuration;
    }

    @Override
    @PreDestroy
    public void close() throws IOException {
        if (redisServer != null) {
            redisServer.stop();
        }
    }

    /**
     * Configuration properties for embedded Redis.
     *
     * @deprecated since 5.3.0, use TestContainers RedisContainer instead.
     */
    @ConfigurationProperties(RedisSetting.REDIS_EMBEDDED)
    @Requires(classes = RedisServerBuilder.class)
    @Deprecated
    public static class Configuration {
        @ConfigurationBuilder(
                prefixes = ""
        )
        RedisServerBuilder builder = new RedisServerBuilder().port(SocketUtils.findAvailableTcpPort());
    }
}
