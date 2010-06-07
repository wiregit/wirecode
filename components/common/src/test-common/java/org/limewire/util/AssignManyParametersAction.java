package org.limewire.util;

import java.util.concurrent.atomic.AtomicReference;

import org.jmock.api.Invocation;
import org.jmock.lib.action.CustomAction;

/**
 * JMock action that allows assignment of all parameters.
 */
public class AssignManyParametersAction extends CustomAction {

    private final AtomicReference[] references;

    public AssignManyParametersAction(AtomicReference... references) {
        super("assign");
        this.references = references;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Object invoke(Invocation invocation) throws Throwable {
        Object[] params = invocation.getParametersAsArray();
        if(params.length != references.length) {
            throw new RuntimeException("invalid parameter list, expected: " + references.length + " params, but got: " + params.length);
        }
        for(int i = 0; i < params.length; i++) {
            references[i].set(params[i]);
        }
        return null;
    }

}
