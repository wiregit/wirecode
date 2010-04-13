package org.limewire.ui;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

import javax.swing.SwingUtilities;

import junit.framework.Test;

import org.limewire.gnutella.tests.LimeTestUtils;
import org.limewire.inject.AbstractModule;
import org.limewire.inject.GuiceUtils;
import org.limewire.inject.LimeWireInjectModule;
import org.limewire.inject.Modules;
import org.limewire.inspection.InspectionException;
import org.limewire.inspection.InspectionRequirement;
import org.limewire.inspection.InspectionTool;
import org.limewire.inspection.Inspector;
import org.limewire.ui.swing.AllLimeWireModules__DO_NOT_USE;
import org.limewire.ui.swing.LimeWireModule;
import org.limewire.ui.swing.LimeWireSwingUiModule;
import org.limewire.ui.swing.mainframe.AppFrame;
import org.limewire.util.BaseTestCase;
import org.limewire.util.OSUtils;

import com.google.inject.Binding;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.Scopes;

public class AnnotationsCheckTest extends BaseTestCase {

    public AnnotationsCheckTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(AnnotationsCheckTest.class);
    }
    
    private Map<String, String> createInspectionMappings(final List<File> buildFolders, final AtomicReference<Injector> injectorRef) throws Exception {
        final Map<String, String> results = new ConcurrentHashMap<String, String>();
        // This is explicitly using the DoNotUse module because that's what the build uses.
        // (do it in the Swing thread to account for UI injectables that need the Swing thread)
        SwingUtilities.invokeAndWait(new Runnable() {
            @Override
            public void run() {
                Injector injector = Guice.createInjector(new AllLimeWireModules__DO_NOT_USE());
                GuiceUtils.loadEagerSingletons(injector);
                injectorRef.set(injector); 
                for(File path : buildFolders) {
                    results.putAll(InspectionTool.generateMappings(path, injectorRef.get(), new String[0]));
                }
            }
        });
        return results;
    }
    
    private void runInspectorTests(Inspector inspector, Map<String, String> results) {
        for(String key : results.keySet()) {
            try {
                inspector.inspect(results.get(key), true);
            } catch(InspectionException ie) {
                boolean validFailure = false;
                if(OSUtils.isLinux()) {
                    validFailure = ie.getRequirements().size() > 0 && !ie.getRequirements().contains(InspectionRequirement.OS_LINUX);
                } else if(OSUtils.isMacOSX()) {
                    validFailure = ie.getRequirements().size() > 0 && !ie.getRequirements().contains(InspectionRequirement.OS_OSX);
                } else if(OSUtils.isWindows()) {
                    validFailure = ie.getRequirements().size() > 0 && !ie.getRequirements().contains(InspectionRequirement.OS_WINDOWS);
                }
                if(!validFailure) {
                    fail("failed on key: " + key + ", value: " + results.get(key), ie);
//                    System.out.println("failed on key: " + key + ", value: " + results.get(key) + " -- " + ie);
                }
            }
        }
    }
    
    
    public void testAnnotations() throws Exception {
        List<File> buildFolders = LimeTestUtils.getBuildFolders(getClass());
        AtomicReference<Injector> injectorRef = new AtomicReference<Injector>();
        Map<String, String> results = createInspectionMappings(buildFolders, injectorRef);
        
        // make sure we got some good # of inspections
        assertFalse(results.isEmpty());
        assertGreaterThan(100, results.size());
        
        // and run through them all and make sure they work!
        Inspector inspector = injectorRef.get().getInstance(Inspector.class);
        
        Map<Key<?>, Binding<?>> preBindings = injectorRef.get().getAllBindings();
        runInspectorTests(inspector, results);
        Map<Key<?>, Binding<?>> postBindings = injectorRef.get().getAllBindings();
        
        // We look at the difference in bindings before & after and fail if any
        // bindings were added.  The idea here is that only things that were singletons
        // should have been bound, so no new bindings should have been added.
        // If a new binding was added, something either wasn't a singleton,
        // wasn't bound, or was created twice!
        Map<Key<?>, Binding<?>> diff = new HashMap<Key<?>, Binding<?>>(postBindings);
        diff.keySet().removeAll(preBindings.keySet());
        assertTrue("added bindings: " + diff, diff.isEmpty());
    }
    
    // This tests exists because the way LW starts, it uses two injectors --
    // one for core, and another for UI.  We can't use child injectors because
    // of quirks with just-in-time bindings.
    // However, by using two injectors, it messes up some detection of singletons...
    // This test makes sure that LW's setup can properly let us get everything
    // we want.
    public void testAnnotationsTheWayLimeWireWillDoIt() throws Exception {
        List<File> buildFolders = LimeTestUtils.getBuildFolders(getClass());
        final AtomicReference<Injector> injectorRef = new AtomicReference<Injector>();
        Map<String, String> results = createInspectionMappings(buildFolders, injectorRef);
        
        // make sure we got some good # of inspections
        assertFalse(results.isEmpty());
        assertGreaterThan(100, results.size());

        // Now create the real injector that we'll do inspection tests on,
        // and make sure that we reset the injector on the Inspector.
        injectorRef.set(null);
        final Injector coreInjector = Guice.createInjector(new LimeWireModule());
        SwingUtilities.invokeAndWait(new Runnable() {
            @Override
            public void run() {
                Injector injector = Guice.createInjector(
                        Modules.providersFrom(coreInjector),
                        new LimeWireInjectModule(),
                        new LimeWireSwingUiModule(),
                        new AbstractModule() {
                            @Override
                            protected void configure() {
                                bind(AppFrame.class).in(Scopes.SINGLETON);
                            }
                        });
                GuiceUtils.loadEagerSingletons(injector);
                injectorRef.set(injector); 
            }
        });
        
        Inspector inspector = injectorRef.get().getInstance(Inspector.class);
        inspector.setInjector(injectorRef.get());
        
        Map<Key<?>, Binding<?>> preBindings = injectorRef.get().getAllBindings();
        runInspectorTests(inspector, results);
        Map<Key<?>, Binding<?>> postBindings = injectorRef.get().getAllBindings();

        // We look at the difference in bindings before & after and fail if any
        // bindings were added.  The idea here is that only things that were singletons
        // should have been bound, so no new bindings should have been added.
        // If a new binding was added, something either wasn't a singleton,
        // wasn't bound, or was created twice!
        Map<Key<?>, Binding<?>> diff = new HashMap<Key<?>, Binding<?>>(postBindings);
        diff.keySet().removeAll(preBindings.keySet());
        assertTrue("added bindings: " + diff, diff.isEmpty());
    }
}

