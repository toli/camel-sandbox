package com.carmanconsulting.sandbox.camel;

import org.apache.camel.test.spring.CamelSpringTestSupport;
import org.apache.camel.util.FileUtil;
import org.apache.commons.io.IOUtils;
import org.apache.xbean.spring.context.ClassPathXmlApplicationContext;
import org.junit.Test;
import org.springframework.context.support.AbstractApplicationContext;

import java.io.File;
import java.io.FileInputStream;

public class SpringFileCopyTest extends CamelSpringTestSupport
{
    @Override
    protected AbstractApplicationContext createApplicationContext()
    {
        return new ClassPathXmlApplicationContext("classpath:file-copy.xml");
    }

    @Test
    public void testFileUnchangedThroughDirect() throws Exception
    {
        FileUtil.deleteFile(new File("target/outbox-direct-spring/order.xls"));
        FileUtil.copyFile(new File("target/test-classes/order.xls"), new File("target/inbox-direct-spring/order.xls"));
        Thread.sleep(2000);
        assertFileExists("target/outbox-direct-spring/order.xls");
        byte[] original = IOUtils.toByteArray(new FileInputStream("target/test-classes/order.xls"));
        byte[] copy = IOUtils.toByteArray(new FileInputStream("target/outbox-direct-spring/order.xls"));
        assertArrayEquals(original, copy);
    }
}
