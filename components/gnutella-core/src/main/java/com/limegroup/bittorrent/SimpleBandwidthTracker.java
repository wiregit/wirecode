
// Commented for the Learning branch

package com.limegroup.bittorrent;

import com.limegroup.gnutella.BandwidthTracker;

/**
 * A SimpleBandwidthTracker object measures the speed of data flow.
 * It keeps 2 speeds: the speed right now, and the total average speed.
 * 
 * Here's how to use a SimpleBandwidthTracker.
 * First, make one, s.
 * Regularly and repeatedly, call s.measureBandwidth().
 * This will keep its current speed up to date.
 * When you transfer some data, call s.count(n) to tell it how many bytes you transferred right now.
 * To get the current speed, call speed = s.getMeasuredBandwidth().
 * To get the total average speed, call speed = s.getAverageBandwidth().
 * 
 * Here's how SimpleBandwidthTracker works.
 * It keeps a current speed, _measuredBandwidth.
 * It has an internal interval, _interval, set by default at 3 seconds.
 * When you call measureBandwidth(), it only does something if the speed is older than 3 seconds.
 * If _measuredBandwidth is stale, it computes it again using just the most recent 3 seconds of data.
 * 
 * This class implements the BandwidthTracker interface.
 * BandwidthTracker requires the methods measureBandwidth(), getMeasuredBandwidth(), and getAverageBandwidth().
 */
public class SimpleBandwidthTracker implements BandwidthTracker {

	/** 3 seconds in milliseconds, by default, a SimpleBandwidthTracker will use at least 3 seconds of data to calculate its bandwidth. */
	private static final int DEFAULT_INTERVAL = 3 * 1000;

	/** Compute the current speed from the bytes we've transferred in the last _interval milliseconds. */
	private final int _interval;

	/** The total amount of distance we covered when we last computed the current speed. */
	private volatile long _lastAmount = 0;

	/** The time when we last computed the current speed. */
	private volatile long _lastTimeMeasured = 0;

	/** The time when we first computed our speed. */
	private volatile long _firstTimeMeasured = 0;

	/** The total amount of distance we've covered, in bytes. */
	private volatile long _amount = 0;

	/** The speed, in KB/s, that we most recently computed from at least _interval milliseconds of recent data. */
	private volatile float _measuredBandwidth = 0.f;

	/**
	 * Make a new SimpleBandwidthTracker object that can record how fast we're transferring data.
	 * The BTUploader, BTDownloader, and BTMessageReader constructors make new SimpleBandwidthTracker objects for themselves, and save them as _tracker.
	 */
	public SimpleBandwidthTracker() {

		// Call the next constructor, passing the default of 3 seconds
		this(DEFAULT_INTERVAL); // This new SimpleBandwidthTracker will use the distance covered in the last 3 seconds to compute its current speed
	}

	/**
	 * Make a new SimpleBandwidthTracker object that can record how fast we're transferring data.
	 * Only the constructor above calls this one.
	 * 
	 * @param interval Compute the current speed from the bytes we've transferred in the last _interval milliseconds
	 */
	public SimpleBandwidthTracker(int interval) {

		// Save the given interval
		_interval = interval;
	}

	/**
	 * Have this SimpleBandwidthTracker object record that we just transferred some more bytes right now.
	 * 
	 * @param added The number of bytes we just downloaded or uploaded
	 */
	public void count(int added) {

		// Add the given size to our total from the start
		_amount += added;
	}

	/**
	 * Find out how much data transferred this SimpleBandwidthTracker object has recorded from its start.
	 * 
	 * @return The total distance in bytes
	 */
	public long getTotalAmount() {

		// Return the total amount of data count() has added
		return _amount;
	}

	/**
	 * Have this SimpleBandwidthTracker object update the speed it keeps current.
	 * 
	 * When you make a SimpleBandwidthTracker object, call this measureBandwidth() method on it repeatedly.
	 * Once every 3 seconds, it will use data from the last 3 seconds to calculate the current speed.
	 * To find out what the speed is, call getMeasuredBandwidth().
	 */
	public void measureBandwidth() {

		// Get the time now, the number of milliseconds since 1970
		long now = System.currentTimeMillis();

		// If this is the first time measureBandwidth() is running on a new SimpleBandwidthTracker object
		if (_firstTimeMeasured == 0) {

			// Set the first and most recent times to now
			_lastTimeMeasured = _firstTimeMeasured = now;
			return;
		}

		// If we haven't waited long enough yet, leave
		if (now - _lastTimeMeasured < _interval) return; // The caller will call measureBandwidth() again when we have enough new data

		/*
		 * If control reaches here, we've waited at least 3 seconds since the last time.
		 */

		// Compute our current speed using data from the last 3 seconds or longer
		_measuredBandwidth = 1.f * (_amount - _lastAmount) / (now - _lastTimeMeasured);

		// Save the current distance and time for the next time we do this
		_lastAmount = _amount;
		_lastTimeMeasured = now;
	}

	/**
	 * Get the current speed this SimpleBandwidthTracker has calculated.
	 * Every 3 seconds, this SimpleBandwidthTracker calculates its current speed from the data it downloaded in the most recent 3 seconds.
	 * This method returns that speed.
	 * 
	 * @return The current speed, in KB/s.
	 *         0.0 if not known or just started.
	 */
	public float getMeasuredBandwidth() {

		// Return the bandwidth measureBandwidth() calculated from the last 3 seconds of data
		return _measuredBandwidth;
	}

	/**
	 * Get the total average bandwidth this SimpleBandwidthTracker object has recorded, from the very start of its record keeping.
	 * Computes the average speed by dividing the total distance by the total time.
	 * 
	 * @return The total average speed, in KB/s.
	 */
	public float getAverageBandwidth() {

		// Compute the speed from the total distance and total time
		return 1.f * _amount / (System.currentTimeMillis() - _firstTimeMeasured);
	}
}
