package com.limegroup.gnutella.lws.server;

/**
 * Some reused values in the download tests.
 */
final class LWSDownloadTestConstants {
    
    /** Name of the file we're downloading. */
    final String FILE = "test.mp3";
    
    /** Relative URL of the file we're downloading. */
    final String URL = "/" + FILE;
    
    /** dummy auth parameters for downloading **/
    final String AUTH_PARAMETERS = "hash=0f3a1cb18b672c468c1b4984a4d5dac86ce9bae0&signedHash=GAWQEFIAS3EKXL2TBFWUFX3B4THYAQKFMXFJAU7VAIKCTF6WY6JZWKCFPLPAYB5NY5L2MSQJAP6Q&browserIP=127.0.0.1&signedBrowserIP=GAWQEFCZHHPCBCNF2X23Z7NVPBCN2ZX2ILOSJTACCUAJAM3LRA2LRAEJY7NOVTXSGY477O2SMA2A";
    
    /** The length of the file */
    final long LENGTH = 400000;
    
    /** Dummy ID of the progress bar we're updating, passed to the <code>Download</code> message. */
    final String ID = "123456";
    
    /** Host from where we're downloading. */
    final String HOST = "127.0.0.1";
    
    /** Port on which the {@link SimpleWebServer} will be running. */
    final int PORT = 8011;
    
    /** The amount we wait for a download.  This should be much more than needed. */
    final long DOWNLOAD_WAIT_TIME = 1000 * 20 * 1;
}
