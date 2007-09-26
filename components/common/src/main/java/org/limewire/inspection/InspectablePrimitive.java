package org.limewire.inspection;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


/**
 * Defines the interface for an annotation of an object which wants to be 
 * inspected via {@link InspectionUtils#inspectValue(String)}.
 * The object must have a <code>String toString()</code> method.
 * <p>
 * See the Lime Wire Wiki for sample code using the <a href="http://www.limewire.org/wiki/index.php?title=Org.limewire.inspection">
 * org.limewire.inspection</a> package.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface InspectablePrimitive {
    String value() default ""; // TODO make value required
}