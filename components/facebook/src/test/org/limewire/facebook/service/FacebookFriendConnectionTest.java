package org.limewire.facebook.service;

import java.io.IOException;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;

import junit.framework.TestCase;

import org.limewire.concurrent.AbstractLazySingletonProvider;
import org.limewire.concurrent.ScheduledListeningExecutorService;
import org.limewire.concurrent.SimpleTimer;
import org.limewire.core.api.friend.FriendEvent;
import org.limewire.core.api.friend.FriendPresenceEvent;
import org.limewire.core.api.friend.feature.FeatureEvent;
import org.limewire.inject.AbstractModule;
import org.limewire.listener.EventBroadcaster;
import org.limewire.listener.EventMulticaster;
import org.limewire.listener.EventMulticasterImpl;
import org.limewire.listener.ListenerSupport;

import com.google.code.facebookapi.FacebookException;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Singleton;
import com.google.inject.Stage;
import com.google.inject.TypeLiteral;
import com.google.inject.name.Names;

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
                        EventMulticaster<FriendEvent> knownMulticaster = new EventMulticasterImpl<FriendEvent>(FriendEvent.class);
                        EventMulticaster<FriendEvent> availMulticaster = new EventMulticasterImpl<FriendEvent>();
                        EventMulticaster<FriendPresenceEvent> presenceMulticaster = new EventMulticasterImpl<FriendPresenceEvent>();
                        EventMulticaster<FeatureEvent> featureMulticaster = new EventMulticasterImpl<FeatureEvent>();
                        
                        bind(new TypeLiteral<ListenerSupport<FriendEvent>>(){}).annotatedWith(Names.named("known")).toInstance(knownMulticaster);
                        bind(new TypeLiteral<EventBroadcaster<FriendEvent>>(){}).annotatedWith(Names.named("known")).toInstance(knownMulticaster);
                        
                        bind(new TypeLiteral<ListenerSupport<FriendEvent>>(){}).annotatedWith(Names.named("available")).toInstance(availMulticaster);
                        bind(new TypeLiteral<EventBroadcaster<FriendEvent>>(){}).annotatedWith(Names.named("available")).toInstance(availMulticaster);
                        
                        bind(new TypeLiteral<ListenerSupport<FriendPresenceEvent>>(){}).toInstance(presenceMulticaster);
                        bind(new TypeLiteral<EventBroadcaster<FriendPresenceEvent>>(){}).toInstance(presenceMulticaster);
                    
                        bind(new TypeLiteral<ListenerSupport<FeatureEvent>>(){}).toInstance(featureMulticaster);
                        bind(new TypeLiteral<EventMulticaster<FeatureEvent>>(){}).toInstance(featureMulticaster);
                        
                        bindAll(Names.named("backgroundExecutor"), ScheduledListeningExecutorService.class, BackgroundTimerProvider.class, ExecutorService.class, Executor.class, ScheduledExecutorService.class);
                                        
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
