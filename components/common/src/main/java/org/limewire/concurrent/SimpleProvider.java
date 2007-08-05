package org.limewire.concurrent;

import com.google.inject.Provider;

/** A simple provider that always returns T. */
public class SimpleProvider<T> implements Provider<T> {
    private final T t;

    public SimpleProvider(T t) {
        this.t = t;
    }

    public T get() {
        return t;
    }
    
    public static <T> Provider<T> of(T instance) {
        return new SimpleProvider<T>(instance);
    }

}
