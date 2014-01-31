package com.carmanconsulting.sandbox.camel;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.Produce;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.junit.Test;

public class LoadBalancerTest extends CamelTestCase {
//----------------------------------------------------------------------------------------------------------------------
// Fields
//----------------------------------------------------------------------------------------------------------------------

    @Produce(uri = "direct:input")
    private ProducerTemplate input;

//----------------------------------------------------------------------------------------------------------------------
// Other Methods
//----------------------------------------------------------------------------------------------------------------------

    @Override
    protected RouteBuilder createRouteBuilder() throws Exception {
        return new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("direct:input").process(new ThreadLogger())
                        .loadBalance().roundRobin().to("direct:route1").to("direct:route2");
                from("direct:route1").process(new ThreadLogger());
                from("direct:route2").process(new ThreadLogger());
            }
        };
    }

    @Test
    public void testLoadBalancing() {
        input.sendBody("Testing 1");
        input.sendBody("Testing 2");
    }

//----------------------------------------------------------------------------------------------------------------------
// Inner Classes
//----------------------------------------------------------------------------------------------------------------------

    private class ThreadLogger implements Processor {
        @Override
        public void process(Exchange exchange) throws Exception {
            logger.info("Received message {} on thread {}.", exchange.getIn().getBody(String.class), Thread.currentThread().getName());
        }
    }
}
