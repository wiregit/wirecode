package com.limegroup.gnutella.statistics;

import com.sun.java.util.collections.*;
import java.io.*;
import java.lang.reflect.*;
import java.util.StringTokenizer;

/**
 * This class provides a default implementation of the <tt>Statistic</tt>
 * interface, providing such functionality as keeping track of the
 * history for the given statistic, providing access to the average
 * value, the maximum value, etc.
 */
public abstract class AbstractStatistic implements Statistic {

	/**
	 * List of all statistics stored over intervals for this
	 * specific <tt>Statistic</tt> instance.
	 */
	private final List STAT_HISTORY = new LinkedList();

	/**
	 * Long for the statistic currently being added to.
	 */
	protected volatile int _current = 0;

	/**
	 * Variable for the array of <tt>Integer</tt> instances for the
	 * history of statistics for this message.  Each 
	 * <tt>Integer</tt> stores the statistic for one time interval.
	 */
	private volatile Integer[] _statHistory;

	/**
	 * Variable for the total number of messages received for this 
	 * statistic.
	 */
	protected volatile double _total = 0;

	/**
	 * The total number of stats recorded.
	 */
	protected volatile int _totalStatsRecorded = 0;

	/**
	 * The maximum value ever recorded for any time period.
	 */
	protected volatile double _max = 0;

	private Writer _writer;

	private boolean _writeStat = false;
	
	private int _numWriters = 0;

	/**
	 * Constructs a new <tt>Statistic</tt> instance with 0 for all 
	 * historical data fields.
	 */
	protected AbstractStatistic() {
		for(int i=0; i<HISTORY_LENGTH; i++) {
			STAT_HISTORY.add(new Integer(0));
		}			
	}

	// inherit doc comment
	public double getTotal() {
		return _total;
	}

	// inherit doc comment
	public double getAverage() {
		return _total/_totalStatsRecorded;
	}

	// inherit doc comment
	public double getMax() {
		return _max;
	}

	// inherit doc comment
	public void incrementStat() {
		_current++;
		_total++;		
	}

	// inherit doc comment
	public void addData(int data) {
		_current += data;
		_total += data;
	}
		
	// inherit doc comment
	public Integer[] getStatHistory() {
		_statHistory = (Integer[])STAT_HISTORY.toArray(new Integer[0]); 
		return _statHistory;
	}

	// inherit doc comment
	public void storeCurrentStat() {
		STAT_HISTORY.remove(0);
		STAT_HISTORY.add(new Integer(_current));
		if(_current > _max) {
			_max = _current;
		}
		if(_writeStat) {
			if(_writer != null) {
				try {
					_writer.write(Integer.toString(_current));
					_writer.write(",");
					_writer.flush();
				} catch(IOException e) {
					e.printStackTrace();
					// not much to do
				}
			}
		}
		_current = 0;
		_totalStatsRecorded++;
	}

	// inherit doc comment
	public synchronized void setWriteStatToFile(boolean write) {
		if(write) {
			_numWriters++;
			_writeStat = true;
			if(_numWriters == 1) {
				try {
					Class superclass = getClass().getSuperclass();
					Class declaringClass = getClass().getDeclaringClass();
					List fieldsList = new LinkedList();
					if(superclass != null) {
						fieldsList.addAll(Arrays.asList(superclass.getFields()));
					}
					if(declaringClass != null) {
						fieldsList.addAll(Arrays.asList(declaringClass.getFields()));
					}
					fieldsList.addAll(Arrays.asList(getClass().getFields()));
					Field[] fields = (Field[])fieldsList.toArray(new Field[0]);
					String name = "";
					for(int i=0; i<fields.length; i++) {
						try {
							Object fieldObject = fields[i].get(null);
							if(fieldObject.equals(this)) {
								StringTokenizer st = 
							        new StringTokenizer(fields[i].toString());
								while(st.hasMoreTokens()) {
									name = st.nextToken();
								}
								name = name.substring(34);
							}
						} catch(IllegalAccessException e) {
							continue;
						}
					}
					_writer = new FileWriter(name, false);
				} catch(IOException e) {
					e.printStackTrace();
				}
			}
		} else if(_numWriters != 0) {
			_numWriters--;
		} 
		if(_numWriters == 0) {
			_writeStat = false;
			_writer = null;
		}
	}
}
