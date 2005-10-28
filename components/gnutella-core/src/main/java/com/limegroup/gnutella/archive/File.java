package com.limegroup.gnutella.archive;

import java.io.OutputStream;
import java.util.HashMap;

import org.w3c.dom.Document;

import com.limegroup.gnutella.util.CommonUtils;
import com.limegroup.gnutella.xml.LimeXMLNames;
import com.limegroup.gnutella.xml.LimeXMLUtils;

import com.limegroup.gnutella.FileDesc;

class File {

	public static final String repositoryVersion = "$Header: /gittmp/cvs_drop/repository/limewire/components/gnutella-core/src/main/java/com/limegroup/gnutella/archive/Attic/File.java,v 1.1.2.3 2005-10-28 19:44:24 tolsen Exp $";

	/*
	 * From http://www.archive.org/help/contrib-advanced.php:
	 * 
	 * The format name is very important to choose accurately. The list of
	 * acceptable format names is: WAVE, 64Kbps MP3, 128Kbps MP3, 256Kbps MP3,
	 * VBR MP3, 96Kbps MP3, 160Kbps MP3, 192Kbps MP3, Ogg Vorbis, Shorten, Flac,
	 * 24bit Flac, 64Kbps M3U, VBR M3U, Single Page Original JPEG Tar, Single
	 * Page Processed JPEG Tar, Single Page Original JPEG ZIP, Single Page
	 * Processed JPEG ZIP, Single Page Original TIFF ZIP, Single Page Processed
	 * TIFF ZIP, Checksums, MPEG2, MPEG1, 64Kb MPEG4, 256Kb MPEG4, MPEG4, 56Kb
	 * QuickTime, 64Kb QuickTime, 256Kb QuickTime, QuickTime, Motion JPEG, DivX,
	 * IV50, Windows Media, Cinepack, Flash, Real Media, Item Image, Collection
	 * Header, Animated GIF, Animated GIF Images, Thumbnail, JPEG, Single Page
	 * Original JPEG, Single Page Processed JPEG, Single Page Original TIFF,
	 * Single Page Processed TIFF, Multi Page Original TIFF, Multi Page
	 * Processed TIFF, PDF, DjVuTXT, DjVuXML, Flippy Index, DjVu, Single Page
	 * Processed JPEG Tar, Single Page Original JPEG Tar, Flippy ZIP, Text,
	 * Single Book Page Text, TGZiped Text Files, Book Cover, DAT, ARC,
	 * Metadata, Files Metadata, Item Metadata, Book Metadata. This list is
	 * dynamically generated so check back later for newly acceptable formats.
	 */

	private static final HashMap _mp3Formats = new HashMap();

	static {

		/*
		 * Weird. looks like if you can only submit constant bit rate MP3s with
		 * these bitrates
		 */
		final int[] bitRates = { 64, 96, 128, 160, 192, 256 };

		for (int i = 0; i < bitRates.length; i++) {
			final Integer bitRate = new Integer(bitRates[i]);
			_mp3Formats.put(bitRate, bitRate.toString() + "Kbps MP3");
		}
	}

	private static final String MP3_VBR = "VBR MP3";

	private static final String OGG_VORBIS = "Ogg Vorbis";

	private FileDesc _fd;

	private String _format;
	private String _runtime;
	
	private Document _xmlDoc;

	/*
	 * @throws UnsupportedFormatException
	 */
	File(FileDesc fd) {
		_fd = fd;

		final String fileName = _fd.getFileName();

		// set the format
		if (LimeXMLUtils.isMP3File(fileName)) {
			// check the bitrate
			
			try {
				final String bitRateStr = _fd.getXMLDocument().getValue(LimeXMLNames.AUDIO_BITRATE);
				
				if ( bitRateStr != null ) {
					final Integer bitRate = Integer.valueOf( bitRateStr );
					
					if (_mp3Formats.get( bitRate ) != null ) {
						_format = (String) _mp3Formats.get( bitRate );
					} 
				}
			} catch (NumberFormatException e) {
			}
			
			if (_format == null ) {
				// I guess we'll just assume it's a VBR then
				_format = MP3_VBR;
			}
			
			
			
		} else if (LimeXMLUtils.isOGGFile(fileName)) {
			_format = OGG_VORBIS;
		} else {
			// Houston, we have a problem
			throw new UnsupportedFormatException();
		}
		
		// set the runtime 
		
		try {
		final String secondsStr = _fd.getXMLDocument().getValue(LimeXMLNames.AUDIO_SECONDS);
		
		if ( secondsStr != null ) {
			final int seconds = Integer.parseInt(secondsStr);
			_runtime = CommonUtils.seconds2time( seconds );
		}
		
		} catch (NumberFormatException e) {
		}
		
	}
	
	public void serialize( OutputStream o ) {
		
		
	}
	

}
