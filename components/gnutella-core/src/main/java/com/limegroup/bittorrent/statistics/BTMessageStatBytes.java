package com.limegroup.bittorrent.statistics;

//TODO: move the non-gnutella specific statistics code out of gnutella package
import com.limegroup.gnutella.statistics.AdvancedKilobytesStatistic;
import com.limegroup.gnutella.statistics.Statistic;

/**
 * statistics for incoming and outgoing BT messages
 */
public class BTMessageStatBytes extends AdvancedKilobytesStatistic {
	private BTMessageStatBytes() {
		// private constructor
	}
	/**
	 * <tt>Statistic</tt> for incoming keep alive messages
	 */
	public static final Statistic INCOMING_KEEP_ALIVE =
		new BTMessageStatBytes();
	
	/**
	 * <tt>Statistic</tt> for incoming keep alive messages
	 */
	public static final Statistic INCOMING_BITFIELD =
		new BTMessageStatBytes();
	
	/**
	 * <tt>Statistic</tt> for incoming choke messages
	 */
	public static final Statistic INCOMING_CHOKE =
		new BTMessageStatBytes();
	
	/**
	 * <tt>Statistic</tt> for incoming unchoke messages
	 */
	public static final Statistic INCOMING_UNCHOKE =
		new BTMessageStatBytes();
	
	/**
	 * <tt>Statistic</tt> for incoming interested messages
	 */
	public static final Statistic INCOMING_INTERESTED =
		new BTMessageStatBytes();
	/**
	 * <tt>Statistic</tt> for incoming not interested messages
	 */
	public static final Statistic INCOMING_NOT_INTERESTED =
		new BTMessageStatBytes();
	
	/**
	 * <tt>Statistic</tt> for incoming have messages
	 */
	public static final Statistic INCOMING_HAVE =
		new BTMessageStatBytes();
	
	/**
	 * <tt>Statistic</tt> for incoming request messages
	 */
	public static final Statistic INCOMING_REQUEST =
		new BTMessageStatBytes();
	
	/**
	 * <tt>Statistic</tt> for incoming piece messages
	 */
	public static final Statistic INCOMING_PIECE =
		new BTMessageStatBytes();
	
	/**
	 * <tt>Statistic</tt> for incoming cancel messages
	 */
	public static final Statistic INCOMING_CANCEL =
		new BTMessageStatBytes();
	
	/**
	 * <tt>Statistic</tt> for incoming alt loc request messages
	 */
	public static final Statistic INCOMING_ALT_LOC_REQUEST =
		new BTMessageStatBytes();

	/**
	 * <tt>Statistic</tt> for incoming alt locs messages
	 */
	public static final Statistic INCOMING_ALT_LOCS =
		new BTMessageStatBytes();
	
	
	/**
	 * <tt>Statistic</tt> for outgoing keep alive messages
	 */
	public static final Statistic OUTGOING_KEEP_ALIVE =
		new BTMessageStatBytes();
	
	/**
	 * <tt>Statistic</tt> for outgoing keep alive messages
	 */
	public static final Statistic OUTGOING_BITFIELD =
		new BTMessageStatBytes();
	
	/**
	 * <tt>Statistic</tt> for outgoing choke messages
	 */
	public static final Statistic OUTGOING_CHOKE =
		new BTMessageStatBytes();
	
	/**
	 * <tt>Statistic</tt> for outgoing unchoke messages
	 */
	public static final Statistic OUTGOING_UNCHOKE =
		new BTMessageStatBytes();
	
	/**
	 * <tt>Statistic</tt> for outgoing interested messages
	 */
	public static final Statistic OUTGOING_INTERESTED =
		new BTMessageStatBytes();
	/**
	 * <tt>Statistic</tt> for outgoing not interested messages
	 */
	public static final Statistic OUTGOING_NOT_INTERESTED =
		new BTMessageStatBytes();
	
	/**
	 * <tt>Statistic</tt> for outgoing have messages
	 */
	public static final Statistic OUTGOING_HAVE =
		new BTMessageStatBytes();
	
	/**
	 * <tt>Statistic</tt> for outgoing request messages
	 */
	public static final Statistic OUTGOING_REQUEST =
		new BTMessageStatBytes();
	
	/**
	 * <tt>Statistic</tt> for outgoing piece messages
	 */
	public static final Statistic OUTGOING_PIECE =
		new BTMessageStatBytes();
	
	/**
	 * <tt>Statistic</tt> for outgoing cancel messages
	 */
	public static final Statistic OUTGOING_CANCEL =
		new BTMessageStatBytes();
	
	/**
	 * <tt>Statistic</tt> for outgoing alt loc request messages
	 */
	public static final Statistic OUTGOING_ALT_LOC_REQUEST =
		new BTMessageStatBytes();

	/**
	 * <tt>Statistic</tt> for outgoing alt locs messages
	 */
	public static final Statistic OUTGOING_ALT_LOCS =
		new BTMessageStatBytes();
}
