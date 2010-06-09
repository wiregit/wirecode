package com.limegroup.gnutella.downloader.serial.conversion;

import java.io.File;

import org.limewire.io.URNImpl;


interface SerialResumeDownloader extends SerialManagedDownloader {

    public File getIncompleteFile();

    public String getName();

    public long getSize();

    public URNImpl getUrn();

}