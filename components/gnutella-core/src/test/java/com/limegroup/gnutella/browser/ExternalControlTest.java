package com.limegroup.gnutella.browser;

import org.limewire.inject.Providers;

import com.limegroup.gnutella.ProviderHacks;
import com.limegroup.gnutella.util.LimeTestCase;

public class ExternalControlTest extends LimeTestCase {

	MagnetOptions[] validMagnets;
	
	public ExternalControlTest(String name) {
		super(name);
	}
	
	public void setUp() {
		validMagnets = MagnetOptions.parseMagnet("magnet:?xt.1=urn:sha1:YNCKHTQCWBTRNJIV4WNAE52SJUQCZO5C&xt.2=urn:sha1:TXGCZQTH26NL6OUQAJJPFALHG2LTGBC7");
	}
	
	public void testParseValidMagnets() {
	    ExternalControl externalControl = new ExternalControl(ProviderHacks.getDownloadServices(), Providers.of(ProviderHacks.getActivityCallback()), ProviderHacks.getSocketsManager());
		String magnets = createMultiLineMagnetLinks(validMagnets);
		MagnetOptions[] opts = externalControl.parseMagnets(magnets);
		assertEquals("Should have parsed " + validMagnets.length + " magnets", validMagnets.length, opts.length);
		// and parse again
		assertEquals("Should have parsed " + opts.length + " magnets", opts.length, 
		        externalControl.parseMagnets(createMultiLineMagnetLinks(opts)).length);
		
		// compound magnets with multiple lines
		opts = externalControl.parseMagnets
			("magnet:?xt.1=urn:sha1:YNCKHTQCWBTRNJIV4WNAE52SJUQCZO5C&xt.2=urn:sha1:TXGCZQTH26NL6OUQAJJPFALHG2LTGBC7"
					+ System.getProperty("line.separator")
					+ "magnet:?xt.1=urn:sha1:YNCKHTQCWBTRNJIV4WNAE52SJUQCZO5C&xt.2=urn:sha1:TXGCZQTH26NL6OUQAJJPFALHG2LTGBC7");
		assertEquals("Should have parsed 2 magnets", 4, opts.length);
	}
	
	private String createMultiLineMagnetLinks(MagnetOptions[] opts) {
		StringBuffer buffer = new StringBuffer();
		for (int i = 0; i < opts.length; i++) {
			if (i > 0) {
				buffer.append(System.getProperty("line.separator"));
			}
			buffer.append(opts[i].toString());
		}
		return buffer.toString();
	}
}
