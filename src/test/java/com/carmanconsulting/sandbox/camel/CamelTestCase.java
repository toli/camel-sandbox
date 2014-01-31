package com.carmanconsulting.sandbox.camel;

import org.apache.camel.CamelContext;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.impl.JndiRegistry;
import org.apache.camel.impl.SimpleRegistry;
import org.apache.camel.test.junit4.CamelTestSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class CamelTestCase extends CamelTestSupport
{
//----------------------------------------------------------------------------------------------------------------------
// Fields
//----------------------------------------------------------------------------------------------------------------------

    protected final Logger logger = LoggerFactory.getLogger(getClass());

//----------------------------------------------------------------------------------------------------------------------
// Other Methods
//----------------------------------------------------------------------------------------------------------------------

    @Override
    protected CamelContext createCamelContext() throws Exception
    {
        final SimpleRegistry registry = new SimpleRegistry();
        doBindings(registry);
        CamelContext context = new DefaultCamelContext(registry);
        initializeCamelContext(context);
        return context;
    }

    protected void doBindings(SimpleRegistry registry)
    {

    }

    protected void initializeCamelContext(CamelContext context)
    {

    }
}
