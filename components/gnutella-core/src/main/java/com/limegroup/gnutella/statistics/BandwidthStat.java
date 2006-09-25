package com.limegroup.gnutella.statistics;

/**
 * This class contains a type-safe enumeration of all classes for
 * bandwidth data.
 */
public class BandwidthStat extends BasicKilobytesStatistic {
	
	/**
	 * Make the constructor private so that only this class can construct
	 * a <tt>BandwidthStat</tt> instances.
	 */
	private BandwidthStat() {}

	/**
	 * Specialized class for accumulating all upstream bandwidth data.
	 */
	public static class UpstreamBandwidthStat extends BandwidthStat {
		public void addData(int data) {
			super.addData(data);
			UPSTREAM_BANDWIDTH.addData(data);
		}
	}

	/**
	 * Specialized class for accumulating all downstream bandwidth data.
	 */
	public static class DownstreamBandwidthStat extends BandwidthStat {
		public void addData(int data) {
			super.addData(data);
			DOWNSTREAM_BANDWIDTH.addData(data);
		}
	}	

	/**
	 * Specialized class for accumulating all downstream bandwidth sent
	 * over Gnutella.
	 */
	private static class DownstreamGnutellaBandwidthStat extends BandwidthStat {
		public void addData(int data) {
			super.addData(data);
			GNUTELLA_DOWNSTREAM_BANDWIDTH.addData(data);
		}
	}

	/**
	 * Specialized class for accumulating all upstream bandwidth sent
	 * over Gnutella.
	 */
	private static class UpstreamGnutellaBandwidthStat extends BandwidthStat {
		public void addData(int data) {
			super.addData(data);
			GNUTELLA_UPSTREAM_BANDWIDTH.addData(data);
		}
	}	

	/**
	 * Specialized class for accumulating all downstream bandwidth sent
	 * over HTTP.
	 */
	private static class DownstreamHTTPBandwidthStat extends BandwidthStat {
		public void addData(int data) {
			super.addData(data);
			HTTP_DOWNSTREAM_BANDWIDTH.addData(data);
		}
	}

	/**
	 * Specialized class for accumulating all upstream bandwidth sent
	 * over HTTP.
	 */
	private static class UpstreamHTTPBandwidthStat extends BandwidthStat {
		public void addData(int data) {
			super.addData(data);
			HTTP_UPSTREAM_BANDWIDTH.addData(data);
		}
	}
    
    /**
     * Specialized class for accumulating all downstream bandwidth sent
     * over HTTP by in-network downloaders
     */
    private static class DownstreamInNetworkHTTPBandwidthStat extends BandwidthStat {
        public void addData(int data) {
            super.addData(data);
            HTTP_DOWNSTREAM_INNETWORK_BANDWIDTH.addData(data);
        }
    }

    /**
     * Specialized class for accumulating all upstream bandwidth sent
     * over HTTP by forced-share uploaders
     */
    private static class UpstreamInNetworkHTTPBandwidthStat extends BandwidthStat {
        public void addData(int data) {
            super.addData(data);
            HTTP_UPSTREAM_INNETWORK_BANDWIDTH.addData(data);
        }
    }
    

	/**
	 * <tt>Statistic</tt> for all upstream bandwidth.
	 */
	public static final Statistic UPSTREAM_BANDWIDTH =
		new BandwidthStat();

	/**
	 * <tt>Statistic</tt> for all downstream bandwidth.
	 */
	public static final Statistic DOWNSTREAM_BANDWIDTH =
		new BandwidthStat();

	/**
	 * <tt>Statistic</tt> for all HTTP downstream bandwidth.
	 */
	public static final Statistic HTTP_DOWNSTREAM_BANDWIDTH =
		new DownstreamBandwidthStat();
    
    /**
     * <tt>Statistic</tt> for all HTTP downstream bandwidth used by in-network downloaders
     */
    public static final Statistic HTTP_DOWNSTREAM_INNETWORK_BANDWIDTH =
        new DownstreamBandwidthStat();

	/**
	 * <tt>Statistic</tt> for all HTTP upstream bandwidth.
	 */
	public static final Statistic HTTP_UPSTREAM_BANDWIDTH =
		new UpstreamBandwidthStat();
    
