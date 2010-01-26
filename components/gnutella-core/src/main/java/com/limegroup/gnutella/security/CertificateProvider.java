package com.limegroup.gnutella.security;

import org.limewire.inject.MutableProvider;
import org.limewire.io.IpPort;

public interface CertificateProvider extends MutableProvider<Certificate> {

    Certificate getFromHttp(IpPort messageSource);
}
