package org.limewire.inspection;

/**
 * Thrown upon a problem with inspection.
 * <p>
 * For example, {@link InspectionUtils#inspectValue(String)} must have two 
 * commas in the encoded field; otherwise, <code>InspectionUtils</code> throws 
 * <code>InspectionException</code>. Additionally, <code>InspectionUtils</code>
 * throws this exception if the inspection is called for an object which is not
 * {@link Inspectable}, or else the annotation is not an instance of 
 * {@link InspectablePrimitive @InspectablePrimitive}, or else the annotation is not an instance of 
 * {@link InspectableForSize @InspectableForSize}.
 */
public class InspectionException extends Exception {

    public InspectionException(){}
    public InspectionException(Throwable cause) {
        super(cause);
    }
}
