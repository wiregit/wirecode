package org.limewire.inspection;

import java.util.concurrent.atomic.AtomicReference;

import org.limewire.ui.swing.util.SwingUtils;

public abstract class SwingInspectable implements Inspectable {
    @Override
    public Object inspect() {
        final AtomicReference<Object> result = new AtomicReference<Object>();
        SwingUtils.invokeNowOrWait(new Runnable() {
            @Override
            public void run() {
                result.set(inspectOnEDT());
            }
        });
        return result;
    }
    
    protected abstract Object inspectOnEDT();
}
