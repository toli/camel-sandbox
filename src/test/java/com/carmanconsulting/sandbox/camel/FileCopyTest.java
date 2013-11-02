package com.carmanconsulting.sandbox.camel;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.util.FileUtil;
import org.apache.commons.io.IOUtils;
import org.junit.Test;

import java.io.File;
import java.io.FileInputStream;

/**
 * User having trouble claimed they were having trouble copying Excel files successfully using Camel.
 *
 * @see <a href="http://camel.465427.n5.nabble.com/Can-t-move-excel-files-correctly-ActiveMQ-bug-td5741661.html">Form Posting</a>
 *
 */
public class FileCopyTest extends JmsTestCase
{
    @Test
    public void testFileUnchangedThroughJms() throws Exception
    {
        FileUtil.deleteFile(new File("target/outbox-jms/order.xls"));
        FileUtil.copyFile(new File("target/test-classes/order.xls"), new File("target/inbox-jms/order.xls"));
        Thread.sleep(2000);
        assertFileExists("target/outbox-jms/order.xls");
        byte[] original = IOUtils.toByteArray(new FileInputStream("target/test-classes/order.xls"));
        byte[] copy = IOUtils.toByteArray(new FileInputStream("target/outbox-jms/order.xls"));
        assertArrayEquals(original, copy);
    }

    @Test
    public void testFileUnchangedThroughDirect() throws Exception
    {
        FileUtil.deleteFile(new File("target/outbox-direct/order.xls"));
        FileUtil.copyFile(new File("target/test-classes/order.xls"), new File("target/inbox-direct/order.xls"));
        Thread.sleep(2000);
        assertFileExists("target/outbox-direct/order.xls");
        byte[] original = IOUtils.toByteArray(new FileInputStream("target/test-classes/order.xls"));
        byte[] copy = IOUtils.toByteArray(new FileInputStream("target/outbox-direct/order.xls"));
        assertArrayEquals(original, copy);
    }

    @Override
    protected RouteBuilder createRouteBuilder() throws Exception
    {
        return new RouteBuilder()
        {
            @Override
            public void configure() throws Exception
            {
                from("file://target/inbox-jms").to("jms:queue:myfiles");
                from("jms:queue:myfiles").to("file://target/outbox-jms");

                from("file://target/inbox-direct").to("direct:myfiles");
                from("direct:myfiles").to("file://target/outbox-direct");

            }
        };
    }
}
