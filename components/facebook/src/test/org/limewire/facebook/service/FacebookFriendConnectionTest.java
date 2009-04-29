package org.limewire.facebook.service;

import java.io.IOException;

import junit.framework.TestCase;

import org.limewire.concurrent.AbstractLazySingletonProvider;
import org.limewire.concurrent.ScheduledListeningExecutorService;
import org.limewire.concurrent.SimpleTimer;
import org.limewire.inject.AbstractModule;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Singleton;
import com.google.inject.Stage;
import com.google.inject.name.Names;
import com.google.code.facebookapi.FacebookException;

public class FacebookFriendConnectionTest extends TestCase {
    private Injector injector;

    public FacebookFriendConnectionTest(String name) {
        super(name);
    }

    @Override
    protected void setUp() throws Exception {
        injector = Guice.createInjector(Stage.PRODUCTION, new LimewireFacebookModule(),
                new AbstractModule() {
                    @Override
                    protected void configure() {
                        bind(String.class).annotatedWith(Names.named("facebookEmail")).toInstance("");
                        bind(String.class).annotatedWith(Names.named("facebookPassword")).toInstance("");
                        bind(String.class).annotatedWith(Names.named("facebookApiKey")).toInstance("");
                    }
                }
                /*, new LimeWireHttpModule(), new LimeWireNetModule(), new LimeWireIOModule(), new AbstractModule() {
                    @Override
                    protected void configure() {
                        bindAll(Names.named("backgroundExecutor"), ScheduledListeningExecutorService.class, BackgroundTimerProvider.class, ExecutorService.class, Executor.class, ScheduledExecutorService.class);
                    }
                }*/);
    }

    public void testLogin() throws IOException, FacebookException {
        FacebookFriendService service = injector.getInstance(FacebookFriendService.class);
        service.loginImpl();
    }
    
    @Singleton
    private static class BackgroundTimerProvider extends AbstractLazySingletonProvider<ScheduledListeningExecutorService> {
        @Override
        protected ScheduledListeningExecutorService createObject() {
            return new SimpleTimer(true);
        }
    }
}
