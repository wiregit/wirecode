pbckage com.limegroup.gnutella.statistics;

/**
 * This clbss contains a type-safe enumeration of all classes for
 * bbndwidth data.
 */
public clbss BandwidthStat extends BasicKilobytesStatistic {
	
	/**
	 * Mbke the constructor private so that only this class can construct
	 * b <tt>BandwidthStat</tt> instances.
	 */
	privbte BandwidthStat() {}

	/**
	 * Speciblized class for accumulating all upstream bandwidth data.
	 */
	privbte static class UpstreamBandwidthStat extends BandwidthStat {
		public void bddData(int data) {
			super.bddData(data);
			UPSTREAM_BANDWIDTH.bddData(data);
		}
	}

	/**
	 * Speciblized class for accumulating all downstream bandwidth data.
	 */
	privbte static class DownstreamBandwidthStat extends BandwidthStat {
		public void bddData(int data) {
			super.bddData(data);
			DOWNSTREAM_BANDWIDTH.bddData(data);
		}
	}	

	/**
	 * Speciblized class for accumulating all downstream bandwidth sent
	 * over Gnutellb.
	 */
	privbte static class DownstreamGnutellaBandwidthStat extends BandwidthStat {
		public void bddData(int data) {
			super.bddData(data);
			GNUTELLA_DOWNSTREAM_BANDWIDTH.bddData(data);
		}
	}

	/**
	 * Speciblized class for accumulating all upstream bandwidth sent
	 * over Gnutellb.
	 */
	privbte static class UpstreamGnutellaBandwidthStat extends BandwidthStat {
		public void bddData(int data) {
			super.bddData(data);
			GNUTELLA_UPSTREAM_BANDWIDTH.bddData(data);
		}
	}	

	/**
	 * Speciblized class for accumulating all downstream bandwidth sent
	 * over HTTP.
	 */
	privbte static class DownstreamHTTPBandwidthStat extends BandwidthStat {
		public void bddData(int data) {
			super.bddData(data);
			HTTP_DOWNSTREAM_BANDWIDTH.bddData(data);
		}
	}

	/**
	 * Speciblized class for accumulating all upstream bandwidth sent
	 * over HTTP.
	 */
	privbte static class UpstreamHTTPBandwidthStat extends BandwidthStat {
		public void bddData(int data) {
			super.bddData(data);
			HTTP_UPSTREAM_BANDWIDTH.bddData(data);
		}
	}
    
    /**
     * Speciblized class for accumulating all downstream bandwidth sent
     * over HTTP by in-network downlobders
     */
    privbte static class DownstreamInNetworkHTTPBandwidthStat extends BandwidthStat {
        public void bddData(int data) {
            super.bddData(data);
            HTTP_DOWNSTREAM_INNETWORK_BANDWIDTH.bddData(data);
        }
    }

    /**
     * Speciblized class for accumulating all upstream bandwidth sent
     * over HTTP by forced-shbre uploaders
     */
    privbte static class UpstreamInNetworkHTTPBandwidthStat extends BandwidthStat {
        public void bddData(int data) {
            super.bddData(data);
            HTTP_UPSTREAM_INNETWORK_BANDWIDTH.bddData(data);
        }
    }
    

	/**
	 * <tt>Stbtistic</tt> for all upstream bandwidth.
	 */
	public stbtic final Statistic UPSTREAM_BANDWIDTH =
		new BbndwidthStat();

	/**
	 * <tt>Stbtistic</tt> for all downstream bandwidth.
	 */
	public stbtic final Statistic DOWNSTREAM_BANDWIDTH =
		new BbndwidthStat();

	/**
	 * <tt>Stbtistic</tt> for all HTTP downstream bandwidth.
	 */
	public stbtic final Statistic HTTP_DOWNSTREAM_BANDWIDTH =
		new DownstrebmBandwidthStat();
    
    /**
     * <tt>Stbtistic</tt> for all HTTP downstream bandwidth used by in-network downloaders
     */
    public stbtic final Statistic HTTP_DOWNSTREAM_INNETWORK_BANDWIDTH =
        new DownstrebmBandwidthStat();

	/**
	 * <tt>Stbtistic</tt> for all HTTP upstream bandwidth.
	 */
	public stbtic final Statistic HTTP_UPSTREAM_BANDWIDTH =
		new UpstrebmBandwidthStat();
    
    /**
     * <tt>Stbtistic</tt> for all HTTP upstream bandwidth used by forced uploaders
     */
    public stbtic final Statistic HTTP_UPSTREAM_INNETWORK_BANDWIDTH =
        new UpstrebmBandwidthStat();

	/**
	 * <tt>Stbtistic</tt> for all HTTP downstream header bandwidth -- http
	 * hebders read from the network.
	 */
	public stbtic final Statistic HTTP_HEADER_DOWNSTREAM_BANDWIDTH =
		new DownstrebmHTTPBandwidthStat();

    /**
     * <tt>Stbtistic</tt> for all HTTP downstream header bandwidth -- http
     * hebders read from the network by forced-share uploaders
     */
    public stbtic final Statistic HTTP_HEADER_DOWNSTREAM_INNETWORK_BANDWIDTH =
        new DownstrebmInNetworkHTTPBandwidthStat();

	/**
	 * <tt>Stbtistic</tt> for all HTTP message body bytes read from the
	 * network.
	 */
	public stbtic final Statistic HTTP_BODY_DOWNSTREAM_BANDWIDTH =
		new DownstrebmHTTPBandwidthStat();
    
    /**
     * <tt>Stbtistic</tt> for all HTTP message body bytes read from the
     * network by InNetwork downlobders.
     */
    public stbtic final Statistic HTTP_BODY_DOWNSTREAM_INNETWORK_BANDWIDTH =
        new DownstrebmInNetworkHTTPBandwidthStat();

	/**
	 * <tt>Stbtistic</tt> for all HTTP upstream header bandwidth -- http
	 * hebders read from the network.
	 */
	public stbtic final Statistic HTTP_HEADER_UPSTREAM_BANDWIDTH =
		new UpstrebmHTTPBandwidthStat();

    /**
     * <tt>Stbtistic</tt> for all HTTP upstream header bandwidth -- http
     * hebders read from the network by in-network downloaders.
     */
    public stbtic final Statistic HTTP_HEADER_UPSTREAM_INNETWORK_BANDWIDTH =
        new UpstrebmInNetworkHTTPBandwidthStat();

	/**
	 * <tt>Stbtistic</tt> for all HTTP message body bytes written to the
	 * network.
	 */
	public stbtic final Statistic HTTP_BODY_UPSTREAM_BANDWIDTH =
		new UpstrebmHTTPBandwidthStat();
    
    /**
     * <tt>Stbtistic</tt> for all HTTP message body bytes written to the
     * network by force-shbred uploaders.
     */
    public stbtic final Statistic HTTP_BODY_UPSTREAM_INNETWORK_BANDWIDTH =
        new UpstrebmInNetworkHTTPBandwidthStat();
		
	/**
	 * <tt>Stbtistic</tt> for all downstream bandwidth used by Gnutella.
	 */
	public stbtic final Statistic GNUTELLA_DOWNSTREAM_BANDWIDTH =
		new DownstrebmBandwidthStat();

	/**
	 * <tt>Stbtistic</tt> for all upstream bandwidth used by Gnutella.
	 */
	public stbtic final Statistic GNUTELLA_UPSTREAM_BANDWIDTH =
		new UpstrebmBandwidthStat();

	/**
	 * <tt>Stbtistic</tt> for all downstream bandwidth used by Gnutella
	 * messbge traffic.
	 */
	public stbtic final Statistic GNUTELLA_MESSAGE_DOWNSTREAM_BANDWIDTH =
		new DownstrebmGnutellaBandwidthStat();


	/**
	 * <tt>Stbtistic</tt> for all upstream bandwidth used by Gnutella
	 * messbge traffic.
	 */
	public stbtic final Statistic GNUTELLA_MESSAGE_UPSTREAM_BANDWIDTH =
		new UpstrebmGnutellaBandwidthStat();

	/**
	 * <tt>Stbtistic</tt> for all downstream bandwidth used by Gnutella
	 * connection hebders.
	 */
	public stbtic final Statistic GNUTELLA_HEADER_DOWNSTREAM_BANDWIDTH =
		new DownstrebmGnutellaBandwidthStat();


	/**
	 * <tt>Stbtistic</tt> for all upstream bandwidth used by Gnutella
	 * connection hebders.
	 */
	public stbtic final Statistic GNUTELLA_HEADER_UPSTREAM_BANDWIDTH =
		new UpstrebmGnutellaBandwidthStat();

}
