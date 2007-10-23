package org.limewire.inject;

import junit.framework.Test;

import org.limewire.concurrent.AbstractLazySingletonProvider;
import org.limewire.util.BaseTestCase;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.Singleton;
import com.google.inject.name.Names;

public class AbstractModuleTest extends BaseTestCase {
    
    public AbstractModuleTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(AbstractModuleTest.class);
    }
    
    public void testSameInstanceIfSingletonAndCached() {
        Injector injector = Guice.createInjector(new AbstractModule() {
            @Override
            protected void configure() {
                bindAll(Names.named("A"), I3.class, P.class, I2.class, I1.class);
            }
        });
        
        I3 i3 = injector.getInstance(Key.get(I3.class, Names.named("A")));
        I2 i2 = injector.getInstance(Key.get(I2.class, Names.named("A")));
        I1 i1 = injector.getInstance(Key.get(I1.class, Names.named("A")));
        
        assertSame(i3, i2);
        assertSame(i3, i1);
    }

    
    private static interface I1 {}
    private static interface I2 extends I1 {}
    private static interface I3 extends I2 {}
    
    @Singleton
    private static class P extends AbstractLazySingletonProvider<I3> {
        @Override
        protected I3 createObject() {
            return new I3() {};
        }
    }
    
   
}
