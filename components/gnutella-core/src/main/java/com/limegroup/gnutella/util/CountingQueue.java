package com.limegroup.gnutella.util;

import java.util.LinkedList;


/**
 *
 * A simple FIFO queue that can report its average size as defined
 * by Little's law - averageTimeInQueue * arrivalRate.
 */
public class CountingQueue<Element> extends LinkedList<Element> {

	private NumericBuffer<Long> timesInQueue;
	private LinkedList<Long> arrivalTimes;
	private long timeStart;

	/**
	 * @param historySize how many elements of history to record
	 */
	public CountingQueue(int historySize) {
		super();
		timesInQueue = new NumericBuffer<Long>(historySize);
		arrivalTimes = new LinkedList<Long>();
	}
	
	public boolean offer(Element el) {
		long now = System.currentTimeMillis();
		if (timeStart == 0)
			timeStart = now;
		arrivalTimes.add(now);
		return super.add(el);
	}
	
	public Element poll() {
		Element ret = super.poll();
		if (ret != null)
			countDeparture();
		return ret;
	}
	
	public Element remove() {
		Element ret = super.remove();
		countDeparture();
		return ret;
	}
	
	private void countDeparture() {
		long now = System.currentTimeMillis();
		timeStart = arrivalTimes.removeFirst();
		timesInQueue.add(now - timeStart);
	}
	
	/**
	 * @return the average number of elements in the queue
	 */
	public double getAverageSize() {
		if (timesInQueue.size() < timesInQueue.getCapacity())
			return -1;
		
		double rate = ((double)timesInQueue.getCapacity()) / 
		(Math.max(1,System.currentTimeMillis() - timeStart));
		
		return timesInQueue.average() * rate;
	}
}
