package org.limewire.facebook.service;

import java.io.IOException;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;

import org.json.JSONException;
import org.limewire.concurrent.AbstractLazySingletonProvider;
import org.limewire.concurrent.ScheduledListeningExecutorService;
import org.limewire.concurrent.SimpleTimer;
import org.limewire.core.api.friend.LimeWireFriendModule;
import org.limewire.core.api.friend.client.FriendException;
import org.limewire.http.auth.LimeWireHttpAuthModule;
import org.limewire.inject.AbstractModule;
import org.limewire.inject.MutableProvider;
import org.limewire.inject.MutableProviderImpl;
import org.limewire.lifecycle.LimeWireCommonLifecycleModule;
import org.limewire.listener.EventMulticasterImpl;
import org.limewire.listener.ListenerSupport;
import org.limewire.net.LimeWireNetTestModule;
import org.limewire.net.address.AddressEvent;

import com.google.code.facebookapi.FacebookException;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Singleton;
import com.google.inject.Stage;
import com.google.inject.TypeLiteral;
import com.google.inject.name.Names;

import junit.framework.TestCase;

public class FacebookFriendConnectionTest extends TestCase {
    private Injector injector;

    public FacebookFriendConnectionTest(String name) {
        super(name);
    }

    @Override
    protected void setUp() throws Exception {
        injector = Guice.createInjector(Stage.PRODUCTION, new LimeWireHttpAuthModule(),
                new LimeWireFriendModule(),
                new LimeWireCommonLifecycleModule(),
                new LimeWireNetTestModule(),
                new LimeWireFacebookModule(),
                new AbstractModule() {
                    @Override
                    protected void configure() {
                        bindAll(Names.named("backgroundExecutor"), ScheduledListeningExecutorService.class, BackgroundTimerProvider.class, ExecutorService.class, Executor.class, ScheduledExecutorService.class);
                        bind(new TypeLiteral<ListenerSupport<AddressEvent>>(){}).toInstance(new EventMulticasterImpl<AddressEvent>());
                        bind(new TypeLiteral<MutableProvider<String>>(){}).annotatedWith(ChatChannel.class).toInstance(new MutableProviderImpl<String>(""));
                    }
                });
    }

    public void testLogin() throws IOException, FacebookException, JSONException, InterruptedException, FriendException {
        FacebookFriendService service = injector.getInstance(FacebookFriendService.class);
        service.loginImpl(null);
        Thread.sleep(1000 * 60 * 5);
    }
    
    @Singleton
    private static class BackgroundTimerProvider extends AbstractLazySingletonProvider<ScheduledListeningExecutorService> {
        @Override
        protected ScheduledListeningExecutorService createObject() {
            return new SimpleTimer(true);
        }
    }
}
