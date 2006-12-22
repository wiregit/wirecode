package com.limegroup.gnutella.metadata;


import org.limewire.util.CommonUtils;

import junit.framework.Test;

/**
 * Test MP3Info class
 *
 *
 * @author  cHANCE mOORE, ctmoore@gottapee.com - 30 July 2002
 */
//34567890123456789012345678901234567890123456789012345678901234567890123456789
public class MP3Test extends com.limegroup.gnutella.util.LimeTestCase {
	
	private static final int runs = 900;
	
	private static final String d = "com/limegroup/gnutella/metadata/";
	
	private static final String file[] = new String[] {
	d+"mpg1layIII_96k-RIFFWAV_441000hz_joint_Xing.wav",
	d+"mpg1layI_0h_448k_frame14_48000hz_dual_fl5Orig.mp3",
	d+"mpg1layI_43h_32k_f256_44100hz_dual_50-15emph_CRCcopyOrig_sectest.mp3",
	d+"mpg1layII_0h_384k_frame24_48000hz_stereo_CRCOrig_Gogo1sectest.mp3",
	d+"mpg1layIII_212k-VBRq0_f1655_441hz_stereo_FhgTAGID3v1_waterson.mp3",
	d+"mpg1layIII_0h_58k-VBRq30_frame1211_44100hz_joint_XingTAG_sample.mp3",
	d+"mpg1layIII_138k-VBRq44_f2895_44100hz_joint_XingTAG_ID3v2_Lame.mp3",
	d+"mpg1layIII_170k-VBRq66_f1721_441hz_stereo_XingTAGcopyID3v1_mutter.mp3",	
	d+"mpg1layII_0h_192k_frame44_44100hz_joint_CRCfl11.mp2",
	d+"mpg2.5layIII_8k-VBRq95_f149_8000khz_mono_ID3v2Orig_XingTAG_Lame.mp3",
	d+"mpg2layI_0h_128k_frame54_22050hz_joint_CRCOrig_test33.mp3",
	d+"mpg2layII_1504h_16k_frame56_24000hz_joint_CRCOrigID3v1&2_test27.mp3",
	d+"mpg2layIII_0h_40k_764or744frame_22050hz_mono_Orig_Fhgl3Frank.mp3",
	d+"mpg2layIII_12kABRq55_f1202_16hz_stereo_XingLameTAG_CRCOrigwaterson.mp3",
	d+"mpgPlus_210k-VBR_f1149_441hz_joint_copy__NOTmp1lay_448k_f540_5015.mpc",
	d+"mpgPRO2layIII_0h_64k_frame2036or2015_22050hz_joint_thomson.mp3",
	d+"mpg4_golem160x90first120.avi",
	d+"corruptFileWithBadHeaders.mp3"};

    public MP3Test(String name) {
        super(name);
    }

    /**
     * 
     * @param args java.lang.String[]
     */
    public static void main(String[] args) {
        
        junit.textui.TestRunner.run(suite());
        
        
    }
    protected void setUp() {
        
    }
    public static Test suite() {
        return buildTestSuite(MP3Test.class);
    }
    protected void tearDown() {
        
    }
    
