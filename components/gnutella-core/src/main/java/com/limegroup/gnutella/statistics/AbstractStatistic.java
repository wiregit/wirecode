package com.limegroup.gnutella.statistics;

import com.limegroup.gnutella.util.*;
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
	 * Constant for the <tt>StatisticsManager</tt> for use in subclasses.
	 */
	protected static final StatisticsManager STATS_MANAGER = 
		StatisticsManager.instance();

	/**
	 * <tt>IntBuffer</tt> for recording stats data.
	 */
	private IntBuffer _buffer;

	/**
	 * Long for the statistic currently being added to.
	 */
	protected volatile int _current = 0;

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
	 * Lock for accessing the <tt>IntBuffer</tt>.
	 */
	private final Object BUFFER_LOCK = new Object(); 

	/**
	 * The file name to write stat data to.  If this is null or the empty
	 * string, we attempt to derive the appropriate file name via
	 * reflection.
	 */
	protected String _fileName;

	/**
	 * Constructs a new <tt>Statistic</tt> instance.
	 */
	protected AbstractStatistic() {}

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
	public IntBuffer getStatHistory() {
		synchronized(BUFFER_LOCK) {
			initializeBuffer();
			return _buffer;
		}
	}

	// inherit doc comment
	public void storeCurrentStat() {
 		synchronized(BUFFER_LOCK) {
			initializeBuffer();
 			_buffer.addLast(_current);
 		}
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
					if(_fileName == null || _fileName.equals("")) {
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
						for(int i=0; i<fields.length; i++) {
							try {
								Object fieldObject = fields[i].get(null);
								if(fieldObject.equals(this)) {
									StringTokenizer st = 
										new StringTokenizer(fields[i].toString());
									while(st.hasMoreTokens()) {
										_fileName = st.nextToken();
									}
									_fileName = _fileName.substring(34);
								}
							} catch(IllegalAccessException e) {
								continue;
							}
						}
					}					 
					_writer = new FileWriter(_fileName, false);
					
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

	/**
	 * Constructs the <tt>IntBuffer</tt> with 0 for all values if it is
	 * not already constructed.
	 */
	private void initializeBuffer() {
		if(_buffer == null) {
			_buffer = new IntBuffer(HISTORY_LENGTH);
			for(int i=0; i<HISTORY_LENGTH; i++) {
				_buffer.addLast(0);
			}
		}
	}
}
