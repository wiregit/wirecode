package com.limegroup.gnutella.statistics;

/**
 * This class acts as a generalized stat handler that records Gnutella message 
 * data by the raw number of messages and by the number of bytes passed.  It 
 * also keeps this data for LimeWire vs. non-LimeWire messages.
 */
final class GeneralStatHandler extends AbstractStatHandler {

	GeneralStatHandler(String fileName) {
		super(fileName);
	}
}
