package com.carmanconsulting.sandbox.camel;

import com.carmanconsulting.sandbox.camel.jms.LoggingPooledConnectionFactory;
import com.google.common.collect.MapMaker;
import org.apache.activemq.pool.PooledConnectionFactory;
import org.apache.camel.Exchange;
import org.apache.camel.Expression;
import org.apache.camel.Processor;
import org.apache.camel.Produce;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.Service;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.builder.SimpleBuilder;
import org.apache.camel.component.jms.JmsConfiguration;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jms.ConnectionFactory;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import java.lang.management.ManagementFactory;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;

public class PermitThrottlerTest extends JmsTestCase {
//----------------------------------------------------------------------------------------------------------------------
// Fields
//----------------------------------------------------------------------------------------------------------------------

    private static final Logger LOGGER = LoggerFactory.getLogger(PermitThrottlerTest.class);
    private static final String PERMITTED_HEADER = "permitted";

    @Produce(uri = "jms:queue:input")
    private ProducerTemplate template;

//----------------------------------------------------------------------------------------------------------------------
// Other Methods
//----------------------------------------------------------------------------------------------------------------------

    @Override
    protected JmsConfiguration createJmsConfiguration() {
        final ConnectionFactory connectionFactory = createConnectionFactory();
        final PooledConnectionFactory pooledConnectionFactory = new LoggingPooledConnectionFactory();
        pooledConnectionFactory.setConnectionFactory(connectionFactory);
        JmsConfiguration config = new JmsConfiguration(connectionFactory);
        config.setListenerConnectionFactory(connectionFactory);
        config.setTemplateConnectionFactory(pooledConnectionFactory);
        return config;
    }

    @Override
    protected RouteBuilder createRouteBuilder() throws Exception {
        return new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                final SemaphoreThrottlingEngine engine = new SemaphoreThrottlingEngine(5);
                final MBeanServer server = ManagementFactory.getPlatformMBeanServer();
                final ObjectName name = new ObjectName("com.carmanconsulting.sandbox.camel:type=SemaphoreThrottlingEngine");
                server.registerMBean(engine, name);
                final SimpleBuilder correlationIdExpression = simple("${header[group]}");
                from("jms:queue:input")
                        .process(new AcquirePermitProcessor(correlationIdExpression, engine))
                        .choice()
                        .when(header(PERMITTED_HEADER)).to("jms:queue:output")
                        .otherwise().to("jms:queue:input");
                from("jms:queue:output?concurrentConsumers=20").process(new RandomProcessor(1000, 2000)).to("log:processed?showAll=true&multiline=true&level=INFO").to("jms:queue:release");
                from("jms:queue:release").process(new ReleasePermitProcessor(correlationIdExpression, engine));
            }
        };
    }

    @Test
    public void testThrottlingEngine() throws Exception {
        final Random random = new Random();
        final int nGroups = 10;
        for (int i = 0; i < 100; ++i) {
            final int group = random.nextInt(nGroups);
            template.sendBodyAndHeader(String.format("Message %d (group %d)", i, group), "group", group);
        }

        Thread.sleep(20000);
    }

