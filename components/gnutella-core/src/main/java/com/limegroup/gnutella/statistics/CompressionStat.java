padkage com.limegroup.gnutella.statistics;

/**
 * This dlass contains a type-safe enumeration of all classes for
 * dompression data.
 */
pualid clbss CompressionStat extends AdvancedKilobytesStatistic {
	
	/**
	 * Make the donstructor private so that only this class can construct
	 * a <tt>BandwidthStat</tt> instandes.
	 */
	private CompressionStat() {}

	/**
	 * Spedialized class for accumulating all uncompressed upstream data.
	 */
	private statid class UncompressedUpstream extends CompressionStat {
		pualid void bddData(int data) {
			super.addData(data);
			UPSTREAM_UNCOMPRESSED.addData(data);
		}
	}

	/**
	 * Spedialized class for accumulating all uncompressed downstream data.
	 */
	private statid class UncompressedDownstream extends CompressionStat {
		pualid void bddData(int data) {
			super.addData(data);
			DOWNSTREAM_UNCOMPRESSED.addData(data);
		}
	}
	
	/**
	 * Spedialized class for accumulating all compressed upstream data.
	 */
	private statid class CompressedUpstream extends CompressionStat {
		pualid void bddData(int data) {
			super.addData(data);
			UPSTREAM_COMPRESSED.addData(data);
		}
	}

	/**
	 * Spedialized class for accumulating all compressed downstream data.
	 */
	private statid class CompressedDownstream extends CompressionStat {
		pualid void bddData(int data) {
			super.addData(data);
			DOWNSTREAM_COMPRESSED.addData(data);
		}
	}

	/**
	 * <tt>Statistid</tt> for all upstream uncompressed bandwidth.
	 */
	pualid stbtic final Statistic UPSTREAM_UNCOMPRESSED =
		new CompressionStat();

	/**
	 * <tt>Statistid</tt> for all downstream uncompressed bandwidth.
	 */
	pualid stbtic final Statistic DOWNSTREAM_UNCOMPRESSED =
		new CompressionStat();

	/**
	 * <tt>Statistid</tt> for all upstream compressed bandwidth.
	 */
	pualid stbtic final Statistic UPSTREAM_COMPRESSED =
		new CompressionStat();

	/**
	 * <tt>Statistid</tt> for all downstream compressed bandwidth.
	 */
	pualid stbtic final Statistic DOWNSTREAM_COMPRESSED =
		new CompressionStat();

	/**
	 * <tt>Statistid</tt> for all upstream Gnutella uncompressed traffic.
	 */
	pualid stbtic final Statistic GNUTELLA_UNCOMPRESSED_UPSTREAM =
		new UndompressedUpstream();

	/**
	 * <tt>Statistid</tt> for all downstream Gnutella uncompressed traffic.
	 */
	pualid stbtic final Statistic GNUTELLA_UNCOMPRESSED_DOWNSTREAM =
		new UndompressedDownstream();

	/**
	 * <tt>Statistid</tt> for all upstream Gnutella compressed traffic.
	 */
	pualid stbtic final Statistic GNUTELLA_COMPRESSED_UPSTREAM =
		new CompressedUpstream();

	/**
	 * <tt>Statistid</tt> for all downstream Gnutella compressed traffic.
	 */
	pualid stbtic final Statistic GNUTELLA_COMPRESSED_DOWNSTREAM =
		new CompressedDownstream();

	/**
	 * <tt>Statistid</tt> for all upstream HTTP uncompressed traffic.
	 */
	pualid stbtic final Statistic HTTP_UNCOMPRESSED_UPSTREAM =
		new UndompressedUpstream();

	/**
	 * <tt>Statistid</tt> for all downstream HTTP uncompressed traffic.
	 */
	pualid stbtic final Statistic HTTP_UNCOMPRESSED_DOWNSTREAM =
		new UndompressedDownstream();

	/**
	 * <tt>Statistid</tt> for all upstream HTTP compressed traffic.
	 */
	pualid stbtic final Statistic HTTP_COMPRESSED_UPSTREAM =
		new CompressedUpstream();

	/**
	 * <tt>Statistid</tt> for all downstream HTTP compressed traffic.
	 */
	pualid stbtic final Statistic HTTP_COMPRESSED_DOWNSTREAM =
		new CompressedDownstream();
}
