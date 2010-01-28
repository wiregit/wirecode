package org.limewire.util;

import java.util.concurrent.atomic.AtomicReference;

import org.jmock.api.Invocation;
import org.jmock.lib.action.CustomAction;

/**
 * JMock action that allows assignment of the ith parameter of an invocation
 * to an atomic reference. 
 */
public class AssignParameterAction<T> extends CustomAction {

    private final AtomicReference<T> reference;
    private final int param;

    public AssignParameterAction(AtomicReference<T> reference, int param) {
        super("assign");
        this.reference = reference;
        this.param = param;
    }
    
    /**
     * Constructor to assign the first parameter of the invoation 
     * to <code>reference</code>. 
     */
    public AssignParameterAction(AtomicReference<T> reference) {
        this(reference, 0);
    }

    @SuppressWarnings("unchecked")
    @Override
    public Object invoke(Invocation invocation) throws Throwable {
        reference.set((T)invocation.getParameter(param));
        return null;
    }

}
