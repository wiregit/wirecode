package com.limegroup.gnutella;

import java.util.Properties;

/**
 * Provides a servent ways to selectively send headers in response to those read
 * from a remote host.  Here, is a typical anonymous implementation of
 * HandshakeResponder:
 * <pre>
 * new HandshakeResponder() {
 *     public Properties respond(Properties read) {
 *          //Ignores read
 *          Properties ret=new Properties();
 *          ret.setProperty("Query-Routing", "0.1");
 *          ret.setProperty("Pong-Caching", "0.1");
 *          return ret;
 *     }
 * }
 * </pre>
 *
 * Here is a more complicated implementation that sends private
 * if appropriate:
 * <pre>
 * new HandshakeResponder() {
 *     public Properties respond(Properties read) {
 *          Properties ret=new Properties();
 *          if (read.getProperty("User-Agent")!=null
 *                 && read.getProperty("User-Agent").equals("BearShare")) {
 *              ret.setProperty("BearShare-Secret", createSecret());
 *          return ret;
 *     }
 * }
 * </pre> 
 *
 * In the future, the respond method may be able to indicate non-standard
 * response codes (e.g., "401 Unauthorized") via exception or return value.
 */
public interface HandshakeResponder {
    /** Returns the properties to be written to the remote host
     *  in response to the given set of properties just read. */
    public Properties respond(Properties read);
}
