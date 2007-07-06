package org.limewire.inspection;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Defines the interface for an annotation of an object which wants to be 
 * inspected via {@link InspectionUtils#inspectValue(String)}.
 * The object must have a <code>String toString()</code> method.
 * <p>
 * See the Lime Wire Wiki for sample code using the <a href="https://www.limewire.org/wiki/index.php?title=Package_org.limewire.inspection%3B">
 * org.limewire.inspection</a> package.
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface InspectablePrimitive {}