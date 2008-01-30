package com.limegroup.gnutella.downloader.serial.conversion;

import java.io.Serializable;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * helps to migrate persistant org.apache.commons.httpclient.URI's to java.net.URI's
 */
public class SerialOldURI implements Serializable {
    private static final long serialVersionUID = 604752400577948726L;


    /**
     * Cache the hash code for this URI.
     */
    private int hash;


    /**
     * This Uniform Resource Identifier (URI).
     * The URI is always in an "escaped" form, since escaping or unescaping
     * a completed URI might change its semantics.  
     */
    private char[] _uri;


    /**
     * The charset of the protocol used by this URI instance.
     */
    private String protocolCharset;
    
    /**
     * The scheme.
     */
    private char[] _scheme;


    /**
     * The opaque.
     */
    private char[] _opaque;


    /**
     * The authority.
     */
    private char[] _authority;


    /**
     * The userinfo.
     */
    private char[] _userinfo;


    /**
     * The host.
     */
    private char[] _host;


    /**
     * The port.
     */
    private int _port;


    /**
     * The path.
     */
    private char[] _path;


    /**
     * The query.
     */
    private char[] _query;


    /**
     * The fragment.
     */
    private char[] _fragment;
    
    // URI-reference = [ absoluteURI | relativeURI ] [ "#" fragment ]
    // absoluteURI   = scheme ":" ( hier_part | opaque_part )
    protected boolean _is_hier_part;
    protected boolean _is_opaque_part;
    // relativeURI   = ( net_path | abs_path | rel_path ) [ "?" query ] 
    // hier_part     = ( net_path | abs_path ) [ "?" query ]
    protected boolean _is_net_path;
    protected boolean _is_abs_path;
    protected boolean _is_rel_path;
    // net_path      = "//" authority [ abs_path ] 
    // authority     = server | reg_name
    protected boolean _is_reg_name;
    protected boolean _is_server;  // = _has_server
    // server        = [ [ userinfo "@" ] hostport ]
    // host          = hostname | IPv4address | IPv6reference
    protected boolean _is_hostname;
    protected boolean _is_IPv4address;
    protected boolean _is_IPv6reference;

    public URI toURI() throws URISyntaxException {
        return (_uri == null) ? null : new URI(new String(_uri));    
    }
}
