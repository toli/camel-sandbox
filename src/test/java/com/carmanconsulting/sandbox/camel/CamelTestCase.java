package com.carmanconsulting.sandbox.camel;

import org.apache.camel.CamelContext;
import org.apache.camel.test.junit4.CamelTestSupport;

public abstract class CamelTestCase extends CamelTestSupport
{
    @Override
    protected CamelContext createCamelContext() throws Exception
    {
        CamelContext context = super.createCamelContext();
        initializeCamelContext(context);
        return context;
    }

    protected void initializeCamelContext(CamelContext context)
    {

    }
}
