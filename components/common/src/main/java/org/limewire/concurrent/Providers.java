package org.limewire.concurrent;

import com.google.inject.Provider;

public class Providers {

    public static <T> Provider<T> of(T instance) {
        return new SimpleProvider<T>(instance);
    }

}
