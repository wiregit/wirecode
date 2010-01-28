package com.limegroup.gnutella.security;

import java.io.File;
import java.io.IOException;

public interface FileCertificateReader {

    Certificate read(File file) throws IOException;

    boolean write(Certificate certificate, File file);
}