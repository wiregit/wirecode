package org.limewire.inject;

import com.google.inject.Binder;
import com.google.inject.Module;

public class Modules {
    
    private Modules() {}
    
    public static Module combine(final Module... modules) {
        return new Module() {
            public void configure(Binder binder) {
                for(Module module : modules) {
                    binder.install(module);
                }
            }
        };
    }

}
