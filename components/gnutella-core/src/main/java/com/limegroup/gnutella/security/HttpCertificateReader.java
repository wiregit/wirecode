package com.limegroup.gnutella.security;

import java.io.IOException;
import java.net.URI;

import org.limewire.io.IpPort;

public interface HttpCertificateReader {

    Certificate read(URI uri, IpPort messageSource) throws IOException;

}