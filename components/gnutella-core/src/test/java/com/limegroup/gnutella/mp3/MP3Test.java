package com.limegroup.gnutella.mp3;


import junit.framework.*;
import de.ueberdosis.mp3info.*;
import com.limegroup.gnutella.util.CommonUtils;
import com.sun.java.util.collections.*;
/**
 * Test MP3Info class
 *
 *
 * @author  cHANCE mOORE, ctmoore@gottapee.com - 30 July 2002
 */
//34567890123456789012345678901234567890123456789012345678901234567890123456789
public class MP3Test extends com.limegroup.gnutella.util.BaseTestCase {
	
	private static final int runs = 900;
	
	private static final String d = "com/limegroup/gnutella/mp3/";
	
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
    
    private ExtendedID3Tag newMP3Info(String loc) throws Exception {
        de.ueberdosis.mp3info.ID3Reader reader = 
            new de.ueberdosis.mp3info.ID3Reader(CommonUtils.getResourceFile(loc).getPath());
        return reader.getExtendedID3Tag();
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
        
        de.ueberdosis.util.OutputCtr.setLevel(-1);
        ExtendedID3Tag info = null;
        
        /*
          mpg1layI_0h_448k_frame14_48000hz_dual_fl5Orig.mp3
        */
        info  = newMP3Info(file[1]);
        assertEquals(info.getSize(), 21952L);
        assertEquals(info.getBitrateI() , 448);		
        assertTrue(!info.getCopyright());		
        assertTrue(info.getOriginal());	
        assertTrue(!info.getPadding()); 	
        assertEquals(info.getFrequencyI() , 48000);	
        assertEquals(info.getLayerI() , 1); 		
        assertEquals(info.getRuntime() , 0); 
        assertEquals(info.getChannelModeS() , "Dual Channel (Stereo)");	
        assertEquals(info.getEmphasisS() , "none");
        //        assertTrue(!info.hasVariableBitRate());
        
        /*
          mpg1layI_32k_f256_441hz_dual_5015emph_CRCcopyOrig_sectest.mp3
        */
        info  = newMP3Info(file[2]);
        assertEquals(info.getSize(), 26645L);
        assertEquals(info.getBitrateI() , 192);		
        assertTrue(info.getCopyright());		
        assertTrue(info.getOriginal());	
        assertTrue(!info.getPadding()); 	
        assertEquals(info.getFrequencyI() , 44100);	
        assertEquals(info.getLayerI() , 1); 		
        assertEquals(info.getRuntime() , 1); 
        assertEquals(info.getChannelModeS() , "Dual Channel (Stereo)");	
        assertEquals(info.getEmphasisS() , "50/15ms");
        //        assertTrue(!info.hasVariableBitRate());
        
        /*
          mpg1layII_384k_f24_48hz_stereo_CRCOrig_Gogo1sectest.mp3
        */
        info  = newMP3Info(file[3]);
        assertEquals(info.getSize(), 31104L);
        assertEquals(info.getBitrateI() , 384);		
        assertTrue(!info.getCopyright());		
        assertTrue(info.getOriginal());	
        assertTrue(!info.getPadding()); 	
        assertEquals(info.getFrequencyI() , 48000);	
        assertEquals(info.getLayerI() , 2); 		
        assertEquals(info.getRuntime() , 0); 
        assertEquals(info.getChannelModeS() , "Stereo");	
        assertEquals(info.getEmphasisS() , "none");
        //        assertTrue(!info.hasVariableBitRate());

        /*
          mpg1layIII_58k-VBRq30_f1211_441hz_joint_XingTAG_sample.mp3
        */
        info  = newMP3Info(file[5]);
        assertEquals(info.getSize(), 232295L); 
        assertEquals(info.getBitrateI() , 192); //we current dont' support VBR
        assertTrue(info.getCopyright());		
        assertTrue(info.getOriginal());	
        assertTrue(!info.getPadding()); 	
        assertEquals(info.getFrequencyI() , 44100);	
        assertEquals(info.getLayerI() , 3); 		
        assertEquals(info.getRuntime() , 31); 
        assertEquals(info.getChannelModeS() , "Joint Stereo (Stereo)");	
        assertEquals(info.getEmphasisS() , "none");
        //        assertTrue(info.hasVariableBitRate());
        //		assertEquals(info.getVBRHeader().getScale() , 30);
        //		assertNotNull(info.getVBRHeader().getTableOfContents());
			
        /*
          mpg1layII_0h_192k_frame44_44100hz_joint_CRCfl11.mp2
        */	
        info  = newMP3Info(file[8]);
        assertEquals(info.getSize(), 30720L);
        assertEquals(info.getBitrateI() , 192);		
        assertTrue(!info.getCopyright());	
        assertTrue(!info.getOriginal());	
        assertTrue(info.getPadding()); 	
        assertEquals(info.getFrequencyI() , 44100);	
        assertEquals(info.getLayerI() , 2);		
        assertEquals(info.getRuntime() , 1);	
        assertEquals(info.getChannelModeS() , "Stereo");	
        assertEquals(info.getEmphasisS() , "none");
        //        assertTrue(!info.hasVariableBitRate());
	
        /*
          mpg2.5layIII_8k-VBRq95_f149_8khz_mono_ID3v2Orig_XingTAGLame.mp3
          !!Can't find Xing tag!! which is why stuff is messed up
        */	
        info  = newMP3Info(file[9]);
        assertEquals(info.getSize(), 14336L);
        assertEquals(info.getBitrateI() , 32); //18-winamp 8-encspot/////////////////////////////////////////////
        assertTrue(!info.getCopyright());	
        assertTrue(info.getOriginal());	
        assertTrue(!info.getPadding()); 	
        assertEquals(info.getFrequencyI() , 16000);	
        assertEquals(info.getLayerI() , 3);		
        assertEquals(info.getRuntime() , 10); //5-winamp 10-encspot/////////////////////////////////////
        assertEquals(info.getChannelModeS() , "Single Channel (Mono)");	
        assertEquals(info.getEmphasisS() , "none");
        //        assertTrue(!info.hasVariableBitRate());
        //	assertEquals(info.getVBRHeader().getScale() , 95);
        //	assertNotNull(info.getVBRHeader().getTableOfContents());
        /*
          mpg2layI_128k_f54_22050hz_joint_CRCOrig_test33.mp3
        */	
        info  = newMP3Info(file[10]);
        assertEquals(info.getSize(), 22572L);
        assertEquals(info.getBitrateI() , 256);		
        assertTrue(!info.getCopyright());	
        assertTrue(info.getOriginal());	
        assertTrue(info.getPadding()); 	
        assertEquals(info.getFrequencyI() , 44100);	
        assertEquals(info.getLayerI() , 1);		
        assertEquals(info.getRuntime() , 0);	
        assertEquals(info.getChannelModeS() , "Joint Stereo (Stereo)");	
        assertEquals(info.getEmphasisS() , "none");
        //        assertTrue(!info.hasVariableBitRate());
	
        /*
          mpg2layII_16k_f56_24000hz_joint_CRCOrigID3v1&2_test27.mp3
        */	
        info  = newMP3Info(file[11]);
        assertEquals(info.getSize(), 4224L);
        assertEquals(info.getBitrateI() , 48);		
        assertTrue(!info.getCopyright());	
        assertTrue(info.getOriginal());	
        assertTrue(!info.getPadding()); 	
        assertEquals(info.getFrequencyI() , 48000);	
        assertEquals(info.getLayerI() , 2);		
        assertEquals(info.getRuntime() , 0); //1-winamp 43-musicmatch(correct)//////////////////////////
        assertEquals(info.getChannelModeS() , "Joint Stereo (Stereo)");	
        assertEquals(info.getEmphasisS() , "none");
        //        assertTrue(!info.hasVariableBitRate());
	
	
        /*
          mpgPlus_210k-VBR_f1149_441hz_joint_protectedUnt_NOTm1lay-448k-f540-5015.mpc
        */	
        info  = newMP3Info(file[14]);
        assertEquals(info.getSize(), 789752L);
        assertEquals(info.getBitrateI() , 448); //448-winamp	210-encspot
        assertTrue(info.getCopyright());	
        assertTrue(!info.getOriginal());	
        assertTrue(info.getPadding()); 	
        assertEquals(info.getFrequencyI() , 44100);	
        assertEquals(info.getLayerI() , 1);		
        assertEquals(info.getRuntime() , 22); //30-encspot
        assertEquals(info.getChannelModeS() , "Joint Stereo (Stereo)");	
        assertEquals(info.getEmphasisS() , "50/15ms");
        //        assertTrue(!info.hasVariableBitRate()); //encspot true
	
        /*
          mpg4_golem160x90first120.avi
          MPEG 4 files contain a 'possible' MP3 header

          info  = newMP3Info(file[18] );
          assertEquals(info.getSize(), 743933L);
          assertEquals(info.getBitrateI() , 40);		
          assertTrue(!info.getCopyright());	
          assertTrue(!info.getOriginal());	
          assertTrue(!info.getPadding()); 	
          assertEquals(info.getFrequencyI() , 8000);	
          assertEquals(info.getLayerI() , 2);		
          assertEquals(info.getRuntime() , 555);	
          assertEquals(info.getChannelModeS() , "Joint Stereo");	
          assertEquals(info.getNumberOfFrames() , 3857); //39626us - 39624 winamp
          assertEquals(info.getEmphasisS() , "none");
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
