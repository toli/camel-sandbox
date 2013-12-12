package com.carmanconsulting.sandbox.camel.throttle;

import com.carmanconsulting.sandbox.camel.CamelTestCase;
import com.carmanconsulting.sandbox.camel.throttle.ClientThrottler;
import org.apache.camel.CamelExecutionException;
import org.apache.camel.Produce;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.junit.Test;

import java.util.concurrent.TimeUnit;

/**
 * Created by jcarman on 12/12/13.
 */
public class ClientThrottlerTest extends CamelTestCase {

    @Produce(uri = "direct:throttleMe")
    private ProducerTemplate input;

    @Override
    protected RouteBuilder createRouteBuilder() throws Exception {
        return new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("direct:throttleMe")
                        .process(new ClientThrottler(body(), 5, 1, TimeUnit.SECONDS))
                .setBody(constant("success"));
            }
        };
    }

    @Test(expected = CamelExecutionException.class)
    public void testOverflow() {
        for (int i = 0; i < 6; ++i) {
            input.requestBody("foo");
        }
    }

    @Test
    public void testNoOverflowWhenMoreThanMaxCount() throws Exception {
        for (int i = 0; i < 5; ++i) {
            input.requestBody("foo");
        }
        Thread.sleep(1000);
        input.requestBody("foo");
    }

    @Test
    public void testNoOverflowWhenAtMaxCount() throws Exception {
        for (int i = 0; i < 5; ++i) {
            input.requestBody("foo");
        }
    }

    @Test
    public void testNoOverflowAfterPriorFailure() throws Exception {
        for (int i = 0; i < 5; ++i) {
            input.requestBody("foo");
        }
        try {
            input.requestBody("foo");
        }
        catch(CamelExecutionException e) {
            // Ignore...
        }
        Thread.sleep(1000);
        input.requestBody("foo");

    }
}
