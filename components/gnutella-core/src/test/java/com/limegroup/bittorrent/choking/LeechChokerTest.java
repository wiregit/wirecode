package com.limegroup.bittorrent.choking;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.States;
import org.limewire.collection.NECallable;

import junit.framework.Test;

import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.name.Names;
import com.limegroup.bittorrent.Chokable;
import com.limegroup.gnutella.LimeTestUtils;
import com.limegroup.gnutella.util.LimeTestCase;

public class LeechChokerTest extends LimeTestCase {

    public LeechChokerTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(LeechChokerTest.class);
    }
    
    private Mockery mockery;
    public void setUp() throws Exception {
        mockery = new Mockery();
    }
    
    /**
     * tests that the choker uses the provided scheduler service when started, executed and shutdown.
     */
    public void testStartRunShutdown() throws Exception {
        final ScheduledExecutorService ses = mockery.mock(ScheduledExecutorService.class);
        Module m = new AbstractModule() {
            public void configure() {
                bind(ScheduledExecutorService.class).annotatedWith(Names.named("nioExecutor")).toInstance(ses);
            }
        };

        final ScheduledFuture chokerFuture = mockery.mock(ScheduledFuture.class);
        final ScheduledFuture chokerFuture2 = mockery.mock(ScheduledFuture.class);
        
        NECallable<List<? extends Chokable>> emptyCallable = new NECallable<List<? extends Chokable>>() {
            public List<? extends Chokable> call() {
                return Collections.emptyList();
            }
        };
        
        Injector inj = LimeTestUtils.createInjector(m);
        ChokerFactory factory = inj.getInstance(ChokerFactory.class);
        final Choker c = factory.getChoker(emptyCallable, false);
        
        
        final States sesState = mockery.states("sesStates").startsAs("empty");
        mockery.checking(new Expectations() {{
            one(ses).schedule(c, 10 * 1000, TimeUnit.MILLISECONDS);
            when(sesState.is("empty"));
            will(returnValue(chokerFuture));
            then(sesState.is("scheduled"));
            one(ses).schedule(c, 10 * 1000, TimeUnit.MILLISECONDS);
            when(sesState.is("scheduled"));
            will(returnValue(chokerFuture2));
            then(sesState.is("rescheduled"));
            one(chokerFuture2).cancel(false);
            when(sesState.is("rescheduled"));
        }});
        c.start();
        c.run();
        c.shutdown();
        mockery.assertIsSatisfied();
    }
}