    /**
     * <tt>Statistic</tt> for all HTTP upstream bandwidth used by forced uploaders
     */
    public static final Statistic HTTP_UPSTREAM_INNETWORK_BANDWIDTH =
        new UpstreamBandwidthStat();

	/**
	 * <tt>Statistic</tt> for all HTTP downstream header bandwidth -- http
	 * headers read from the network.
	 */
	public static final Statistic HTTP_HEADER_DOWNSTREAM_BANDWIDTH =
		new DownstreamHTTPBandwidthStat();

    /**
     * <tt>Statistic</tt> for all HTTP downstream header bandwidth -- http
     * headers read from the network by forced-share uploaders
     */
    public static final Statistic HTTP_HEADER_DOWNSTREAM_INNETWORK_BANDWIDTH =
        new DownstreamInNetworkHTTPBandwidthStat();

	/**
	 * <tt>Statistic</tt> for all HTTP message body bytes read from the
	 * network.
	 */
	public static final Statistic HTTP_BODY_DOWNSTREAM_BANDWIDTH =
		new DownstreamHTTPBandwidthStat();
    
    /**
     * <tt>Statistic</tt> for all HTTP message body bytes read from the
     * network by InNetwork downloaders.
     */
    public static final Statistic HTTP_BODY_DOWNSTREAM_INNETWORK_BANDWIDTH =
        new DownstreamInNetworkHTTPBandwidthStat();

	/**
	 * <tt>Statistic</tt> for all HTTP upstream header bandwidth -- http
	 * headers read from the network.
	 */
	public static final Statistic HTTP_HEADER_UPSTREAM_BANDWIDTH =
		new UpstreamHTTPBandwidthStat();

    /**
     * <tt>Statistic</tt> for all HTTP upstream header bandwidth -- http
     * headers read from the network by in-network downloaders.
     */
    public static final Statistic HTTP_HEADER_UPSTREAM_INNETWORK_BANDWIDTH =
        new UpstreamInNetworkHTTPBandwidthStat();

	/**
	 * <tt>Statistic</tt> for all HTTP message body bytes written to the
	 * network.
	 */
	public static final Statistic HTTP_BODY_UPSTREAM_BANDWIDTH =
		new UpstreamHTTPBandwidthStat();
    
    /**
     * <tt>Statistic</tt> for all HTTP message body bytes written to the
     * network by force-shared uploaders.
     */
    public static final Statistic HTTP_BODY_UPSTREAM_INNETWORK_BANDWIDTH =
        new UpstreamInNetworkHTTPBandwidthStat();
		
	/**
	 * <tt>Statistic</tt> for all downstream bandwidth used by Gnutella.
	 */
	public static final Statistic GNUTELLA_DOWNSTREAM_BANDWIDTH =
		new DownstreamBandwidthStat();

	/**
	 * <tt>Statistic</tt> for all upstream bandwidth used by Gnutella.
	 */
	public static final Statistic GNUTELLA_UPSTREAM_BANDWIDTH =
		new UpstreamBandwidthStat();

	/**
	 * <tt>Statistic</tt> for all downstream bandwidth used by Gnutella
	 * message traffic.
	 */
	public static final Statistic GNUTELLA_MESSAGE_DOWNSTREAM_BANDWIDTH =
		new DownstreamGnutellaBandwidthStat();


	/**
	 * <tt>Statistic</tt> for all upstream bandwidth used by Gnutella
	 * message traffic.
	 */
	public static final Statistic GNUTELLA_MESSAGE_UPSTREAM_BANDWIDTH =
		new UpstreamGnutellaBandwidthStat();

	/**
	 * <tt>Statistic</tt> for all downstream bandwidth used by Gnutella
	 * connection headers.
	 */
	public static final Statistic GNUTELLA_HEADER_DOWNSTREAM_BANDWIDTH =
		new DownstreamGnutellaBandwidthStat();


	/**
	 * <tt>Statistic</tt> for all upstream bandwidth used by Gnutella
	 * connection headers.
	 */
	public static final Statistic GNUTELLA_HEADER_UPSTREAM_BANDWIDTH =
		new UpstreamGnutellaBandwidthStat();

}
