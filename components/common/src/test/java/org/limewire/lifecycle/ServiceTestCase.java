package org.limewire.lifecycle;

import java.util.ArrayList;
import java.util.List;

import org.limewire.common.LimeWireCommonModule;
import org.limewire.util.BaseTestCase;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;

public abstract class ServiceTestCase extends BaseTestCase {
    protected Injector injector;
    protected ServiceRegistry registry;

    public ServiceTestCase(String name) {
        super(name);
    }

    protected void setUp() throws Exception {
        super.setUp();
        injector = Guice.createInjector(getModules());
        registry = injector.getInstance(ServiceRegistry.class);
        registry.initialize();
        registry.start();
    }
    
    private Module [] getModules() {
        List<Module> modules = new ArrayList<Module>();
        modules.add(new LimeWireCommonModule());
        modules.addAll(getServiceModules());
        return modules.toArray(new Module[]{});
    }

    protected abstract List<Module> getServiceModules();

    protected void tearDown() throws Exception {
        super.tearDown();
        registry.stop();
    }
}
