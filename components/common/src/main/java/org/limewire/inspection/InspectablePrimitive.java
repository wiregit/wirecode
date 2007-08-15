package org.limewire.inspection;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Annotation for fields that wish to be inspectable by {@link InspectionUtils}
 * through the <code>String.valueOf</code> method. 
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface InspectablePrimitive {}