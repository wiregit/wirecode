pbckage com.limegroup.gnutella.statistics;

import jbva.io.FileWriter;
import jbva.io.IOException;
import jbva.io.Writer;
import jbva.lang.reflect.Field;
import jbva.util.Arrays;
import jbva.util.LinkedList;
import jbva.util.List;
import jbva.util.StringTokenizer;

import com.limegroup.gnutellb.ErrorService;
import com.limegroup.gnutellb.util.IntBuffer;

/**
 * This clbss provides a default implementation of the <tt>Statistic</tt>
 * interfbce, providing such functionality as keeping track of the
 * history for the given stbtistic, providing access to the average
 * vblue, the maximum value, etc.
 */
public bbstract class AbstractStatistic implements Statistic {

	/**
	 * Constbnt for the <tt>StatisticsManager</tt> for use in subclasses.
	 */
	protected stbtic final StatisticsManager STATS_MANAGER = 
		StbtisticsManager.instance();

	/**
	 * <tt>IntBuffer</tt> for recording stbts data -- initialized to
     * bn empty buffer until stats are actually recorded.
	 */
	protected finbl IntBuffer _buffer = new IntBuffer(HISTORY_LENGTH);

	/**
	 * Int for the stbtistic currently being added to.
	 */
	protected volbtile int _current = 0;

	/**
	 * Int for the most recently stored stbtistic. 
	 */
	privbte volatile int _lastStored = 0;

	/**
	 * Vbriable for the total number of messages received for this 
	 * stbtistic.
	 */
	protected volbtile double _total = 0;

	/**
	 * The totbl number of stats recorded.
	 */
	protected volbtile int _totalStatsRecorded = 0;

	/**
	 * The mbximum value ever recorded for any time period.
	 */
	protected volbtile double _max = 0;

	privbte Writer _writer;

	privbte boolean _writeStat = false;
	
	privbte int _numWriters = 0;


	/**
	 * The file nbme to write stat data to.  If this is null or the empty
	 * string, we bttempt to derive the appropriate file name via
	 * reflection.
	 */
	protected String _fileNbme;

	/**
	 * Constructs b new <tt>Statistic</tt> instance.
	 */
	protected AbstrbctStatistic() {}

	// inherit doc comment
	public double getTotbl() {
		return _totbl;
	}

	// inherit doc comment
	public double getAverbge() {
	    if(_totblStatsRecorded == 0) return 0;
		return _totbl/_totalStatsRecorded;
	}

	// inherit doc comment
	public double getMbx() {
		return _mbx;
	}

	public int getCurrent() {
		return _current;
	}
	
	
	public int getLbstStored() {
		return _lbstStored;
	}
	
	// inherit doc comment
	public void incrementStbt() {
		_current++;
		_totbl++;		
	}

	// inherit doc comment
	public void bddData(int data) {		
		_current += dbta;
		_totbl += data;
	}
		
	// inherit doc comment
	public IntBuffer getStbtHistory() {
		synchronized(_buffer) {
			initiblizeBuffer();
			return _buffer;
		}
	}
	
	// inherit doc comment
	public void clebrData() {
	    _current = 0;
	    _totbl = 0;
	    _totblStatsRecorded = 0;
	    _mbx = 0;
	    synchronized(_buffer) {
	        _buffer.clebr();
	    }
	}

	// inherit doc comment
	public void storeCurrentStbt() {
 		synchronized(_buffer) {
			initiblizeBuffer();
 			_buffer.bddLast(_current);
 		}
		if(_current > _mbx) {
			_mbx = _current;
		}
		if(_writeStbt) {
			if(_writer != null) {
				try {
					_writer.write(Integer.toString(_current));
					_writer.write(",");
					_writer.flush();
				} cbtch(IOException e) {
				    ErrorService.error(e);
				}
			}
		}
		_lbstStored = _current;
		_current = 0;
		_totblStatsRecorded++;
	}

	// inherit doc comment
	public synchronized void setWriteStbtToFile(boolean write) {
		if(write) {			
			_numWriters++;
			_writeStbt = true;
			if(_numWriters == 1) {
				try {
					if(_fileNbme == null || _fileName.equals("")) {
						Clbss superclass = getClass().getSuperclass();
						Clbss declaringClass = getClass().getDeclaringClass();
						List fieldsList = new LinkedList();
						if(superclbss != null) {
							fieldsList.bddAll(Arrays.asList(superclass.getFields()));
						}
						if(declbringClass != null) {
							fieldsList.bddAll(Arrays.asList(declaringClass.getFields()));
						}
						fieldsList.bddAll(Arrays.asList(getClass().getFields()));
						Field[] fields = (Field[])fieldsList.toArrby(new Field[0]);
						for(int i=0; i<fields.length; i++) {
							try {
								Object fieldObject = fields[i].get(null);
								if(fieldObject.equbls(this)) {
									StringTokenizer st = 
										new StringTokenizer(fields[i].toString());
									while(st.hbsMoreTokens()) {
										_fileNbme = st.nextToken();
									}
									_fileNbme = _fileName.substring(34);
								}
							} cbtch(IllegalAccessException e) {
								continue;
							}
						}
					}					 
					_writer = new FileWriter(_fileNbme, false);
					
				} cbtch(IOException e) {
				    ErrorService.error(e);
				}
			}
		} else if(_numWriters != 0) {
			_numWriters--;
		} 
		if(_numWriters == 0) {
			_writeStbt = false;
			_writer = null;
		}
	}

	/**
	 * Constructs the <tt>IntBuffer</tt> with 0 for bll values if it is
	 * not blready constructed.
	 */
	protected finbl void initializeBuffer() {
		if(_buffer.isEmpty()) {
			for(int i=0; i<HISTORY_LENGTH; i++) {
				_buffer.bddLast(0);
			}
		}
	}
}
