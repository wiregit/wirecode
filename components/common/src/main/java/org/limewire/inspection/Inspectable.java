package org.limewire.inspection;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

public interface Inspectable {
    @Retention(RetentionPolicy.RUNTIME)
    public @interface InspectablePrimitive {}
    @Retention(RetentionPolicy.RUNTIME)
    public @interface InspectableForSize {}

    public String inspect();
}
