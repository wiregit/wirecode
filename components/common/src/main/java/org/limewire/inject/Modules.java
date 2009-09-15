package org.limewire.inject;

import java.lang.annotation.Annotation;
import java.util.Map;
import java.util.logging.Logger;

import com.google.inject.Binder;
import com.google.inject.Binding;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.Module;
import com.google.inject.Scope;
import com.google.inject.Scopes;
import com.google.inject.Stage;
import com.google.inject.spi.BindingScopingVisitor;

public class Modules {
    
    private Modules() {}

    /**
     * Returns a module that returns all providers from the parent module.
     */
    public static Module providersFrom(final Injector parent) {
        return new Module() {
            @SuppressWarnings("unchecked")
            public void configure(Binder binder) {
                // These types cannot be rebound.
                Key loggerKey = Key.get(Logger.class);
                Key injectorKey = Key.get(Injector.class);
                Key stageKey = Key.get(Stage.class);
                
                for(Map.Entry<Key<?>, Binding<?>> entry : parent.getBindings().entrySet()) {
                    Key key = entry.getKey();
                    Binding<?> binding = entry.getValue();
                    // We need to properly scope the new bindings
                    // so that we can inspect the bindings later
                    // and make sure the scopes are proper.
                    BindingScopingVisitor<Scope> scoper = new BindingScopingVisitor<Scope>() {
                        @Override
                        public Scope visitEagerSingleton() {
                            return Scopes.SINGLETON;
                        }
                        @Override
                        public Scope visitNoScoping() {
                            return Scopes.NO_SCOPE;
                        }
                        @Override
                        public Scope visitScope(Scope scope) {
                            return scope;
                        }
                        @Override
                        public Scope visitScopeAnnotation(Class<? extends Annotation> scopeAnnotation) {
                            throw new IllegalStateException();
                        }
                    };
                    Scope scope = binding.acceptScopingVisitor(scoper);
                    if(!key.equals(loggerKey) && !key.equals(injectorKey) && !key.equals(stageKey)) {
                        binder.bind(key).toProvider(binding.getProvider()).in(scope) ;
                    }
                }
                
            }
        };
    }

}
