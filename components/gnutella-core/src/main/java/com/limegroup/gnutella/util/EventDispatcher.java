package com.limegroup.gnutella.util;

import java.util.EventListener;
import java.util.EventObject;

public interface EventDispatcher<T extends EventObject, Y extends EventListener> {
	public void dispatchEvent(T event);
	public void addEventListener(Y listener);
	public void removeEventListener(Y listener);
}
