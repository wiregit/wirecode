package com.limegroup.gnutella.browser;

import java.net.URLEncoder;

import com.limegroup.gnutella.util.BaseTestCase;

public class ExternalControlTest extends BaseTestCase {

	private MagnetOptions[] validMagnets; 
	
	public ExternalControlTest(String name) {
		super(name);
	}
	
	public void setUp() {
		MagnetOptions o1 = new MagnetOptions();
		o1.setDN(URLEncoder.encode("compilc.:fileNm?7ßä"));
		o1.setKT(URLEncoder.encode("keyword topic string"));
		o1.addXT("urn:sha1:YNCKHTQCWBTRNJIV4WNAE52SJUQCZO5C");
		o1.addXS("http://127.0.0.1:6346/uri-res/N2R?urn:sha1:YNCKHTQCWBTRNJIV4WNAE52SJUQCZO5C");
		
		MagnetOptions o2 = new MagnetOptions();
		o2.setDN(URLEncoder.encode("compile me"));
		o1.addXT("urn:sha1:YNCKHTQCWBTRNJIV4WNAE52SJUQCZO59");
		o1.addXS("http://127.0.0.1:6346/uri-res/N2R?urn:sha1:YNCKHTQCWBTRNJIV4WNAE52SJUQCZO59");
		
		validMagnets = new MagnetOptions[] {
				o1, o2
		};
	}
	
	public void testParseValidMagnet() {
		
		for (int i = 0; i < validMagnets.length; i++) {
			MagnetOptions[] opts = ExternalControl.parseMagnet(validMagnets[i].toString());
			assertEquals("Should have parsed one magnet", 1, opts.length);
			opts = ExternalControl.parseMagnet(opts[0].toString());
			assertEquals("Should have parsed one magnet", 1, opts.length);
		}
		
		MagnetOptions[] opts = ExternalControl.parseMagnet("magnet:?xt.1=urn:sha1:YNCKHTQCWBTRNJIV4WNAE52SJUQCZO5C&xt.2=urn:sha1:TXGCZQTH26NL6OUQAJJPFALHG2LTGBC7");
		assertEquals("Should have parsed 2 magnets", 2, opts.length);
	}

	public void testParseValidMagnets() {
		String magnets = createMultiLineMagnetLinks(validMagnets);
		MagnetOptions[] opts = ExternalControl.parseMagnets(magnets);
		assertEquals("Should have parsed " + validMagnets.length + " magnets", validMagnets.length, opts.length);
		// and parse again
		assertEquals("Should have parsed " + opts.length + " magnets", opts.length, 
				ExternalControl.parseMagnets(createMultiLineMagnetLinks(opts)).length);
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
