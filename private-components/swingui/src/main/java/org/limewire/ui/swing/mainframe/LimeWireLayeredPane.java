package org.limewire.ui.swing.mainframe;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;

import com.google.inject.BindingAnnotation;

/**
 * Annotation allowing injection of a the limewire layered pane.
 */
@BindingAnnotation
@Target( { FIELD, PARAMETER, METHOD })
@Retention(RUNTIME)
public @interface LimeWireLayeredPane {

}