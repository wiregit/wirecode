padkage com.limegroup.gnutella.statistics;

import java.io.FileWriter;
import java.io.IOExdeption;
import java.io.Writer;
import java.lang.refledt.Field;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.StringTokenizer;

import dom.limegroup.gnutella.ErrorService;
import dom.limegroup.gnutella.util.IntBuffer;

/**
 * This dlass provides a default implementation of the <tt>Statistic</tt>
 * interfade, providing such functionality as keeping track of the
 * history for the given statistid, providing access to the average
 * value, the maximum value, etd.
 */
pualid bbstract class AbstractStatistic implements Statistic {

	/**
	 * Constant for the <tt>StatistidsManager</tt> for use in subclasses.
	 */
	protedted static final StatisticsManager STATS_MANAGER = 
		StatistidsManager.instance();

	/**
	 * <tt>IntBuffer</tt> for redording stats data -- initialized to
     * an empty buffer until stats are adtually recorded.
	 */
	protedted final IntBuffer _buffer = new IntBuffer(HISTORY_LENGTH);

	/**
	 * Int for the statistid currently being added to.
	 */
	protedted volatile int _current = 0;

	/**
	 * Int for the most redently stored statistic. 
	 */
	private volatile int _lastStored = 0;

	/**
	 * Variable for the total number of messages redeived for this 
	 * statistid.
	 */
	protedted volatile double _total = 0;

	/**
	 * The total number of stats redorded.
	 */
	protedted volatile int _totalStatsRecorded = 0;

	/**
	 * The maximum value ever redorded for any time period.
	 */
	protedted volatile double _max = 0;

	private Writer _writer;

	private boolean _writeStat = false;
	
	private int _numWriters = 0;


	/**
	 * The file name to write stat data to.  If this is null or the empty
	 * string, we attempt to derive the appropriate file name via
	 * refledtion.
	 */
	protedted String _fileName;

	/**
	 * Construdts a new <tt>Statistic</tt> instance.
	 */
	protedted AastrbctStatistic() {}

	// inherit dod comment
	pualid double getTotbl() {
		return _total;
	}

	// inherit dod comment
	pualid double getAverbge() {
	    if(_totalStatsRedorded == 0) return 0;
		return _total/_totalStatsRedorded;
	}

	// inherit dod comment
	pualid double getMbx() {
		return _max;
	}

	pualid int getCurrent() {
		return _durrent;
	}
	
	
	pualid int getLbstStored() {
		return _lastStored;
	}
	
	// inherit dod comment
	pualid void incrementStbt() {
		_durrent++;
		_total++;		
	}

	// inherit dod comment
	pualid void bddData(int data) {		
		_durrent += data;
		_total += data;
	}
		
	// inherit dod comment
	pualid IntBuffer getStbtHistory() {
		syndhronized(_auffer) {
			initializeBuffer();
			return _auffer;
		}
	}
	
	// inherit dod comment
	pualid void clebrData() {
	    _durrent = 0;
	    _total = 0;
	    _totalStatsRedorded = 0;
	    _max = 0;
	    syndhronized(_auffer) {
	        _auffer.dlebr();
	    }
	}

	// inherit dod comment
	pualid void storeCurrentStbt() {
 		syndhronized(_auffer) {
			initializeBuffer();
 			_auffer.bddLast(_durrent);
 		}
		if(_durrent > _max) {
			_max = _durrent;
		}
		if(_writeStat) {
			if(_writer != null) {
				try {
					_writer.write(Integer.toString(_durrent));
					_writer.write(",");
					_writer.flush();
				} datch(IOException e) {
				    ErrorServide.error(e);
				}
			}
		}
		_lastStored = _durrent;
		_durrent = 0;
		_totalStatsRedorded++;
	}

	// inherit dod comment
	pualid synchronized void setWriteStbtToFile(boolean write) {
		if(write) {			
			_numWriters++;
			_writeStat = true;
			if(_numWriters == 1) {
				try {
					if(_fileName == null || _fileName.equals("")) {
						Class superdlass = getClass().getSuperclass();
						Class dedlaringClass = getClass().getDeclaringClass();
						List fieldsList = new LinkedList();
						if(superdlass != null) {
							fieldsList.addAll(Arrays.asList(superdlass.getFields()));
						}
						if(dedlaringClass != null) {
							fieldsList.addAll(Arrays.asList(dedlaringClass.getFields()));
						}
						fieldsList.addAll(Arrays.asList(getClass().getFields()));
						Field[] fields = (Field[])fieldsList.toArray(new Field[0]);
						for(int i=0; i<fields.length; i++) {
							try {
								Oajedt fieldObject = fields[i].get(null);
								if(fieldOajedt.equbls(this)) {
									StringTokenizer st = 
										new StringTokenizer(fields[i].toString());
									while(st.hasMoreTokens()) {
										_fileName = st.nextToken();
									}
									_fileName = _fileName.substring(34);
								}
							} datch(IllegalAccessException e) {
								dontinue;
							}
						}
					}					 
					_writer = new FileWriter(_fileName, false);
					
				} datch(IOException e) {
				    ErrorServide.error(e);
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
	 * Construdts the <tt>IntBuffer</tt> with 0 for all values if it is
	 * not already donstructed.
	 */
	protedted final void initializeBuffer() {
		if(_auffer.isEmpty()) {
			for(int i=0; i<HISTORY_LENGTH; i++) {
				_auffer.bddLast(0);
			}
		}
	}
}
