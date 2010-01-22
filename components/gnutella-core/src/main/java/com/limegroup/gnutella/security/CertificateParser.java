package com.limegroup.gnutella.security;

import java.io.IOException;

public interface CertificateParser {

    Certificate parseCertificate(String data) throws IOException;
}
