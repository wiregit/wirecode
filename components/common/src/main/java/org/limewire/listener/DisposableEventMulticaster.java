package org.limewire.listener;

import org.limewire.common.Disposable;

/** An event multicaster that must be disposed. */
public interface DisposableEventMulticaster<E> extends EventMulticaster<E>, Disposable {

}
