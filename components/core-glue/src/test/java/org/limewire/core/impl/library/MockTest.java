package org.limewire.core.impl.library;

import java.util.concurrent.atomic.AtomicReference;

import org.jmock.Mockery;
import org.jmock.api.Invocation;
import org.jmock.internal.InvocationExpectation;
import org.jmock.internal.matcher.MethodMatcher;
import org.jmock.lib.action.ReturnValueAction;
import org.jmock.lib.legacy.ClassImposteriser;
import org.limewire.util.BaseTestCase;

public class MockTest extends BaseTestCase{

    public MockTest(String name) {
        super(name);
    }

    /** 
     * Test what happens when a mockery gains critical mass
     */
    public void testJMockBlackhole() throws NoSuchMethodException {
        
        // Initialise
        Mockery context = new Mockery() {{setImposteriser(ClassImposteriser.INSTANCE);}};
        final AtomicReference<Mockery> contextReactor = new AtomicReference<Mockery>();
        contextReactor.set(context.mock(Mockery.class));
        
        // Set Exceptions
        InvocationExpectation expectation = new InvocationExpectation();
        expectation.setMethodMatcher(new MethodMatcher(Mockery.class.getMethod("mock", Class.class)));
        expectation.setAction(new ReturnValueAction(null) {
            @Override
            public Object invoke(Invocation invocation) {
                contextReactor.set(contextReactor.get().mock(Mockery.class));
                return contextReactor.get();
            }
        });
        context.addExpectation(expectation);
        
        // Kick off experiment
        contextReactor.set(contextReactor.get().mock(Mockery.class));
        
        // ???
        contextReactor.get().assertIsSatisfied();
    }
}
