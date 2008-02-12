package com.limegroup.bittorrent.tracking;


public interface Tracker {    
    /*
     * possible EVENT codes for the tracker protocol
     */
    public enum Event {
        START (100, "started"),
        STOP (0, "stopped"), 
        COMPLETE (20, "completed"), 
        NONE (50, null);
        
        private final String numWant;
        private final String description;
        Event(int numWant, String description) {
            this.numWant = numWant > 0 ? Integer.toString(numWant) : null;
            this.description = description;
        }
        public final String getNumWant() {
            return numWant;
        }
        public final String getDescription() {
            return description;
        }
    }
    /**
     * Notifies the tracker that a request to it failed.
     * @return how many times it had failed previously.
     */
    public abstract void recordFailure();

    /**
     * Notifies the tracker that a request completed successfully.
     */
    public abstract void recordSuccess();

    /**
     * @return how many consecutive failures we have for this
     * tracker.
     */
    public abstract int getFailures();

    /**
     * Does a tracker request for a certain event code
     * 
     * @param event
     *            the event code to send to the tracker
     * @return TrackerResponse holding the data the tracker sent or null if the
     *         tracker did not send any data
     */
    public abstract TrackerResponse request(Event event);

    public abstract String toString();
}