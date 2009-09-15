package org.limewire.xmpp.client.impl;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.google.inject.BindingAnnotation;

/**
 * Settings annotation whether to advertise the fact that you're logged on
 * to XMPP with LimeWire in the status text.
 */
@BindingAnnotation
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.METHOD}) 
@Retention(RetentionPolicy.RUNTIME)
public @interface XMPPAdvertiseLimeWireStatus {
}
