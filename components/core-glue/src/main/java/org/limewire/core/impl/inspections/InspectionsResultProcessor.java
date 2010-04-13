package org.limewire.core.impl.inspections;

/**
 * Used to specify what to do with inspection results
 */
public interface InspectionsResultProcessor {

    /**
     * Process inspection results
     * 
     * @param insps inspection results
     */
    public void inspectionsPerformed(InspectionsSpec spec, InspectionDataContainer insps);
    

    /**
     * Called when {@link InspectionsCommunicator} service has been stopped
     */
    public void stopped();
}
