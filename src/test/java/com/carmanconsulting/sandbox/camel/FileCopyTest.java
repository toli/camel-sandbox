package com.carmanconsulting.sandbox.camel;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.util.FileUtil;
import org.junit.Test;

import java.io.File;

public class FileCopyTest extends JmsTestCase
{
    @Test
    public void testFileUnchangedThroughJms() throws Exception
    {
        FileUtil.deleteFile(new File("target/outbox/order.xls"));
        FileUtil.copyFile(new File("./target/test-classes/order.xls"), new File("./target/inbox/order.xls"));
        Thread.sleep(1000);
        assertFileExists("target/outbox/order.xls");
    }

    @Override
    protected RouteBuilder createRouteBuilder() throws Exception
    {
        return new RouteBuilder()
        {
            @Override
            public void configure() throws Exception
            {
                from("file://target/inbox").to("jms:queue:myfiles");
                from("jms:queue:myfiles").to("file://target/outbox");
            }
        };
    }
}
