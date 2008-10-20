package com.limegroup.gnutella.filters;

import java.util.HashSet;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.google.inject.Singleton;
import com.limegroup.gnutella.Response;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.messages.BadPacketException;
import com.limegroup.gnutella.messages.Message;
import com.limegroup.gnutella.messages.QueryReply;
import com.limegroup.gnutella.messages.QueryRequest;

@Singleton
public class URNFilter implements SpamFilter {
    
    private static final Log LOG = LogFactory.getLog(URNFilter.class);
    private static final HashSet<URN> blacklist = new HashSet<URN>();
    // These URNs were collected by sending out queries like
    // 'eiuhf dkghwkejgh dkfjhek' that should not match any real files
    private static final String[] badURNs = new String[]{
        // URNs sent in response to every query, echoing the search terms
        "urn:sha1:272AHJPHS4EXN6NVSWNSYHUNFNTA6TCQ",
        "urn:sha1:2EUWHZTCVSITJRQDVMYQX4PVNOXCVJ4T",
        "urn:sha1:34POLIWE2LFXINC36JOXZHF2HGFHUVCZ",
        "urn:sha1:3ELIDHUURNVAOAEES32N2ETJ5AJ62GSX",
        "urn:sha1:3FK32IDBGDW7LJOYBOFQDIKNJJORM24P",
        "urn:sha1:3RW24MQ76JT2BFJPI6SDDXIWHUFA2KSC",
        "urn:sha1:3YCYXU7N3F77SXCMVM2OG57BFCGMFBGE",
        "urn:sha1:4AGEFDA2CZ3PHYQSJPDRNTZZKD54CRZ6",
        "urn:sha1:4IUO6VHPEG65C3PMNRW5JAZHXOJSICFG",
        "urn:sha1:4JOSE2R6DQJ2ROXGM327PCAWSSEI6TUG",
        "urn:sha1:52KRXO3KNVWTPTPMWMNBB2XVAPFYTYF3",
        "urn:sha1:5T6HGYBCEXZMQ2NCAITILE5SLBMX7IZH",
        "urn:sha1:62D2NANCSJDDFTFT2QGX4RVOPBSHTTGH",
        "urn:sha1:6F3VL2GOGPKWGAQZ5YY7RYD7OI7LWA2M",
        "urn:sha1:6FPIGULR3OMOMOESYFELHS2YVV2M7ZGF",
        "urn:sha1:AAJP5DRNKV2A4NCGSBRUR4DDTNTXW2BY",
        "urn:sha1:AN5N6Z6CRLK6KM46MINPNEPRBGE3YYOA",
        "urn:sha1:ATY26E5YGCPHOEUNWAIVVNO4SLXEKDQS",
        "urn:sha1:BAYURWNMQAF5DGVPMRQRLRIZ77R3AGDJ",
        "urn:sha1:BO7P7237ODXDDBRBCVHDXVH63PLDHNRB",
        "urn:sha1:BWCZOGLWZ25QIRLU3HOZE4A3UTLSCB3N",
        "urn:sha1:D7F4NCV5VCIN56VTZEWJB5B7WAW3OCKL",
        "urn:sha1:EBKSAC3OY66LTLYL2Z6LYMSV2ZOT7DVD",
        "urn:sha1:ETQSA3GEBHAY536PRM655JHIN42AQ5LM",
        "urn:sha1:FXL6O6QTWAS4XNL54E4ELU4M42XDDXFF",
        "urn:sha1:FZMBTBIMI7BMTNSDHYAQTVL4NR2AZXRS",
        "urn:sha1:G5ZU5CNC2IY5JWKRCWKA3ZXKJX5JTQ7Y",
        "urn:sha1:GBWWEZ3UWXRG763YJSUW6LRXPTDE6VWX",
        "urn:sha1:GD7WND75YYIB57HA3W66YMOJP5YAOAZQ",
        "urn:sha1:GOZG2MFW5XWQHWRIFS62FWVSPDD7YUFK",
        "urn:sha1:HJJGKBHZVQLYU5ABSG6TB3A7NER6AYFC",
        "urn:sha1:HVIXE33CM7J6IXTWFSD43QGGDL566QEU",
        "urn:sha1:I2ETA5GIEVJPJKUQMFD2FX6K3HYLLWEM",
        "urn:sha1:I6NJRMKHZQMWEM2MKWKLKFKAF6BZHLPN",
        "urn:sha1:IAF3RDA6PC62DZ7HT3LFYA6HBHX4Y2CZ",
        "urn:sha1:IF6CFKWMFCSHHXEV2EVPQERMT3JVBFHY",
        "urn:sha1:IPQNZANKU7BETN3Z6TPAP3KTCBYQCFH7",
        "urn:sha1:JQBBZKR3CWASGTVX6YD55GTMOVQYMCUP",
        "urn:sha1:JQVJ2YXY4YPW4RL7DIETV7IBTNC4JKV6",
        "urn:sha1:JYMLNADQONAK3MUH7EQM57ADRE6CPRZA",
        "urn:sha1:KDWJ6POQONIIPSVRV4BWPDEPXL6RQCG5",
        "urn:sha1:L2LWJF35GRNVAOXCNDW6VVHWNNUY3BRD",
        "urn:sha1:LD6ELITT6CPRQ37ZGXZVQPUO26Z4ZZ6C",
        "urn:sha1:LWZHFCEK3UWSVTAY3XBD5PNFW3VDWFFR",
        "urn:sha1:KHXDJGPNSEXFP6HKXF5UMF23JGC2CYUK",
        "urn:sha1:M45MNRNQKANXDJDRM5RRNEGIMLEVQTRU",
        "urn:sha1:MA6G2ZGEQZALJJZJY7GSG3SQ7C23ZNB3",
        "urn:sha1:MF2PHNLWS5Q4CGJ35HT4DNLIHEN5GN7O",
        "urn:sha1:MFJJ3GVPQKKY7ZIELFOK2RJ2Z53GJ74F",
        "urn:sha1:NSH3Y6BL5XBYLYYB43ND2DZ3CQBI2L5I",
        "urn:sha1:OIFR7TOZMF7ZTJGUWIZCIIAHQE4AUG2K",
        "urn:sha1:P53HONQTHF4LIWKWQZTD5RESX7DWPJUU",
        "urn:sha1:PLKMRUZ7COPB7G2XQP2CAKR2TVCIBDJC",
        "urn:sha1:PXT4SHVIWGTCI4ZXF5IFWZDHBAW65O42",
        "urn:sha1:PZB2T2DCTUUP6WGLW5RHBVVFZDLGJHAE",
        "urn:sha1:Q6PFCDHG3NX5OZL3CR453RTDXSPXZDO2",
        "urn:sha1:QVHTNDBX3TSP6G4EDLMCPRJ7KSWZHJEX",
        "urn:sha1:QYWDCNFLK4B43TCL72V4HUS3YRT4YZKI",
        "urn:sha1:RA25MMPN5PUQ3EFPZZ3YUD6N6MSC4AOT",
        "urn:sha1:RLZFWTBINJ4MP7U3TUHCNH7TBG4S6CXG",
        "urn:sha1:RZ2A7RHJBWUGA6VEOBRDB57BFFYVZQ3M",
        "urn:sha1:SAUF7WAACKZMIZRF5XNPPEC3BPDBOSHJ",
        "urn:sha1:SDGLABQUER34NCWHZGXLZT4G2LPORED4",
        "urn:sha1:SEJTKUJNTOZLXIPIACHX33G5INTMCFH5",
        "urn:sha1:UO3FR5RVVAWDCOV7B5WEUAANTDLM65CT",
        "urn:sha1:VH3WVVIPHFT4TDRPFTWVVES2AJ7JHDUP",
        "urn:sha1:VHKHYERJO5QE7QER74MUKQM5NM4X7L2K",
        "urn:sha1:WN3GVQWCD5TQK2LMCWYNLAHHNKY37HOJ",
        "urn:sha1:WNCNE7YXMFEKERERDSZWSXXJMFWI55BQ",
        "urn:sha1:WTY2FNDCRYE7ICR2FBV7ACJR5M4JWL77",
        "urn:sha1:XK3GHUI2ZHMQQH65TKPSTMBHUL5SU4ED",
        "urn:sha1:Y64ZHRNFAYZEOZ2BMGCWY7DV2ZL2HWY4",
        "urn:sha1:ZCOEMP43CYQSRCWUZ6W4DQLY7PBVVARA",
        "urn:sha1:ZEJZYDS34GVMZJDTCM2QTIYVM2R4BYN4",
        "urn:sha1:ZWXTZKDCLY3LNFQ66F3F54Y5XKBO2JTA",
        // URNs sent in response to every query, not echoing the search terms
        "urn:sha1:2HD3WMLTQRTGEEBHTI2VRJ2VD6F4YJG3",
        "urn:sha1:3ZNHTWRVJGOM54CNG635YMSPS6A5RJTS",
        "urn:sha1:4BTM7OX2UUFDWEWI7KGO5YIFT6FSXGD6",
        "urn:sha1:67PRZD66FD7LMRGE2QPZWNJ2JAZ4PNLU",
        "urn:sha1:7WTLN26RUCC5PRAQIWMQ5HFAY5GRJT65",
        "urn:sha1:BTKCJRUKE3G4GKIIE3G6E7FX2XTYTKNO",
        "urn:sha1:D7SMP7PEFT5GCGJ7CZ6PQJIWWV2YNE2P",
        "urn:sha1:HPMT4H3QLJBLYXJA4YF6BTEUNZCT73OA",
        "urn:sha1:IZ4NPZB7TN2YHDWZWPGCJD645SUVXD4S",
        "urn:sha1:LWOFNGNM75QSIAFSDT2MJJXD7XLGN7AE",
        "urn:sha1:XOFT5UKUXK3OV5IY5EYYFKSTJTWWWXKF",
        "urn:sha1:YIINM2KDJAJVEG5SINIBGHL4YAEGEGB5"
    };
    
    public URNFilter() {
        try {
            for(String s : badURNs)
                blacklist.add(URN.createSHA1Urn(s));
        } catch (Exception x) {
            if(LOG.isDebugEnabled())
                LOG.debug("Error creating URN blacklist: " + x);
        }
    }
    
    @Override
    public boolean allow(Message m) {
        if(m instanceof QueryReply) {
            QueryReply q = (QueryReply)m;
            try {
                for(Response r : q.getResultsArray()) {
                    for(URN u : r.getUrns()) {
                        if(blacklist.contains(u)) {
                            if(LOG.isDebugEnabled())
                                LOG.debug("Filtering response with URN " + u);
                            return false;
                        }
                    }
                }
                return true;
            } catch (BadPacketException bpe) {
                return true;
            }
        } else if(m instanceof QueryRequest) {
            QueryRequest q = (QueryRequest)m;
            for(URN u : q.getQueryUrns()) {
                if(blacklist.contains(u)) {
                    if(LOG.isDebugEnabled())
                        LOG.debug("Filtering request with URN " + u);
                    return false;
                }
            }
            return true;
        }
        return true; // Don't block other kinds of messages
    }
}