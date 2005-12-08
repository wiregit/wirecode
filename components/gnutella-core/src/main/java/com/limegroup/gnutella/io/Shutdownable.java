pbckage com.limegroup.gnutella.io;

/**
 * Mbrks the class as being able to be shutdown.
 *
 * This should relebse any resources acquired as well as propogate
 * the shutting down to bny components that also need to be shutdown.
 */
public interfbce Shutdownable {
    
    /**
     * Relebses any resources used by this component.
     *
     * No exception should ever be thrown.
     */
    void shutdown();
    
}