    private MP3Info newMP3Info(String loc) throws Exception {
        return new MP3Info(CommonUtils.getResourceFile(loc).getPath());
    }

    
    /**
     *
     * All values (where possible) have been validated by
     *  Winamp	winamp.com
     *  EncSpot	guerillasoft.com
     *  LAME
     *  MusicMatch musicmatch.com
     *  		
     */
    public void testMP3()
        throws Exception {
        
        MP3Info info = null;
        
        /*
          mpg1layI_0h_448k_frame14_48000hz_dual_fl5Orig.mp3
        */
        info  = newMP3Info(file[1]);
        assertEquals(info.getFileSize(), 21952L);
        assertEquals(info.getBitRate() , 448);		
        assertTrue(!info.isCoprighted());		
        assertTrue(info.isOriginal());	
        assertTrue(!info.isPadded()); 	
        assertTrue(!info.isPrivate());	
        assertTrue(info.isProtected());	
        assertTrue(!info.isRiffWav());		
        assertEquals(info.getFrequency() , 48000);	
        assertEquals(info.getHeaderBitRate() , 448);
        assertEquals(info.getLayer_Numeric() , 1); 		
        assertEquals(info.getLengthInSeconds() , 0); 
        assertEquals(info.getMode() , "Dual Channel");	
        assertEquals(info.getVersion_Numeric() , 1.0d, 0D);
        assertEquals(info.getNumberOfFrames() , 1); //14-encspot 16-winamp//////////////////////////////////////
        assertEquals(info.getEmphasis() , "none");
        assertTrue(!info.hasVariableBitRate());
        
        /*
          mpg1layI_32k_f256_441hz_dual_5015emph_CRCcopyOrig_sectest.mp3
        */
        info  = newMP3Info(file[2]);
        assertEquals(info.getFileSize(), 26645L);
        assertEquals(info.getBitRate() , 32);		
        assertTrue(info.isCoprighted());		
        assertTrue(info.isOriginal());	
        assertTrue(!info.isPadded()); 	
        assertTrue(info.isPrivate());	
        assertTrue(info.isProtected());	
        assertTrue(!info.isRiffWav());		
        assertEquals(info.getFrequency() , 44100);	
        assertEquals(info.getHeaderBitRate() , 32);
        assertEquals(info.getLayer_Numeric() , 1); 		
        assertEquals(info.getLengthInSeconds() , 6); 
        assertEquals(info.getMode() , "Dual Channel");	
        assertEquals(info.getVersion_Numeric() , 1.0d, 0D);
        assertEquals(info.getNumberOfFrames() , 2); //256-winamp no encspot/////////////////////////////////////
        assertEquals(info.getEmphasis() , "50/15 ms");
        assertTrue(!info.hasVariableBitRate());
        
        /*
          mpg1layII_384k_f24_48hz_stereo_CRCOrig_Gogo1sectest.mp3
        */
        info  = newMP3Info(file[3]);
        assertEquals(info.getFileSize(), 31104L);
        assertEquals(info.getBitRate() , 384);		
        assertTrue(!info.isCoprighted());		
        assertTrue(info.isOriginal());	
        assertTrue(!info.isPadded()); 	
        assertTrue(!info.isPrivate());	
        assertTrue(info.isProtected());	
        assertTrue(!info.isRiffWav());		
        assertEquals(info.getFrequency() , 48000);	
        assertEquals(info.getHeaderBitRate() , 384);
        assertEquals(info.getLayer_Numeric() , 2); 		
        assertEquals(info.getLengthInSeconds() , 0); 
        assertEquals(info.getMode() , "Stereo");	
        assertEquals(info.getVersion_Numeric() , 1.0d, 0D);
        assertEquals(info.getNumberOfFrames() , 27); //24-encspot 27-winamp/////////////////////////////////////
        assertEquals(info.getEmphasis() , "none");
        assertTrue(!info.hasVariableBitRate());

        /*
          mpg1layIII_58k-VBRq30_f1211_441hz_joint_XingTAG_sample.mp3
        */
        info  = newMP3Info(file[5]);
        assertEquals(info.getFileSize(), 232295L); 
        assertEquals(info.getBitRate() , 58); //59-winamp(wrong) 58-encspot	
        assertTrue(info.isCoprighted());		
        assertTrue(info.isOriginal());	
        assertTrue(!info.isPadded()); 	
        assertTrue(!info.isPrivate());	
        assertTrue(!info.isProtected());	
        assertTrue(!info.isRiffWav());		
        assertEquals(info.getFrequency() , 44100);	
        assertEquals(info.getHeaderBitRate() , 48);
        assertEquals(info.getLayer_Numeric() , 3); 		
        assertEquals(info.getLengthInSeconds() , 31); 
        assertEquals(info.getMode() , "Joint Stereo");	
        assertEquals(info.getVersion_Numeric() , 1.0d, 0D);
        assertEquals(info.getNumberOfFrames() , 1212);
        assertEquals(info.getEmphasis() , "none");
        assertTrue(info.hasVariableBitRate());
		assertEquals(info.getVBRHeader().getScale() , 30);
		assertNotNull(info.getVBRHeader().getTableOfContents());
			
        /*
          mpg1layII_0h_192k_frame44_44100hz_joint_CRCfl11.mp2
        */	
        info  = newMP3Info(file[8]);
        assertEquals(info.getFileSize(), 30720L);
        assertEquals(info.getBitRate() , 192);		
        assertTrue(!info.isCoprighted());	
        assertTrue(!info.isOriginal());	
        assertTrue(info.isPadded()); 	
        assertTrue(!info.isPrivate());	
        assertTrue(info.isProtected());	
        assertTrue(!info.isRiffWav());		
        assertEquals(info.getFrequency() , 44100);	
        assertEquals(info.getHeaderBitRate() , 192);
        assertEquals(info.getLayer_Numeric() , 2);		
        assertEquals(info.getLengthInSeconds() , 1);	
        assertEquals(info.getMode() , "Stereo");	
        assertEquals(info.getVersion_Numeric() , 1.0d, 0D);
        assertEquals(info.getNumberOfFrames() , 48); //44-encspot 49-winamp/////////////////////////////////////
        assertEquals(info.getEmphasis() , "none");
        assertTrue(!info.hasVariableBitRate());
	
        /*
          mpg2.5layIII_8k-VBRq95_f149_8khz_mono_ID3v2Orig_XingTAGLame.mp3
          !!Can't find Xing tag!! which is why stuff is messed up
        */	
        info  = newMP3Info(file[9]);
        assertEquals(info.getFileSize(), 14336L);
        assertEquals(info.getBitRate() , 32); //18-winamp 8-encspot/////////////////////////////////////////////
        assertTrue(!info.isCoprighted());	
        assertTrue(info.isOriginal());	
        assertTrue(!info.isPadded()); 	
        assertTrue(!info.isPrivate());	
        assertTrue(!info.isProtected());	
        assertTrue(!info.isRiffWav());		
        assertEquals(info.getFrequency() , 8000);	
        assertEquals(info.getHeaderBitRate() , 32);	//18-winamp 8-encspot//////////////////////////////////////
        assertEquals(info.getLayer_Numeric() , 3);		
        assertEquals(info.getLengthInSeconds() , 3); //5-winamp 10-encspot/////////////////////////////////////
        assertEquals(info.getMode() , "Single Channel");	
        assertEquals(info.getVersion_Numeric() , 2.5d, 0D);
        assertEquals(info.getNumberOfFrames() , 24); //149-winamp&encspot//////////////////////////////////////
        assertEquals(info.getEmphasis() , "none");
        assertTrue(!info.hasVariableBitRate());
        //	assertEquals(info.getVBRHeader().getScale() , 95);
        //	assertNotNull(info.getVBRHeader().getTableOfContents());
        /*
          mpg2layI_128k_f54_22050hz_joint_CRCOrig_test33.mp3
        */	
        info  = newMP3Info(file[10]);
        assertEquals(info.getFileSize(), 22572L);
        assertEquals(info.getBitRate() , 128);		
        assertTrue(!info.isCoprighted());	
        assertTrue(info.isOriginal());	
        assertTrue(info.isPadded()); 	
        assertTrue(!info.isPrivate());	
        assertTrue(info.isProtected());	
        assertTrue(!info.isRiffWav());		
        assertEquals(info.getFrequency() , 22050);	
        assertEquals(info.getHeaderBitRate() , 128);
        assertEquals(info.getLayer_Numeric() , 1);		
        assertEquals(info.getLengthInSeconds() , 1);	
        assertEquals(info.getMode() , "Joint Stereo");	
        assertEquals(info.getVersion_Numeric() , 2.0d, 0D);
        assertEquals(info.getNumberOfFrames() , 1); //54-winamp no encspot//////////////////////////////////////
        assertEquals(info.getEmphasis() , "none");
        assertTrue(!info.hasVariableBitRate());
	
        /*
          mpg2layII_16k_f56_24000hz_joint_CRCOrigID3v1&2_test27.mp3
        */	
        info  = newMP3Info(file[11]);
        assertEquals(info.getFileSize(), 4224L);
        assertEquals(info.getBitRate() , 16);		
        assertTrue(!info.isCoprighted());	
        assertTrue(info.isOriginal());	
        assertTrue(!info.isPadded()); 	
        assertTrue(!info.isPrivate());	
        assertTrue(info.isProtected());	
        assertTrue(!info.isRiffWav());		
        assertEquals(info.getFrequency() , 24000);	
        assertEquals(info.getHeaderBitRate() , 16);
        assertEquals(info.getLayer_Numeric() , 2);		
        assertEquals(info.getLengthInSeconds() , 2); //1-winamp 43-musicmatch(correct)//////////////////////////
        assertEquals(info.getMode() , "Joint Stereo");	
        assertEquals(info.getVersion_Numeric() , 2.0d, 0D);
        assertEquals(info.getNumberOfFrames() , 44); // 56-winamp  no encspot//////////////////////////////////
        assertEquals(info.getEmphasis() , "none");
        assertTrue(!info.hasVariableBitRate());
	
	
        /*
          mpgPlus_210k-VBR_f1149_441hz_joint_protectedUnt_NOTm1lay-448k-f540-5015.mpc
        */	
        info  = newMP3Info(file[14]);
        assertEquals(info.getFileSize(), 789752L);
        assertEquals(info.getBitRate() , 448); //448-winamp	210-encspot
        assertTrue(info.isCoprighted());	
        assertTrue(!info.isOriginal());	
        assertTrue(info.isPadded()); 	
        assertTrue(!info.isPrivate());	
        assertTrue(!info.isProtected());	
        assertTrue(!info.isRiffWav());		
        assertEquals(info.getFrequency() , 44100);	
        assertEquals(info.getHeaderBitRate() , 448); //448-winamp 210-encspot
        assertEquals(info.getLayer_Numeric() , 1);		
        assertEquals(info.getLengthInSeconds() , 14); //30-encspot
        assertEquals(info.getMode() , "Joint Stereo");	
        assertEquals(info.getVersion_Numeric() , 1.0d, 0D);
        assertEquals(info.getNumberOfFrames() , 65); // 540-winamp 1149-encspot
        assertEquals(info.getEmphasis() , "50/15 ms");
        assertTrue(!info.hasVariableBitRate()); //encspot true
	
        /*
          mpg4_golem160x90first120.avi
          MPEG 4 files contain a 'possible' MP3 header

          info  = newMP3Info(file[18] );
          assertEquals(info.getFileSize(), 743933L);
          assertEquals(info.getBitRate() , 40);		
          assertTrue(!info.isCoprighted());	
          assertTrue(!info.isOriginal());	
          assertTrue(!info.isPadded()); 	
          assertTrue(info.isPrivate());	
          assertTrue(!info.isProtected());	
          assertTrue(!info.isRiffWav());		
          assertEquals(info.getFrequency() , 8000);	
          assertEquals(info.getHeaderBitRate() , 40);
          assertEquals(info.getLayer_Numeric() , 2);		
          assertEquals(info.getLengthInSeconds() , 555);	
          assertEquals(info.getMode() , "Joint Stereo");	
          assertEquals(info.getVersion_Numeric() , 2.5d, 0D);
          assertEquals(info.getNumberOfFrames() , 3857); //39626us - 39624 winamp
          assertEquals(info.getEmphasis() , "none");
          assertTrue(!info.hasVariableBitRate());
        */	
        /*
          corruptFileWithBadHeaders.mp3
        */
        try {
            info  = newMP3Info(file[19]);
            fail("Corrupt file with headers passed!"); //shouldn't make it
        }
        catch (Exception e) {}
	
    }
    /**
     * timing new code
     */
    public void notestTIME()
        throws Exception {

        int i = runs;
        long start = 0;
	
        newMP3Info(file[5]);
	
        start = System.currentTimeMillis();
        while (--i >= 0) {
		
            newMP3Info(file[i%9]);
	
        }

        System.out.println("NEW time:" + (System.currentTimeMillis()-start));
    }
}
