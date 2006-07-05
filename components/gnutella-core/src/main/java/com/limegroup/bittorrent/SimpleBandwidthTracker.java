package com.limegroup.bittorrent;

import com.limegroup.gnutella.BandwidthTracker;
import com.limegroup.gnutella.InsufficientDataException;

/**
 * A simple implementation of the BandwidthTracker interface
 */
public class SimpleBandwidthTracker implements BandwidthTracker {
	private static final int DEFAULT_INTERVAL = 500;

	private final int _interval;

	private volatile long _lastAmount = 0;

	private volatile long _lastTimeMeasured = 0;

	private volatile long _firstTimeMeasured = 0;

	private volatile long _amount = 0;

	private volatile float _measuredBandwidth = -1f;

	public SimpleBandwidthTracker() {
		this(DEFAULT_INTERVAL);
	}

	public SimpleBandwidthTracker(int interval) {
		_interval = interval;
	}

	public void count(int added) {
		_amount += added;
	}

	public long getTotalAmount() {
		return _amount;
	}

	public void measureBandwidth() {
		long now = System.currentTimeMillis();
		if (_firstTimeMeasured == 0) {
			_firstTimeMeasured = now;
			_lastTimeMeasured = _firstTimeMeasured;
		}
		
		if (now - _lastTimeMeasured < _interval)
			return;

		_measuredBandwidth = (_amount - _lastAmount)
				/ (now - _lastTimeMeasured) ;
		_lastAmount = _amount;
		_lastTimeMeasured = now;
	}

	public float getMeasuredBandwidth() throws InsufficientDataException {
		if (_measuredBandwidth < 0)
			throw new InsufficientDataException();
		return _measuredBandwidth;
	}

	public float getAverageBandwidth() {
		return _amount
				/ (System.currentTimeMillis() - _firstTimeMeasured);
	}
}
