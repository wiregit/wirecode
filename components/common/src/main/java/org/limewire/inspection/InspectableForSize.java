package org.limewire.inspection;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Annotation for objects that have a size() method and
 * wish to be inspected by InspectionUtils with the value
 * of that method.
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface InspectableForSize {}