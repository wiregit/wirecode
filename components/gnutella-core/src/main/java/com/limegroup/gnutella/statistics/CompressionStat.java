package com.limegroup.gnutella.statistics;

import org.limewire.statistic.AdvancedKilobytesStatistic;
import org.limewire.statistic.Statistic;

/**
 * This class contains a type-safe enumeration of all classes for
 * compression data.
 */
public class CompressionStat extends AdvancedKilobytesStatistic {
	
	/**
	 * Make the constructor private so that only this class can construct
	 * a <tt>BandwidthStat</tt> instances.
	 */
	private CompressionStat() {}

	/**
	 * Specialized class for accumulating all uncompressed upstream data.
	 */
	private static class UncompressedUpstream extends CompressionStat {
		public void addData(int data) {
			super.addData(data);
			UPSTREAM_UNCOMPRESSED.addData(data);
		}
	}

	/**
	 * Specialized class for accumulating all uncompressed downstream data.
	 */
	private static class UncompressedDownstream extends CompressionStat {
		public void addData(int data) {
			super.addData(data);
			DOWNSTREAM_UNCOMPRESSED.addData(data);
		}
	}
	
	/**
	 * Specialized class for accumulating all compressed upstream data.
	 */
	private static class CompressedUpstream extends CompressionStat {
		public void addData(int data) {
			super.addData(data);
			UPSTREAM_COMPRESSED.addData(data);
		}
	}

	/**
	 * Specialized class for accumulating all compressed downstream data.
	 */
	private static class CompressedDownstream extends CompressionStat {
		public void addData(int data) {
			super.addData(data);
			DOWNSTREAM_COMPRESSED.addData(data);
		}
	}

	/**
	 * <tt>Statistic</tt> for all upstream uncompressed bandwidth.
	 */
	public static final Statistic UPSTREAM_UNCOMPRESSED =
		new CompressionStat();

	/**
	 * <tt>Statistic</tt> for all downstream uncompressed bandwidth.
	 */
	public static final Statistic DOWNSTREAM_UNCOMPRESSED =
		new CompressionStat();

	/**
	 * <tt>Statistic</tt> for all upstream compressed bandwidth.
	 */
	public static final Statistic UPSTREAM_COMPRESSED =
		new CompressionStat();

	/**
	 * <tt>Statistic</tt> for all downstream compressed bandwidth.
	 */
	public static final Statistic DOWNSTREAM_COMPRESSED =
		new CompressionStat();

	/**
	 * <tt>Statistic</tt> for all upstream Gnutella uncompressed traffic.
	 */
	public static final Statistic GNUTELLA_UNCOMPRESSED_UPSTREAM =
		new UncompressedUpstream();

	/**
	 * <tt>Statistic</tt> for all downstream Gnutella uncompressed traffic.
	 */
	public static final Statistic GNUTELLA_UNCOMPRESSED_DOWNSTREAM =
		new UncompressedDownstream();

	/**
	 * <tt>Statistic</tt> for all upstream Gnutella compressed traffic.
	 */
	public static final Statistic GNUTELLA_COMPRESSED_UPSTREAM =
		new CompressedUpstream();

	/**
	 * <tt>Statistic</tt> for all downstream Gnutella compressed traffic.
	 */
	public static final Statistic GNUTELLA_COMPRESSED_DOWNSTREAM =
		new CompressedDownstream();

	/**
	 * <tt>Statistic</tt> for all upstream HTTP uncompressed traffic.
	 */
	public static final Statistic HTTP_UNCOMPRESSED_UPSTREAM =
		new UncompressedUpstream();

	/**
	 * <tt>Statistic</tt> for all downstream HTTP uncompressed traffic.
	 */
	public static final Statistic HTTP_UNCOMPRESSED_DOWNSTREAM =
		new UncompressedDownstream();

	/**
	 * <tt>Statistic</tt> for all upstream HTTP compressed traffic.
	 */
	public static final Statistic HTTP_COMPRESSED_UPSTREAM =
		new CompressedUpstream();

	/**
	 * <tt>Statistic</tt> for all downstream HTTP compressed traffic.
	 */
	public static final Statistic HTTP_COMPRESSED_DOWNSTREAM =
		new CompressedDownstream();
}
