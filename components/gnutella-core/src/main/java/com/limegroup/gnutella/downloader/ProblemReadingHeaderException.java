package com.limegroup.gnutella.downloader;

import java.io.IOException;

/**
 * Thrown in replace of IndexOutOfBoundsException or NumberFormatException
 */
public class ProblemReadingHeaderException extends IOException {
	public ProblemReadingHeaderException() { super("Problem Reading Header"); }
	public ProblemReadingHeaderException(String msg) { super(msg); }
}

