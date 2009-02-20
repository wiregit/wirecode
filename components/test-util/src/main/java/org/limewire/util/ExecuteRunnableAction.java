/**
 * 
 */
package org.limewire.util;

import org.jmock.api.Invocation;
import org.jmock.lib.action.CustomAction;

public class ExecuteRunnableAction extends CustomAction {
    public ExecuteRunnableAction() {
        super("Run a Runnable");
    }

    @Override
    public Object invoke(Invocation invocation) throws Throwable {
        Runnable runnable = (Runnable) invocation.getParameter(0);
        runnable.run();
        return null;
    }
}