package com.limegroup.gnutella.statistics;

import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.StringTokenizer;

import com.limegroup.gnutella.ErrorService;
import com.limegroup.gnutella.util.IntBuffer;

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
	 * <tt>IntBuffer</tt> for recording stats data -- initialized to
     * an empty buffer until stats are actually recorded.
	 */
	protected final IntBuffer _buffer = new IntBuffer(HISTORY_LENGTH);

	/**
	 * Int for the statistic currently being added to.
	 */
	protected volatile int _current = 0;

	/**
	 * Int for the most recently stored statistic. 
	 */
	private volatile int _lastStored = 0;

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
	    if(_totalStatsRecorded == 0) return 0;
		return _total/_totalStatsRecorded;
	}

	// inherit doc comment
	public double getMax() {
		return _max;
	}

	public int getCurrent() {
		return _current;
	}
	
	
	public int getLastStored() {
		return _lastStored;
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
		synchronized(_buffer) {
			initializeBuffer();
			return _buffer;
		}
	}
	
	// inherit doc comment
	public void clearData() {
	    _current = 0;
	    _total = 0;
	    _totalStatsRecorded = 0;
	    _max = 0;
	    synchronized(_buffer) {
	        _buffer.clear();
	    }
	}

	// inherit doc comment
	public void storeCurrentStat() {
 		synchronized(_buffer) {
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
				    ErrorService.error(e);
				}
			}
		}
		_lastStored = _current;
		_current = 0;
		_totalStatsRecorded++;
	}
    
    public void storeStats(Writer writer) throws IOException {
        writer.write(Integer.toString(getCurrent()));
        writer.write("\t");
        writer.write(Double.toString(getTotal()));
        writer.write("\t");
        writer.write(Double.toString(getAverage()));
        writer.write("\t");
        writer.write(Double.toString(getMax()));
        writer.flush();
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
						List<Field> fieldsList = new LinkedList<Field>();
						if(superclass != null) {
							fieldsList.addAll(Arrays.asList(superclass.getFields()));
						}
						if(declaringClass != null) {
							fieldsList.addAll(Arrays.asList(declaringClass.getFields()));
						}
						fieldsList.addAll(Arrays.asList(getClass().getFields()));
						Field[] fields = fieldsList.toArray(new Field[0]);
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
				    ErrorService.error(e);
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
	protected final void initializeBuffer() {
		if(_buffer.isEmpty()) {
			for(int i=0; i<HISTORY_LENGTH; i++) {
				_buffer.addLast(0);
			}
		}
	}
}
