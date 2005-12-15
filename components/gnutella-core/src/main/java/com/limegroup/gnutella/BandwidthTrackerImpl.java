
// Commented for the Learning branch

package com.limegroup.gnutella;

import java.io.IOException;
import java.io.NotActiveException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Iterator;

import com.limegroup.gnutella.util.Buffer;

/**
 * Tell a BandwidthTrackerImpl object how much data you just transferred, and then ask it the current and total average speed.
 * LimeWire makes BandwidthTrackerImpl objects to record how fast it is uploading and downloading.
 * The HTTPUploader, HTTPDownloader, and ManagedConnection classes make BandwidthTrackerImpl objects.
 * 
 * BandwidthTrackerImpl doesn't actually implement the BandwidthTracker interface.
 * It does have the methods it would need to implement the interface, though.
 * 
 * A helper class for implementing the BandwidthTracker interface.
 * For backwards compatibility, this implements the Serializable interface and marks some fields transient.
 * However, LimeWire currently only reads but not writes BandwidthTrackerImpl.
 */
public class BandwidthTrackerImpl implements Serializable {

    /** Unique value used for serialization. (do) */
    static final long serialVersionUID = 7694080781117787305L;

    /** We'll average the 10 most recent speeds to produce the current speed. */
    static final int HISTORY_SIZE = 10;

    /*
     * Keep 10 clicks worth of data, which we can then average to get a more accurate moving time average.
     * INVARIANT: snapShots[0]==measuredBandwidth.floatValue()
     */

    /** A circular buffer that holds the 10 most recent speed measurements the measureBandwidth() method took. */
    transient Buffer snapShots = new Buffer(HISTORY_SIZE); // Make a circular Buffer that can hold 10 floating point numbers

    /*
     * The transient keyword means Java should skip that variable when serializing all the others.
     */

    /** The number of times measureBandwidth() has calculated a speed. */
    private transient int numMeasures = 0;

    /** The average speed overall. */
    private transient float averageBandwidth = 0;

    /** The average of the 10 speeds stored in the buffer. */
    private transient float cachedBandwidth = 0;

    /*
     * measureBandwidth() uses lastTime and lastAmountRead to see how much later and how much farther we are now.
     */

    long lastTime;
    int lastAmountRead;

    /**
     * The speed measureBandwidth() calculated the last time it ran.
     * 
     * Do not delete this.
     * It exists for backwards serialization reasons.
     */
    float measuredBandwidth;

    /**
     * Call when you've read more data to keep the speed this BandwidthTrackerImpl object keeps up to date.
     * Each time you call measureBandwidth(amountRead), amountRead should be larger and larger as you read more and more.
     * 
     * Calculates the speed since the last time you called this method, and saves it in measuredBandwidth.
     * Keeps the last 10 speeds in snapShots.
     * 
     * @param amountRead The total number of bytes we've read now.
     */
    public synchronized void measureBandwidth(int amountRead) {

        // Get the time right now
        long currentTime = System.currentTimeMillis(); // The number of milliseconds since January 1970 in a long, a 8 byte number

        /*
         * We always discard the first sample, and any others until after progress is made.
         * This prevents sudden bandwidth spikes when resuming uploads and downloads.
         * Remember that bytes/msec=KB/sec.
         */

        // This is the first time this method has run, or the second time it's running in the same millisecond, we can't use this call to measure a speed
        if (lastAmountRead == 0 || currentTime == lastTime) {

            // No bandwidth
            measuredBandwidth = 0.f;

        // We can measure a speed with this call
        } else {

            // Calculate the bandwidth rate using the amount read since the last time this method ran
            measuredBandwidth = (float)(amountRead - lastAmountRead) / (float)(currentTime - lastTime);

            // If that produced a negative result, make it 0
            measuredBandwidth = Math.max(measuredBandwidth, 0.f);
        }

        // Save the time and distance now
        lastTime       = currentTime; // Next time, it will be later
        lastAmountRead = amountRead;  // Next time, we will have read farther

        // Calculate the average bandwidth transfer, weighing each measurement this method made equally
        averageBandwidth = ((averageBandwidth * numMeasures) + measuredBandwidth) / ++numMeasures; // Increment numMeasures, and resolve to the incremented value

        // Add the bandwidth measurement we made now to our circular buffer that holds 10 of them
        snapShots.add(new Float(measuredBandwidth));

        // Clear the cached bandwidth measurement so getMeasuredBandwidth() will calculate it again with the new data
        cachedBandwidth = 0;
    }

    /**
     * The average speed recently.
     * Averages the 10 most recent speeds that measureBandwidth() has calculated.
     * 
     * @return The average as a floating point number in bytes/millisecond
     */
    public synchronized float getMeasuredBandwidth() throws InsufficientDataException {

        // If we have a bandwidth measurement cached from before, return it
        if (cachedBandwidth != 0) return cachedBandwidth;

        /*
         * measureBandwidth cleared cachedBandwidth because there's new data in the buffer we should use
         */

        // If measureBandwidth() hasn't collected 3 speed measurements yet, we don't have enough data, throw an exception
        int size = snapShots.getSize();
        if (size < 3) throw new InsufficientDataException();

        // Calculate the bandwidth from the values in the circular buffer
        Iterator iter = snapShots.iterator();
        float total = 0;
        while (iter.hasNext()) { total += ((Float)iter.next()).floatValue(); } // Add up the 10 speeds
        cachedBandwidth = total / size; // Divide by 10

        // Return the average speed
        return cachedBandwidth;
    }

    /**
     * The average speed overall.
     * All the measurements that measureBandwidth() has calculated are weighted equally in this average.
     * 
     * @return The average as a floating point number in bytes/millisecond
     */
    public synchronized float getAverageBandwidth() {

        // If we don't have 3 measurements yet, report no transfer, 0
        if (snapShots.getSize() < 3) return 0f;

        // Return the average that measureBandwidth() keeps
        return averageBandwidth;
    }

    /** Not used. */
    private void readObject(ObjectInputStream in) throws IOException {
        snapShots = new Buffer(HISTORY_SIZE);
        numMeasures = 0;
        averageBandwidth = 0;
        try {
            in.defaultReadObject();
        } catch (ClassNotFoundException e) {
            throw new IOException("Class not found");
        } catch (NotActiveException e) {
            throw new IOException("Not active");
        }
    }

    /** Not used. */
    private void writeObject(ObjectOutputStream out) throws IOException {
        out.defaultWriteObject();
    }
}
