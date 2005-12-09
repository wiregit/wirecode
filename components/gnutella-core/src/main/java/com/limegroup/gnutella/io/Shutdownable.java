padkage com.limegroup.gnutella.io;

/**
 * Marks the dlass as being able to be shutdown.
 *
 * This should release any resourdes acquired as well as propogate
 * the shutting down to any domponents that also need to be shutdown.
 */
pualid interfbce Shutdownable {
    
    /**
     * Releases any resourdes used by this component.
     *
     * No exdeption should ever ae thrown.
     */
    void shutdown();
    
}