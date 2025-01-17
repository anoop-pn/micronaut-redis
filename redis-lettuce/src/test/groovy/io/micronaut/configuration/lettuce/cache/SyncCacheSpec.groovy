package io.micronaut.configuration.lettuce.cache

import io.micronaut.context.ApplicationContext
import io.micronaut.core.io.socket.SocketUtils
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import spock.lang.AutoCleanup
import spock.lang.Shared
import spock.lang.Specification

/**
 * @author Graeme Rocher
 * @since 1.0
 */
class SyncCacheSpec extends Specification{

    @Shared @AutoCleanup ApplicationContext applicationContext = ApplicationContext.run(
            'redis.uri':"redis://localhost:${SocketUtils.findAvailableTcpPort()}",
            'redis.caches.counter.enabled':'true',
            'redis.caches.counter2.enabled':'true'
    )

    void "test cacheable annotations"() {

        when:
        CounterService counterService = applicationContext.getBean(CounterService)

        then:
        Flux.from(counterService.flowableValue("test")).toIterable().iterator().next() == 0
        Mono.from(counterService.singleValue("test")).toFuture().get() == 0

        when:
        counterService.reset()
        def result =counterService.increment("test")

        then:
        result == 1
        Flux.from(counterService.flowableValue("test")).toIterable().iterator().next() == 1
        counterService.futureValue("test").get() == 1
        Mono.from(counterService.singleValue("test")).toFuture().get() == 1
        counterService.getValue("test") == 1
        counterService.getValue("test") == 1

        when:
        result = counterService.incrementNoCache("test")

        then:
        result == 2
        Flux.from(counterService.flowableValue("test")).toIterable().iterator().next() == 1
        counterService.futureValue("test").get() == 1
        counterService.getValue("test") == 1

        when:
        counterService.reset("test")
        then:
        counterService.getValue("test") == 0

        when:
        counterService.reset("test")
        then:
        counterService.futureValue("test").get() == 0
        
        when:
        counterService.set("test", 3)

        then:
        counterService.getValue("test") == 3
        counterService.futureValue("test").get() == 3

        when:
        result = counterService.increment("test")

        then:
        result == 4
        counterService.getValue("test") == 4
        counterService.futureValue("test").get() == 4

        when:
        result = counterService.futureIncrement("test").get()

        then:
        result == 5
        counterService.getValue("test") == 5
        counterService.futureValue("test").get() == 5

        when:
        counterService.reset()

        then:
        !counterService.getOptionalValue("test").isPresent()
        counterService.getValue("test") == 0
        counterService.getOptionalValue("test").isPresent()
        counterService.getValue2("test") == 0

        when:
        counterService.increment("test")
        counterService.increment("test")

        then:
        counterService.getValue("test") == 2
        counterService.getValue2("test") == 0

        when:
        counterService.increment2("test")

        then:
        counterService.getValue("test") == 1
        counterService.getValue2("test") == 1

    }


}