//----------------------------------------------------------------------------------------------------------------------
// Inner Classes
//----------------------------------------------------------------------------------------------------------------------

    private static class AcquirePermitProcessor implements Processor {
        private final Expression correlationIdExpression;
        private final SemaphoreThrottlingEngine engine;

        private AcquirePermitProcessor(Expression correlationIdExpression, SemaphoreThrottlingEngine engine) {
            this.correlationIdExpression = correlationIdExpression;
            this.engine = engine;
        }

        @Override
        public void process(Exchange exchange) throws Exception {
            final String correlationId = correlationIdExpression.evaluate(exchange, String.class);
            exchange.getIn().setHeader(PERMITTED_HEADER, engine.acquirePermit(correlationId));
        }
    }

    private static class RandomProcessor implements Processor {
        private final long minimumDuration;
        private final long variance;
        private final Random random = new Random();

        private RandomProcessor(long minimumDuration, long variance) {
            this.minimumDuration = minimumDuration;
            this.variance = variance;
        }

        @Override
        public void process(Exchange exchange) throws Exception {
            final long duration = minimumDuration + Math.abs(random.nextLong()) % variance;
            Thread.sleep(duration);
        }
    }

    private static class ReleasePermitProcessor implements Processor {
        private final Expression correlationIdExpression;
        private final SemaphoreThrottlingEngine engine;

        private ReleasePermitProcessor(Expression correlationIdExpression, SemaphoreThrottlingEngine engine) {
            this.correlationIdExpression = correlationIdExpression;
            this.engine = engine;
        }

        @Override
        public void process(Exchange exchange) throws Exception {
            final String correlationId = correlationIdExpression.evaluate(exchange, String.class);
            LOGGER.info("Releasing permit ({})...", correlationId);
            engine.releasePermit(correlationId);
        }
    }

    public static interface SemaphoreThrottlingEngineMBean {
        Map<String, Integer> getPermitCounts();

        void setPermitCount(String correlationId, int permits);
    }

    private static class SemaphoreThrottler {
        private final Semaphore semaphore;
        private final AtomicInteger maxPermitCount;

        private SemaphoreThrottler(int maxPermitCount) {
            this.maxPermitCount = new AtomicInteger(maxPermitCount);
            this.semaphore = new Semaphore(maxPermitCount);
        }

        public void setMaxPermitCount(int newMaxPermitCount) {
            final int oldMaxPermitCount = maxPermitCount.getAndSet(newMaxPermitCount);
            final int additionalPermits = newMaxPermitCount - oldMaxPermitCount;
            if (additionalPermits > 0) {
                semaphore.release(additionalPermits);
            }
        }

        public int getMaxPermitCount() {
            return maxPermitCount.get();
        }

        public boolean acquirePermit() {
            return semaphore.tryAcquire();
        }

        public void releasePermit() {
            if (semaphore.availablePermits() < maxPermitCount.get() ) {
                semaphore.release();
            }
        }

    }

    private static class SemaphoreThrottlingEngine implements SemaphoreThrottlingEngineMBean, Service {

        @Override
        public void start() throws Exception {

        }

        @Override
        public void stop() throws Exception {

        }

        private Map<String, SemaphoreThrottler> throttlers = new MapMaker().concurrencyLevel(10).makeMap();

        private final int defaultPermitCount;

        private SemaphoreThrottlingEngine(int defaultPermitCount) {
            this.defaultPermitCount = defaultPermitCount;
        }

        public boolean acquirePermit(String correlationId) {
            LOGGER.debug("Attempting to acquire permit ({})...", correlationId);
            SemaphoreThrottler throttler = getOrCreateThrottler(correlationId);
            final boolean acquired = throttler.acquirePermit();
            if (acquired) {
                LOGGER.info("Permit acquired ({})", correlationId);
            } else {
                LOGGER.debug("Permit unavailable ({})", correlationId);
            }
            return acquired;
        }

        private SemaphoreThrottler getOrCreateThrottler(String correlationId) {
            SemaphoreThrottler throttler = throttlers.get(correlationId);
            if (throttler == null) {
                LOGGER.info("Creating new throttler ({})...", correlationId);
                throttler = new SemaphoreThrottler(defaultPermitCount);
                throttlers.put(correlationId, throttler);
            }
            return throttler;
        }

        public void releasePermit(String correlationId) {
            SemaphoreThrottler throttler = throttlers.get(correlationId);
            if (throttler != null) {
                throttler.releasePermit();
            }
        }

        @Override
        public Map<String, Integer> getPermitCounts() {
            Map<String, Integer> permits = new HashMap<String, Integer>();
            for (Map.Entry<String, SemaphoreThrottler> entry : throttlers.entrySet()) {
                permits.put(entry.getKey(), entry.getValue().getMaxPermitCount());
            }
            return permits;
        }

        @Override
        public void setPermitCount(String correlationId, int permits) {
            getOrCreateThrottler(correlationId).setMaxPermitCount(permits);
        }
    }
}
