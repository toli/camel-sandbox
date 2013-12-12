package com.carmanconsulting.sandbox.camel.throttle;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.EvictingQueue;
import com.google.common.collect.Queues;
import org.apache.camel.Exchange;
import org.apache.camel.Expression;
import org.apache.camel.Processor;

import java.util.Queue;
import java.util.concurrent.TimeUnit;

public class ClientThrottler implements Processor {
//----------------------------------------------------------------------------------------------------------------------
// Fields
//----------------------------------------------------------------------------------------------------------------------

    private final Expression clientIdExpression;
    private final int maxCount;
    private final long window;
    private final LoadingCache<String,ClientThrottlingState> stateCache = CacheBuilder.<String,ClientThrottlingState>newBuilder().maximumSize(1000).build(new ClientThrottlingStateLoader());

//----------------------------------------------------------------------------------------------------------------------
// Constructors
//----------------------------------------------------------------------------------------------------------------------

    public ClientThrottler(Expression clientIdExpression, int maxCount, long duration, TimeUnit timeUnit) {
        this.clientIdExpression = clientIdExpression;
        this.maxCount = maxCount;
        this.window = timeUnit.toMillis(duration);
    }

//----------------------------------------------------------------------------------------------------------------------
// Processor Implementation
//----------------------------------------------------------------------------------------------------------------------

    @Override
    public void process(Exchange exchange) throws Exception {
        final String clientId = clientIdExpression.evaluate(exchange, String.class);
        ClientThrottlingState state = stateCache.get(clientId);
        state.newMessageReceived();
    }

//----------------------------------------------------------------------------------------------------------------------
// Inner Classes
//----------------------------------------------------------------------------------------------------------------------

    private class ClientThrottlingState {
        private final Queue<Long> timestamps = Queues.synchronizedQueue(EvictingQueue.<Long>create(maxCount));

        public void newMessageReceived() {
            final long now = System.currentTimeMillis();
            if(timestamps.size() == maxCount) {
                final Long oldestTimestamp = timestamps.peek();

                final long duration = now - oldestTimestamp;
                if(duration <= window) {
                    throw new ClientThrottlingException("Received " + (maxCount + 1) + " requests in " + duration + " milliseconds.");
                }
                else {
                    timestamps.offer(now);
                }
            }
            else {
                timestamps.offer(now);
            }
        }
    }

    private class ClientThrottlingStateLoader extends CacheLoader<String,ClientThrottlingState> {
        @Override
        public ClientThrottlingState load(String clientId) throws Exception {
            return new ClientThrottlingState();
        }
    }
}
