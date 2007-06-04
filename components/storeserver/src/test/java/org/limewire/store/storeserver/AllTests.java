package org.limewire.store.storeserver;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.StringTokenizer;

import org.limewire.store.storeserver.core.CookieGenTest;
import org.limewire.store.storeserver.core.CreateCookieDateTest;
import org.limewire.store.storeserver.local.CommunicationTest;
import org.limewire.store.storeserver.util.AddURLEncodedArgumentsTest;
import org.limewire.store.storeserver.util.GetIPAddressTest;
import org.limewire.store.storeserver.util.ParseArgsTest;
import org.limewire.store.storeserver.util.ParseHeaderTest;
import org.limewire.store.storeserver.util.RemoveCallbackTest;
import org.limewire.store.storeserver.util.TestUtil;
import org.limewire.util.LimeTestSuite;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * Runs all the tests.
 * 
 * @author jpalm
 */
public final class AllTests {

    private AllTests() {
    }

    public static Test suite() throws InstantiationException,
            IllegalAccessException {
        final TestSuite res = new TestSuite();
        final Class[] ts = { 
                AddURLEncodedArgumentsTest.class,
                CookieGenTest.class, 
                CreateCookieDateTest.class,
                GetIPAddressTest.class,
                ParseArgsTest.class, 
                ParseHeaderTest.class,
                RemoveCallbackTest.class, 
                CommunicationTest.class,
        };
        for (Class t : ts) res.addTest((TestCase) t.newInstance());
        return res;
    }

}
