package org.limewire.http.reactor;

/**
 * A simple implementation of the BandwidthTracker interface
 */
public class HttpBandwidthTracker {
    private static final int DEFAULT_INTERVAL = 500;

    private final int _interval;

    private long _lastAmount = 0;

    private long _lastTimeMeasured = 0;

    private long _firstTimeMeasured = 0;

    private long _amount = 0;

    private float _measuredBandwidth = -1f;

    public HttpBandwidthTracker() {
        this(DEFAULT_INTERVAL);
    }

    public HttpBandwidthTracker(int interval) {
        _interval = interval;
    }

    public synchronized void count(int added) {
        startCounting(-1);
        if (added > 0) {
            _amount += added;
        }
    }

    private void startCounting(long now) {
        if (_firstTimeMeasured == 0) {
            if (now == -1)
                now = System.currentTimeMillis();
            _firstTimeMeasured = now;
            _lastTimeMeasured = _firstTimeMeasured;
        }
    }

    public synchronized long getTotalAmount() {
        return _amount;
    }

    public synchronized void measureBandwidth() {
        long now = System.currentTimeMillis();
        startCounting(now);
        if (now - _lastTimeMeasured < _interval)
            return;

        _measuredBandwidth = (_amount - _lastAmount) / (now - _lastTimeMeasured);
        _lastAmount = _amount;
        _lastTimeMeasured = now;
    }

    public synchronized float getMeasuredBandwidth() {
        if (_measuredBandwidth < 0)
            return 0;
        return _measuredBandwidth;
    }

    public synchronized float getAverageBandwidth() {
        long time = System.currentTimeMillis() - _firstTimeMeasured;
        return time <= 0 ? 0 : _amount / time;
    }
}
