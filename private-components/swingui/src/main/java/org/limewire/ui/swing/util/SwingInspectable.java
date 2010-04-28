package org.limewire.ui.swing.util;

import java.util.concurrent.atomic.AtomicReference;

import org.limewire.inspection.Inspectable;

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
        return result.get();
    }
    
    protected abstract Object inspectOnEDT();
}
