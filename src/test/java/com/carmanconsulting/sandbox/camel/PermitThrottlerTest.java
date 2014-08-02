package com.carmanconsulting.sandbox.camel;

import com.carmanconsulting.sandbox.camel.jms.LoggingPooledConnectionFactory;
import com.google.common.collect.MapMaker;
import org.apache.activemq.pool.PooledConnectionFactory;
import org.apache.camel.Exchange;
import org.apache.camel.Expression;
import org.apache.camel.Processor;
import org.apache.camel.Produce;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.builder.SimpleBuilder;
import org.apache.camel.component.jms.JmsConfiguration;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jms.ConnectionFactory;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.Semaphore;

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
                final ThrottlingEngine engine = new ThrottlingEngine(5);
                final SimpleBuilder correlationIdExpression = simple("${header[group]}");
                from("jms:queue:input")
                        .process(new AcquirePermitProcessor(correlationIdExpression, engine))
                        .choice()
                        .when(header(PERMITTED_HEADER)).to("jms:queue:output")
                        .otherwise().to("jms:queue:input");
                from("jms:queue:output").process(new RandomProcessor(1000, 2000)).to("log:processed?showAll=true&multiline=true&level=INFO").to("jms:queue:release");
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
        private final ThrottlingEngine engine;

        private AcquirePermitProcessor(Expression correlationIdExpression, ThrottlingEngine engine) {
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
        private final ThrottlingEngine engine;

        private ReleasePermitProcessor(Expression correlationIdExpression, ThrottlingEngine engine) {
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

    private static class ThrottlingEngine {
        private Map<String, Semaphore> semaphores = new MapMaker().concurrencyLevel(10).makeMap();

        private final int defaultPermitCount;

        private ThrottlingEngine(int defaultPermitCount) {
            this.defaultPermitCount = defaultPermitCount;
        }

        public boolean acquirePermit(String correlationId) {
            LOGGER.debug("Attempting to acquire permit ({})...", correlationId);
            Semaphore semaphore = semaphores.get(correlationId);
            if (semaphore == null) {
                LOGGER.info("Creating new semaphore ({})...", correlationId);
                semaphore = new Semaphore(defaultPermitCount);
                semaphores.put(correlationId, semaphore);
            }
            final boolean acquired = semaphore.tryAcquire();
            if(acquired) {
                LOGGER.info("Permit acquired ({})", correlationId);
            }
            else {
                LOGGER.debug("Permit unavailable ({})", correlationId);
            }
            return acquired;
        }

        public void releasePermit(String correlationId) {
            Semaphore semaphore = semaphores.get(correlationId);
            if (semaphore != null) {
                semaphore.release();
            }
        }
    }
}
