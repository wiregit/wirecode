package com.limegroup.gnutella.simpp;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.DefaultedHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.limewire.io.IOUtils;
import org.limewire.util.Base32;
import org.limewire.util.Clock;
import org.limewire.util.CommonUtils;
import org.limewire.util.FileUtils;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.limegroup.gnutella.ApplicationServices;
import com.limegroup.gnutella.NetworkUpdateSanityChecker;
import com.limegroup.gnutella.ReplyHandler;
import com.limegroup.gnutella.NetworkUpdateSanityChecker.RequestType;
import com.limegroup.gnutella.http.HTTPHeaderName;
import com.limegroup.gnutella.http.HttpClientListener;
import com.limegroup.gnutella.http.HttpExecutor;
import com.limegroup.gnutella.settings.SimppSettingsManager;
import com.limegroup.gnutella.settings.UpdateSettings;
import com.limegroup.gnutella.util.LimeWireUtils;

/**
 * Used for managing signed messages published by LimeWire, and changing settings
 * as necessary.
 */
@Singleton
public class SimppManagerImpl implements SimppManager {
    
    private static final Log LOG = LogFactory.getLog(SimppManagerImpl.class);
    
    private static int MIN_VERSION = 3;
   
    private static final String FILENAME = "simpp.xml";    
    private static final Random RANDOM = new Random();
    private static final int IGNORE_ID = Integer.MAX_VALUE;
   
    /** Cached Simpp bytes in case we need to sent it out on the wire */
    private volatile byte[] _lastBytes = new byte[0];
    private volatile String _lastString = "";    
    private volatile int _lastId = MIN_VERSION;
    
    /** If an HTTP failover update is in progress */
    private final HttpRequestControl httpRequestControl = new HttpRequestControl();

    private final List<SimppListener> listeners = new CopyOnWriteArrayList<SimppListener>();
    private final CopyOnWriteArrayList<SimppSettingsManager> simppSettingsManagers;    
    private final Provider<NetworkUpdateSanityChecker> networkUpdateSanityChecker;
    private final ApplicationServices applicationServices;    
    private final Clock clock;
    private final Provider<HttpExecutor> httpExecutor;
    private final ScheduledExecutorService backgroundExecutor;
    private final Provider<HttpParams> defaultParams;
    
    private volatile List<String> maxedUpdateList = Arrays.asList("http://simpp1.limewire.com/v2/simpp.def",
            "http://simpp2.limewire.com/v2/simpp.def",
            "http://simpp3.limewire.com/v2/simpp.def",
            "http://simpp4.limewire.com/v2/simpp.def",
            "http://simpp5.limewire.com/v2/simpp.def",
            "http://simpp6.limewire.com/v2/simpp.def",
            "http://simpp7.limewire.com/v2/simpp.def",
            "http://simpp8.limewire.com/v2/simpp.def",
            "http://simpp9.limewire.com/v2/simpp.def",
            "http://simpp10.limewire.com/v2/simpp.def");
    private volatile int minMaxHttpRequestDelay = 1000 * 60;
    private volatile int maxMaxHttpRequestDelay = 1000 * 60 * 30;
    private volatile int silentPeriodForMaxHttpRequest = 1000 * 60 * 5;
    
    private static enum UpdateType {
        FROM_NETWORK, FROM_DISK, FROM_HTTP;
    }
    
    @Inject
    public SimppManagerImpl(Provider<NetworkUpdateSanityChecker> networkUpdateSanityChecker, Clock clock,
            ApplicationServices applicationServices, Provider<HttpExecutor> httpExecutor,
            @Named("backgroundExecutor") ScheduledExecutorService backgroundExecutor,
            @Named("defaults") Provider<HttpParams> defaultParams) {
        this.networkUpdateSanityChecker = networkUpdateSanityChecker;
        this.clock = clock;
        this.applicationServices = applicationServices;
        this.simppSettingsManagers = new CopyOnWriteArrayList<SimppSettingsManager>();
        this.httpExecutor = httpExecutor;
        this.backgroundExecutor = backgroundExecutor;
        this.defaultParams = defaultParams;
        initialize(); // TODO: move elsewhere
    }
    
    List<String> getMaxUrls() {
        return maxedUpdateList;
    }
    
    void setMaxUrls(List<String> urls) {
        this.maxedUpdateList = urls;
    }
    
    int getMinHttpRequestUpdateDelayForMaxFailover() {
        return minMaxHttpRequestDelay;
    }
    
    int getMaxHttpRequestUpdateDelayForMaxFailover() {
        return maxMaxHttpRequestDelay;
    }
    
    void setMinHttpRequestUpdateDelayForMaxFailover(int min) {
        minMaxHttpRequestDelay = min;
    }
    
    void setMaxHttpRequestUpdateDelayForMaxFailover(int max) {
        maxMaxHttpRequestDelay = max;
    }
    
    int getSilentPeriodForMaxHttpRequest() {
        return silentPeriodForMaxHttpRequest;
    }
    
    void setSilentPeriodForMaxHttpRequest(int silentPeriodForMaxHttpRequest) {
        this.silentPeriodForMaxHttpRequest = silentPeriodForMaxHttpRequest;
    }
    
    
    public void initialize() {
        LOG.trace("Initializing SimppManager");
        backgroundExecutor.execute(new Runnable() {
            public void run() {
                handleDataInternal(FileUtils.readFileFully(new File(CommonUtils
                        .getUserSettingsDir(), FILENAME)), UpdateType.FROM_DISK, null);
            }
        });
    }
        
    public int getVersion() {
        return _lastId;
    }
    
    /**
     * @return the cached value of the simpp bytes. 
     */ 
    public byte[] getSimppBytes() {
        return _lastBytes;
    }

    public String getPropsString() {
        return _lastString;
    }
    
    public void addSimppSettingsManager(SimppSettingsManager simppSettingsManager) {
        simppSettingsManagers.add(simppSettingsManager);
    }

    public List<SimppSettingsManager> getSimppSettingsManagers() {
        return simppSettingsManagers;
    }
    
    public void addListener(SimppListener listener) {
        listeners.add(listener);
    }
    
    public void removeListener(SimppListener listener) {
        listeners.remove(listener);
    }
    
    public void checkAndUpdate(final ReplyHandler handler, final byte[] data) {
        if(data != null) {
            backgroundExecutor.execute(new Runnable() {
                public void run() {
                    LOG.trace("Parsing new data...");
                    handleDataInternal(data, UpdateType.FROM_NETWORK, handler);
                }
            });
        }
    }
    
    private void handleDataInternal(byte[] data, UpdateType updateType, ReplyHandler handler) {
        if (data == null) {
            if (updateType == UpdateType.FROM_NETWORK && handler != null)
                networkUpdateSanityChecker.get().handleInvalidResponse(handler, RequestType.SIMPP);
            LOG.warn("No data to handle.");
            return;
        }
        
        SimppDataVerifier verifier=new SimppDataVerifier(data);
        if(!verifier.verifySource()) {
            if(updateType == UpdateType.FROM_NETWORK && handler != null)
                networkUpdateSanityChecker.get().handleInvalidResponse(handler, RequestType.SIMPP);
            LOG.warn("Couldn't verify signature on data.");
            return;
        }
        
        if(updateType == UpdateType.FROM_NETWORK && handler != null)
            networkUpdateSanityChecker.get().handleValidResponse(handler, RequestType.SIMPP);
        
        SimppParser parser = null;
        try {
            parser = new SimppParser(verifier.getVerifiedData());
        } catch(IOException iox) {
            LOG.error("IOX parsing simpp data", iox);
            return;
        }
        
        switch(updateType) {
        case FROM_NETWORK:
            if(parser.getVersion() == IGNORE_ID) {
                if(_lastId != IGNORE_ID)
                    doHttpMaxFailover();
            } else if(parser.getVersion() > _lastId) {
                storeAndUpdate(data, parser, updateType);
            }
            break;
        case FROM_DISK:
            if(parser.getVersion() > _lastId) {
                storeAndUpdate(data, parser, updateType);
            }
            break;
        case FROM_HTTP:
            if(parser.getVersion() >= _lastId) {
                storeAndUpdate(data, parser, updateType);
            }
            break;
        }
    }
    
    private void storeAndUpdate(byte[] data, SimppParser parser, UpdateType updateType) {
        if(LOG.isTraceEnabled())
            LOG.trace("Retrieved new data from: " + updateType + ", storing & updating");
        if(parser.getVersion() == IGNORE_ID && updateType == UpdateType.FROM_NETWORK)
            throw new IllegalStateException("shouldn't be here!");
        
        if(updateType == UpdateType.FROM_NETWORK && httpRequestControl.isRequestPending())
            return;
        
        _lastId = parser.getVersion();
        _lastBytes = data;
        _lastString = parser.getPropsData();
        
        if(updateType != UpdateType.FROM_DISK) {
            FileUtils.verySafeSave(CommonUtils.getUserSettingsDir(), FILENAME, data);
            for(SimppSettingsManager ssm : simppSettingsManagers)
                ssm.updateSimppSettings(_lastString);
            for (SimppListener listener : listeners)
                listener.simppUpdated(_lastId);
        }
    }
    
    private void doHttpMaxFailover() {
        long maxTimeAgo = clock.now() - silentPeriodForMaxHttpRequest; 
        if(!httpRequestControl.requestQueued(HttpRequestControl.RequestReason.MAX) &&
                UpdateSettings.LAST_SIMPP_FAILOVER.getValue() < maxTimeAgo) {
            int rndDelay = RANDOM.nextInt(maxMaxHttpRequestDelay) + minMaxHttpRequestDelay;
            final String rndUri = maxedUpdateList.get(RANDOM.nextInt(maxedUpdateList.size()));
            LOG.debug("Scheduling http max failover in: " + rndDelay + ", to: " + rndUri);
            backgroundExecutor.schedule(new Runnable() {
                public void run() {
                    String url = rndUri;
                    try {
                        launchHTTPUpdate(url);
                    } catch (URISyntaxException e) {
                        httpRequestControl.requestFinished();
                        httpRequestControl.cancelRequest();
                        LOG.warn("uri failure", e);
                    }
                }
            }, rndDelay, TimeUnit.MILLISECONDS);
        } else {
            LOG.debug("Ignoring http max failover.");
        }
    }
    
    /**
     * Launches an http update to the failover url.
     */
    private void launchHTTPUpdate(String url) throws URISyntaxException {
        if (!httpRequestControl.isRequestPending())
            return;
        LOG.debug("about to issue http request method");
        HttpGet get = new HttpGet(LimeWireUtils.addLWInfoToUrl(url, applicationServices.getMyGUID()));
        get.addHeader("User-Agent", LimeWireUtils.getHttpServer());
        get.addHeader(HTTPHeaderName.CONNECTION.httpStringValue(),"close");
        httpRequestControl.requestActive();
        HttpParams params = new BasicHttpParams();
        HttpConnectionParams.setConnectionTimeout(params, 10000);
        HttpConnectionParams.setSoTimeout(params, 10000);
        params = new DefaultedHttpParams(params, defaultParams.get());
        httpExecutor.get().execute(get, params, new RequestHandler());
    }
    
    public byte[] getOldUpdateResponse() {
        return Base32.decode("I5AVOQKFIZCDITCQIRAVSVZUI5AUKMSWKNCDGSCZGVLVCWJSIREFARSFK5KFESKDINIUWVSOKQ3UCVKRGRAUCWKOK43FKSSBINMUMTZXKZEVKN2PJRDFKND4HRZWS3LQOA7DY5TFOJZWS33OHYZDCNBXGQ4DGNRUG46C65TFOJZWS33OHY6HA4TPOBZT4PBBLNBUIQKUIFNSGU3VNYQEC4DSEAYTGIBRGY5DCOJ2GM4CARKEKQQDEMBQHAGQUY3PNZ2GK3TUFZQXK5DIN5ZGS5DJMVZT2ZTTMVZHMMJONRUW2ZLXNFZGKLTDN5WVYORRGAYDAMANBJWWC6C7ONVWS4C7MFRWWPJSBUFFGZLBOJRWQU3FOR2GS3THOMXGY2LNMVIVEUCFNZ2HE2LFOM6WY2LNMU5XO2LSMU5WY2LNMV3WS4TFHNYHE3Z3NRUW2ZLXNFZGK4DSN4GQUVKJFZUW45DSN5BWY2LDNNGGS3TLHVUHI5DQLQ5C6L3XO53S43DJNVSXO2LSMUXGG33NF5UW4Y3MNFSW45BPH5ZXIYLHMVOD22LOORZG6JTSMVZW65LSMNSVYPLOMV2HO33SNMGQURCIKQXECY3UNF3GKUTPOV2GKVDBMJWGKVTFOJZWS33OHUYQ2CSEJBKC4QTPN52HG5DSMFYEQ33TORZT2NZWFY4C4NRXFYZDOXB2GYYDAMQNBJCEQVCTMV2HI2LOM5ZS4RDJONQWE3DFIREFIPLUOJ2WKDIKKJSXG5LMORIGC3TFNQXFGZLBOJRWQV3BOJXGS3THHV5TA7JAKZUXG2LUEBGGS3LFK5UXEZJOMNXW2IDGN5ZCA2LNOBXXE5DBNZ2CA5LQMRQXIZLTFYGQUU3FMFZGG2CTMV2HI2LOM5ZS43DJNVSVGZLBOJRWQVDFOJWXGPLMNFWWKO3XNFZGKO3MNFWWK53JOJSTW4DSN45WY2LNMV3WS4TFOBZG6OYNBJWGC43UIJ2WOVTFOJZWS33OHU2C4MJWFY3A2CSEJBKC4UDVMJWGS43IIFWHITDPMNZT25DSOVSQ2CSNN5VGS5DPFZBG633UON2HEYLQKRUW2ZLPOV2D2MRUGAYDAMANBJJVOVCCOJXXO43FOJJWK5DUNFXGO4ZOON3XIQTSN53XGZLSKRUXI3DFK5UXI2CBNVYHGPKOMV3SAQBAEZGGS3LFBUFFKSJOMFTHIZLSKNSWC4TDNBBWC3SMNFXGWPLUOJ2WKDIKKVES4YLGORSXEU3FMFZGG2CDNRUWG22MNFXGWPLIOR2HAXB2F4XXO53XFZWGS3LFO5UXEZJOMNXW2L3JNZRWY2LFNZ2C6P3TORQWOZK4HVQWM5DFOITHEZLTN52XEY3FLQ6W4ZLUO5XXE2YNBJ2XAZDBORSUI33XNZWG6YLEIRSWYYLZHUYTINBQGAYDAMINBJKGQZLYFZHG6ZDFKBZGKZTJPBSXGPLTNAGQURCIKQXE22LOKBQXG43JOZSUYZLBMZAXMZLSMFTWKVLQORUW2ZJ5GYYDAMBQBUFHK4DEMF2GKTLJNZAXI5DFNVYHI4Z5GE4TSOINBJKUSLTBMZ2GK4STMVQXEY3IKVJEYPLIOR2HAXB2F4XWG3DJMVXHI4DJPAXGY2LNMV3WS4TFFZRW63JPOBUXQL3BMZ2GK4STMVQXEY3IBUFEG33ONZSWG5DJN5XFGZLUORUW4Z3TFZIVEVCTNF5GKPJRGI4A2CTVOBSGC5DFKJSXI4TZIRSWYYLZHUYTQMBQGAYDCDIKKVWHI4TBOBSWK4STMV2HI2LOM5ZS43LBPBGGKYLWMVZT2MZQBUFE2ZLTONQWOZKTMV2HI2LOM5ZS4Y3VON2G63KGIRBXE2LUMVZGSYJ5OVYHGO3BORKXAU3FOQ5TYO3DOVYHGO3DKVYFGZLUHM6DWT2SHNHE6VB3NRQXG5DVOA5XEZTUKNSXIOZ6HNAU4RANBJCEQVBOIVXGCYTMMVIGC43TNF3GKTLPMRSVMMR5ORZHKZINBJCEQVBOKB2WE3DJONUFA5LTNBIHE33YNFSXGPLUOJ2WKDIKKVES42LOORZG6Q3BNZGGS3TLHV2HE5LFBUFFYIBLLQQFKSJOMFTHIZLSKNSWC4TDNBKXE3B5NB2HI4C4HIXS6Y3MNFSW45DQNF4C43DJNVSXO2LSMUXGG33NF5YGS6BPMFTHIZLSKNSWC4TDNAGQURCIKQXE2YLYIFWHITDPMNIXKZLSPFAXI5DFNVYHI4Z5GUYDADIKKVWHI4TBOBSWK4STMV2HI2LOM5ZS43LBPBGGKYLWMVZVMMR5GQYA2CSVJEXGCZTUMVZFGZLBOJRWQUDSN5JWQ33XHV2HE5LFBUFEE5LHKNSXI5DJNZTXGLTTMVXGIVDSMVSVG5DPOJQWOZKCOVTXGQTFORQT2ZTBNRZWKDIKIREFILSNNFXECY3UNF3GKSLONF2GSYLMKVYHI2LNMU6TGNRQGAYDAMANBJ2XAZDBORSUO2LWMVKXARTBMN2G64R5GQ4Q2CSNN5VGS5DPFZBHKY3LMV2FEZLGOJSXG2DFOJIGS3THJZSWC4TFON2D2NRQGAYDAMANBJJWKYLSMNUFGZLUORUW4Z3TFZWGS3LFKNUWO3TFMRJGK43QN5XHGZJ5KZKFOUKBIJGFIT2JIFBUCWJTKBHFKWCHLEZEYTSNKZKFQRJTGNLE6QKYI5HTGVCWJ5JFGV2ZGNCEERS2GJMESMSMJVDFUQ2HIM2UIQSEGREFONCMIRNECNRVJRBUCUKBIFDE4UKBIJCEKTKGGJDUGNKBIFAUUTSVIU3EIUKPKZNECQKBKMZUSS2XKBDUMN2ZIFMUKRSKLFAUGQKBIFEFCNCBIFAUCQKXINAU6S2EIJAUIMSHJZHTONZXG43DONZXG43TONZWIFAUKQKBIFCTMWSUI5HEMUSXKNMUYTKFIJDUOUZTJRDEWNKVLBCVUSSBJNBFURZWJFCEET22KFLVGM2EIJGUUV2HJNEUISKNKZNEOS2MKRCE4NKXKFAUCQ2NJJDEOVKLIJBDISCII5AUCQKHIRIUUQSFKFIUORCBJJFFKRKRI5CEWTSFKVHTGWKMI5AVOQKFIZCEKWCBLBEE2WSELJCVGVKDLBFEGQKGJEZFUN2NJ43UMTCJJM3TIUKDINJFIQJSKFKUORKBJVLE6SCEJA3FQS2JJBLEYRJUIVCTGTBSKVJVKTJWIQ3DMRCOJZIVCSCNLJGFGT2OKVLTMM2SGVCUSWKTGRGUEQ2IGQ3UIWKZJRLE2USVK43DIWSBKBBFUV2TJ5KE6TRVJBDUGM2MIZHU4WKHINMTGRSLJZJFOUK2JRHE2RSHI43FSM2CJ5JFKVZWGNJDKRKKKVEESNKEKFEESWCTGY2TGWCPGRMEOWJSJRHE2VRTK5JTIVCGIZNFEVZWGNFFAT2OKJLVCWSMJZGUMWSTGZMUYVSNKJKVONSMKRMU6TSTINCVAURUJVDDEV2JGJGFARKCKFLUONKEJJHDKWCEGJEVISKPKIZEQQKPKJIEMNJTLBHTKWSPJZJFKVZSLJGFQTSGLJDUWTCUIRHDKV2TGY2UYUKNKJIVQSK2JI3U4RSYI5DTGRCKJVLFQSCJJVJEGRKCKFLUSWSDIJGU4MSHKMZTGT2JKJJVQSKZJRFE4UJWKNCTKRCTJ5LFGU2FJFCEUTS2KNDUWNSCGVCUSWKDIVGFUNSIKFMFOQZVJRCU4RSYLBDVAUKBIFAUCQKBIFAUCQKBIFAUCQKBIFAUCQKBIFAUCQKBIEGQUU3FMFZGG2CTMV2HI2LOM5ZS4ZDFONUXEZLTKBQXE5DJMFWFEZLTOVWHI42CMV2GCPLGMFWHGZINBJJVOVCCOJXXO43FOJJWK5DUNFXGO4ZOON3XIQTSN53XGZLSKRUXI3DFHVHGK5ZAIAQEY2LNMUGQURCIKQXEK3TBMJWGKUDBONZWS5TFJVXWIZJ5MZQWY43FBUFEG33ONZSWG5DJN5XFGZLUORUW4Z3TFZEWI3DFINXW43TFMN2GS33OOM6TEDIKIREFIU3FOR2GS3THOMXFAZLSONUXG5CEMF2GCYTBONST2ZTBNRZWKDIKLQQCWXBAKVES4YLGORSXEU3FMFZGG2CDMFXEY2LONM6XI4TVMUGQURTJNR2GK4STMV2HI2LOM5ZS43LBPBAWY5DTKBSXEUTFONYG63TTMU6TCMINBJJWKYLSMNUFGZLUORUW4Z3TFZZWK3TEJRUW2ZKSMVZXA33OONSXGPJQFY4TSOLGBUFFGV2UIJZG653TMVZFGZLUORUW4Z3TFZZGK3LPORSUY2LNMVLWS4TFKN2G64TFKVZGYPLIOR2HAXB2F4XXO53XFZWGS3LFO5UXEZJOMNXW2L3DNRUWK3TUF5UW4ZDFPAXHA2DQH5YFYPLCGIGQURTJNR2GK4STMV2HI2LOM5ZS42LOONYGKY3UN5ZES4DTHU3TMLRYFY3DOLRSG4GQUZLWNFWF62DPON2HGPINBJGWK43TMFTWKU3FOR2GS3THOMXHI4TBMNVWS3THI5KUSRB5BUFEISCUFZCW4YLCNRSUC3DUJRXWGULVMVZGSZLTKYZD25DSOVSQ2CS4EAVVYICVJEXGCZTUMVZFGZLBOJRWQQ3MNFRWWTDJNZVUY33DMFWD22DUORYFYORPF53XO5ZONRUW2ZLXNFZGKLTDN5WS62LOMNWGSZLOOQXT643UMFTWKXB5MFTHIZLSEZZGK43POVZGGZK4HVWG6Y3BNQGQUXBAFNOCAVKJFZQWM5DFOJJWKYLSMNUFA4TPKNUG65Z5ORZHKZINBJCEQVBOJVUW4QLDORUXMZKBOZSXEYLHMVKXA5DJNVST2MJYGAYDAMBQBUFFI2DJOJSFAYLSOR4VGZLBOJRWQUTFON2WY5DTKNSXI5DJNZTXGLTTMVQXEY3IIRQXIYLCMFZWKPLMN53SA3DPO5ZVY5DUNBSSA3DPO4QGY33XONOHI4DBOJVWK4RAMFXGIIDMNFWHSXDUOBQXE23FOIQG433PNZOHI3DJNR4XO33MMZSVY5DKMVZGK3LZEB3WQZLBORWGK6K4ORTGS4TFEBXW4IDUNBSSAYTSNFTWQ5BAONVXSXDUNEQGC3JAMEQGO5LOLR2GU5LOMUQGO3DPN5WVY5DVONSXEJ3TEBTXK2LEMVOHI5DIMUQGYYLTOQQGO33PMQQG42LHNB2FY5DTOVUXIIDPMYQGM2LSMVOHI2LOOZUXG2LCNRSSAY3JORUWK424ORRWC3TEPETXGIDMMFZXIIDEMF4VY5DTNVQXG2DJNZTSA4DBOJ2HSXDUNEQGI33OE52CA3DJOZSSA2DFOJSSAYLOPFWW64TFLR2GIZLBOIQGM3DJMVZSYIDMN53GKIDTOBUWIZLSLR2HO2DJORSSA3DJNZSXEXDUOZSWY5TFOROHI43UFYQG4ZLJNROHIIDXN5WHMZLTEBSWC5BAMRXWO424ORWGC3TFEBTGS4TFLR2G43ZAON2WG2BAORUGS3THEBQXGIDTMFZGCIDKMFXGKXDUMFYXKYLOMF2XIXDUORUGKIDSOVZXG2LBNYQGK3TENFXGOXDUNBSWY3DPEBUGC3DPLR2GQZLSMUQGG33NMVZSA53JNZ2GK4T4NZQW2ZK4HVBWQZLDNMQG65LUEBKGQZJAJRXXOICMN53XGJZAJRUW2ZKTOBXXIICQMFTWKXBBLR2HK4TMLQ6WQ5DUOBODULZPORUGK3DPO5WG653TFZWGS3LFONYG65BOMNXW2L24NZZWQ2LONFXGOIDWNFXWYZLOMNSXY3TBNVSVYPKDNBSWG2ZAN52XIICUNBSSATDPO4QEY33XOMTSATDJNVSVG4DPOQQFAYLHMVOCCXDUOVZGYXB5NB2HI4C4HIXS65DIMVWG653MN53XGLTMNFWWK43QN52C4Y3PNUXVY3TBONXWE2JAONSWW43VLR2GC43PMJUVY5DTMVVXG5L4NZQW2ZK4HVBWY2LDNMQGQZLSMUQHI3ZAM5SXIICBONXWE2JAKNSWW43VE5ZSAJ2DNF2HE5LTE4QGS3RAGI2TM23COBZSATKQGNOHI6DNNRPWC3DCOVWVYPKDNF2HE5LTLR2HQ3LML5QXE5DJON2FYPKBONXWE2JAKNSWW43VLR2GM2LMMVKHS4DFLQ6W24BTLR2HQ3LMKNRWQZLNMFOD2YLVMRUW6XDUOVZGYXB5NB2HI4C4HIXS63DJNVSXO2LSMUXG64THF5WWE3DPM4XWC43PMJUXGZLLON2VY3TQMFZHI33OLR2GI33MNR4SA4DBOJ2G63S4ORSG63DMPFOHIZDPNRWGSZK4ORSG63DMNFSSA4DBOJ2G63S4ORRGCY3LO5XW6ZDTEBRGC4TCNFSVY5DCMFZGE2LFPRXGC3LFLQ6UOZLUEBCG63DMPEQFAYLSORXW4J3TEBHGK5ZAIFWGE5LNLR2HQ3LML5QXE5DJON2FYPKEN5WGY6JAKBQXE5DPNZOHIZTJNRSXI6LQMVOD23LQGNOHI6DNNRJWG2DFNVQVYPLBOVSGS324OR4G23C7MFWGE5LNLQ6UEYLDNN3W633EOMQEEYLSMJUWKXDUOVZGYXB5NB2HI4C4HIXS63DJNVSXO2LSMUXG64THF5WWE3DPM4XWI33MNR4Q2CSEJBKC4RLOMFRGYZKBNR2EY33DKF2WK4TJMVZT2ZTBNRZWKDIKIREFILSFNZQWE3DFKB2XG2CQOJXXQ6KROVSXE2LFONLDEPLUOJ2WKDIKIZUWY5DFOJJWK5DUNFXGO4ZONVQXQQLMORZVI32ENFZXA3DBPE6TEDIKKNSWC4TDNBJWK5DUNFXGO4ZOIRUXGYLCNRSU6T2CKYZD2MBOHE4TSZQNBJKUSLTJNZ2HE32MN5RWC3CDNRUWG22MNFXGWPINBJGW62TJORXS4U3UN5ZGCYTMMVIHKYTMNFZWQZLSKBSXE2LPMQ6TCOBWGAYDAMANBJ2XAZDBORSUIZLMMF4T2MRVGIYDAMBQGEGQUU2XKRBHE33XONSXEU3FOR2GS3THOMXHG53UIJZG653TMVZFI2LUNRSVO2LUNBAW24DTKBZG6PKOMV3SAQBAEZGGS3LFBUFE233KNF2G6LSJONBG633UON2HEYLQOBSWIUTBORUW6PJQFYZQ2CSTK5KEE4TPO5ZWK4STMV2HI2LOM5ZS443XORBHE33XONSXEVDPN5WHI2LQHVGGKYLSNYQE233SMUXC4LQNBJOCAK24EBKUSLTBMZ2GK4STMVQXEY3IINWGSY3LJRUW42Z5NB2HI4C4HIXS653XO4XGY2LNMV3WS4TFFZRW63JPNFXGG3DJMVXHILZ7ON2GCZ3FLQ6WCZTUMVZCM4TFONXXK4TDMVOD23TFOR3W64TLBUFFKSJOMFTHIZLSKNSWC4TDNBBWY2LDNNGGS3TLJRXWGYLMHVUHI5DQLQ5C6L3XO53S43DJNVSXO2LSMUXGG33NF5UW4Y3MNFSW45BPH5ZXIYLHMVOD2YLGORSXEJTSMVZW65LSMNSVYPLMN5RWC3ANBJRW63TOMVRXIYTBMNVWM2LSMV3WC3DMHV2HE5LFBUFFEZLTOVWHIUDBNZSWYLSTMVQXEY3IIJQW43TFOI6VYID3GB6SAVTJONUXIICMNFWWKV3JOJSS4Y3PNUQGM33SEBUW24DPOJ2GC3TUEB2XAZDBORSXGLR3NB2HI4C4HIXS64TFON2WY5DTFZWGS3LFO5UXEZJOMNXW2L3TMVRXK4TJOR4TWMJOGBTA2CTDN5XHIZLOOQXG2YLOMFTWK3LFNZ2ECY3UNF3GKPLUOJ2WKDIKKVES45LTMVHGK5DXN5ZGWSLNMFTWK4Z5ORZHKZINBJKWY5DSMFYGKZLSKNSXI5DJNZTXGLSNNFXEG33ONZSWG5CUNFWWKPJUBUFEM2LMORSXEU3FOR2GS3THOMXGG4TBO5WGK4SJOBZT2NZWFY4C4NRXFYZDOOYNBJTGS3DUMVZF62DBONUD25DSOVSQ2CSVJEXGS3TUOJXVK4TMHVUHI5DQLQ5C6L3DNRUWK3TUOBUXQLTMNFWWK53JOJSS4Y3PNUXXA2LYF5UW45DSN4GQUZTMOVZWQZDFNRQXSPJQBUFE233KNF2G6LSNMF4EG33OORQWG5DTKBSXETTFOR3W64TLINWGC43TKJQXI2LPHUYC4MINBJCEQVBOJVUW4UDBONZWS5TFJRSWCZSJNZUXI2LBNRKXA5DJNVST2MZQGAYDAMANBJCEQVBOIRUXGYLCNRSU4ZLUO5XXE2Z5MZQWY43FBUFEISCUFZCW4YLCNRSVAYLTONUXMZKMMVQWMTLPMRST25DSOVSQ2CSGNFWHIZLSKNSXI5DJNZTXGLTIN5ZXI2LMMVEXA4Z5GEZDQLRRGA4C4KROFI5TEMBYFYYTAOJOFIXCUOZWGQXDKOJOGY2C4MBPGE4DWMRQHAXDSOBOGAXDALZRHA5TEMJWFYYTGOJOGIYDQLRQF4ZDAOZSGE3C4MJTHEXDEMRUFYYC6MJZHM3DOLRRGU4S4MBOGAXTCOB3GIYDILRRGEXDEMJWFYYC6MRRHMZDILRUGQXDEMZXFYVDWNZSFY3DGLRUG4XCUOZXGEXDQNZOG4YS4KR3GIZDALRSGM4S4NBTFYVDWMRUFY3DGLRRHA3C4KR3GY3C4MRUGAXDCOJSFYYC6MJYHM3TQLRWGMXDCMZXFYVDWNRZFY2DELRWGQXDALZRHE5TEMBYFY3TCLRRGE2C4KR3HA2S4MJXFYYC4MBPGE3DWOBVFYYTOLRRGU2S4KR3GIYDQLRVGMXDCMRYFYYC6MJYHMZDAOJOGE3TELRWGAXCUOZWGQXDQOJOGE3C4KR3GIYTMLRRGUYS4NRTFYVDWMRRGYXDEMJYFYYTEOBOGAXTCNZ3GIYDILRYFYZTELRQF4ZDEOZYGIXDOOBOGI2DKLRKHMYTEMJOGE2TMLRWGUXCUOZSGQXDGMJOGIYDKLRKHM3DQLRRHA4C4OJRFYVDWOBWFYYTENZOGIZDCLRKHMZDANBOGEZS4NJVFYVDWNRZFY3DILRWGQXDALZRHE5TQNJOHA4C4MROFI5TQNJOHA4C4OJOFI5TMNBOHEZS4OBZFYVDWNRUFY4TGLRYHAXCUOZWGYXDCMJOGEYTELRQF4ZDAOZXGQXDCMZOGE4DCLRKHM3DILRXGIXDCMRQFYYC6MRRHM3DILRXGIXDCMJSFYYC6MRRHM3DQLRVHEXDQNZOFI5TEMBYFYYTCNROGAXDALZRHA5TCMRRFYYTCOJOGE4DELRKHM3DILRZGIXDEMRUFYYC6MRQHMZDAOBOGEYDCLRQFYYC6MJYHM4DELRRGQ4C4MJRGEXCUOZYGIXDEMBYFY2DALRQF4ZDEOZWGMXDENBTFYYTMMROFI5TQNJOHEZC4MJVHAXCUOZWGMXDENBTFYYTQMJOFI5TMNBOHEZS4OJQFYVDWMRRGIXDENJOGEYDGLRKHM3DILRSGEYC4NRTFYVDWMJTGAXDCMJXFYVC4KR3GIYTMLRXFYYTQMZOFI5TKOBOGE3DQLRSHEXCUOZWGQXDQOJOGI3S4KR3GY4S4NZSFYYTMOBOFI5TMOBOGIZTCLRWGMXCUOZSGA3S4MJYHEXDEMZSFYVDWNRXFY4DCLRRGQ2S4KR3G42C4NRQFY3TSLRKHM3DQLRUGIXDCNZWFYVDWNZYFYYTEOJOGEZTOLRKHM3DSLRSGQ4C4MRQHEXCUOZSGQXDCNJOGIYDKLRKHM3TCLRRHE4S4MRVGAXCUOZWHAXDSOJOGEXCUOZXGEXDCMRQFY2TILRKHMZDILRWGMXDENROFI5TOMROGIZC4MBOGAXTCOJ3GIYTMLRRGY3C4MRRGYXCUOZSGE3C4MJWGYXDCNJZFYVDWNRUFYZDCMBOGE2DILRKHM4DGLRRGMZS4MJSGYXCUOZYGMXDCMZTFYYTENZOFI5TEMBYFYYTEMROGAXDALZRHA5TEMBYFYYTEMROGE4TELRQF4YTSOZWHEXDSLRRGYYC4MBPGE4TWMRQG4XDCNJQFYYTONROGAXTEMB3GYZS4MRUGYXDCMRYFYYC6MJZHM3DCLRRGI4S4NJRFYVDWOBOGMXDEMJYFYVDWNRUFYZTILRKFYVDWMRQGYXDCNRZFYZDENJOFI5TMNBOGIYTALRRHEZC4KR3GIZDELRXGMXDCOJSFYYC6MJYHMZDANROGE3DSLRRG4YC4KR3GE3DOLRSGE3C4MRTGIXCUOZYGEXDCNJSFYYTMLRKHM3DMLRRGI4C4MRSG4XCUOZSGE3C4MJUGQXDMNBOFI5TMNBOHA4S4NBRFYVDWMRQHEXDCNBTFYZDENBOFI5TEMBZFYZDAMZOHE4S4KR3GIYDMLRRGMZC4MZSFYYC6MRQHMZDCNROGEYDILRSGI2C4KR3GIYDSLRRGYZS4MRRHAXCUOZSGA3C4MJWHEXDEMZQFYVDWMRRGYXDCNZXFY3DILRKHM3DILRSGAXDGMROGAXTCOJ3GY4S4NBUFYYTKOBOFI5TEMBZFY3DOLRQFYVDWMRQGQXDSLRRGE4C4KR3GY3C4MRTGIXDSNROGAXTCOJ3GE3DQLRSGE2S4MJSHEXCUOZWHEXDINBOGE2TMLRKHMZDANBOHEXDCMJXFYVDWNRZFY2DILRRGU3S4KR3GY2C4MZXFYYTSMROFI5TMOBOGE2DSLRWGUXCUOZWHEXDINBOGE2TKLRKHMZDAOBOGUYC4MBOFI5TMNROGI2TALRUGYXCUOZSGE3C4MJWHEXDSNROGAXTCOJ3G4YC4OBUFYYC4MBPGE2DWMRRGYXDEMJZFY4TMLRKHM3DILRRGI2C4MBOGAXTCNJ3G42S4NZUFY4DGLRKHM3DOLRRHEYS4MZRFYVDWNZQFYZDINZOGE2DQLRKHMZDILRRFYYTONJOFI5TENBOGEZDQLRRG43C4KR3G42S4MJRGEXDSNZOFI5TMOBOHAYC4MJYGYXCUOZXGEXDEMZYFYYTKMROFI5TMOJOG4ZC4MJWGEXCUOZWG4XDCNRTFYZDIMZOFI5TMOBOGM3S4NBSFYVDWNRXFYYTMNZOGE3TSLRKHM3DQLRTFYYTMOBOFI5TMNROGMYC4OBQFYVDWOBSFY3TSLRWGMXCUOZSGA3C4MRSGIXDALRQF4YTSOZWHEXDKOJOGE3C4MBPGIYDWNRUFYZDAOBOGAXDALZRGY5TEMBZFYYTSMBOGAXDALZRG45TEMBZFYYTMMBOGMZC4KR3GIYDSLRRGYYC4MZVFYVDWMRRGIXDCNZZFYYTQLRKHM4DKLRRG43C4NRTFYVDWOBVFYYTONROGEZDQLRQF4YTQOZWG4XDCNJOGY3C4KR3GY3S4NJVFY3DILRQF4YTQOZSGA4C4NRTFY4TALRKHM3DILRUGAXDSNROGAXTCOJ3GY3C4MJZGMXDEMRUFYVDWMRRGYXDCMZQFYYTONROGAXTEMB3GY2C4NRSFYYTEOBOGAXTCNZ3GY3C4NRTFYYTMMBOGAXTCOJ3GIYTMLRSGU2S4MJXGYXDALZSGA5TMOJOGUYC4MJWGAXDALZRHE5TOMROGEYS4NBYFYYC6MRQHM3TMLRXGYXDALRQF4ZDAOZWHEXDIMJOGE3DALRQF4YTSOZWGQXDEMBZFYYC4MBPGE3TWNRVFY2TMLRQFYYC6MJUHM3TELRTGYXDCMRYFYYC6MJXHM2TQLRRGMXDSMROGAXTEMR3GE4DSLRRHAXDCMJWFYYC6MRSHMYTENJOGIYDKLRRGAYC4MBPGIZDWOBVFY4TELRRGU3C4MBPGIZDWNRWFYZTOLRUHAXDALZSGA5TMNROGIYTELRRGI4C4MBPGE4TWMRQHEXDCOJVFYYTEOBOGAXTCOB3GIYDMLRRHEZS4MRSGQXDALZSGA5TEMJWFYZDKLRUGQXCUOZUFY2DGLRRGE4S4KR3GY2S4MRQGYXDKNJOFI5TMNJOGIYDMLRVGEXCUOZXGIXDCNJZFYYTINJOFI5TQMZOGE2DELRSGI2C4MBPGIYTWNRWFYYTCNJOGEZDQLRKHM3DQLRRGE2C4MJZHAXCUOZXGIXDKMJOGMZC4MBPGIYDWMRQGYXDKMJOGIZTALRKHMZDCMROGE3TSLRRGMZS4KR3GY3C4MJWGAXDCNJYFYVDWNZYFYYTEOJOGEZTMLRKHM3DSLRYGUXDCOJSFYYC6MJYHMZDANROGQ4C4MBOFI5TEMJWFYZDGMBOGE2TALRKHM4DQLRYGYXDCMBZFYVDWNRVFY4TQLRQFYYC6MJXHMYTENJOGYYC4MJZGIXDALZRHA5TEMBYFYYTCNJOGIZTILRKHMZDCMZOGE2TMLRVGIXCUOZUGEXDEMRRFYYTMLRQF4ZDAOZSGA4S4NJZFYYTCMBOFI5TOMBOGQ4S4MRTHEXCUOZYG4XDEMZWFYYTSMROGAXTEMJ3HAZC4NRZFYYTCOJOFI5TOMROGEYS4MJUGMXCUOZYGQXDIMBOGMYC4KR3G4ZC4OJOGEYDQLRKHM3TELRZFYYTAOJOFI5TMNBOGYYS4MRVFYYC6MRVHM3DILRWGEXDENJOGE4TELZSGY5TMNBOGYYS4MRVFY3TELZSHA5TMNBOGYYS4MRVFYYTONRPGI4DWNRUFY3DCLRSGUXDCMRYF4ZDSOZWGQXDMMJOGI2S4MJWGAXTEOJ3GY4C4MJXHAXDCMRYFYYC6MJXHMZDCNROGIZTCLRRGYZC4KR3GE2DILRSGYXDCMRZFYVDWMRQGUXDCNZXFYYTQNROFI5TEMBVFYYTONZOGE4DMLRKHM3TKLRRGEYS4MJSHAXCUOZSGQXDMMROGU4C4KR3GI2C4MJQGYXDCNBQFYVDWMRRHEXDCNZOHAZC4KR3GIYDCLRYGYXDENJUFYVDWNRUFYYTCMJOGE4TELRQF4YTSOZSGAYC4MJUGAXDMMZOFI5TEMBRFY4C4MJRG4XCUOZWGYXDCMJYFYZDIMROFI5TOMROGUYS4MJZGIXCUOZWGEXDCMRZFYZDKMJOFI5TEMBZFYZDANJOGI2DMLRKHM3TKLRRGI3C4KROFI5TEMBVFYYTGOJOGIYDQLRQF4ZDEOZWGYXDEOBOGIYDOLRKHM3DMLRSHAXDEMBTFYVDWNZVFYYTMNBOGY3S4KR3GYYS4MJSHEXDOMBOFI5TCMZQFY2DSLRRGE3C4MRVGE5TEMJWFY3DSLRRGY2C4MBPGIZDWNRWFYYTSOJOGE3TMLRQF4ZDGOZWGYXDCOJZFYZDENBOGAXTCOJ3HAZS4MJUHAXDQLRKHM4DQLRRGAZC4NROFI5TMNROGEYDSLRRGYXDALZSGA5TQLRRGMZS4NRZFYVDWNJYFYYTSNBOGIZTOLRKHMYTELRRHE3S4MRVGMXCUOZSGA4C4MJQGEXDQMROFI5TCNRRFYZTCLRSGQYC4KR3GI2C4NZWFY4TKLRKHMZDILRYGUXDINJOFI5TENBOHEYC4MJTGQXCUOZSGQXDSNBOG4XCUOZSGQXDCMJXFYZDSLRKHMZDILRRGQYS4MJYGYXCUOZWG4XDCOBTFYYTOOBOFI5TMOBOGUYS4OBWFYVDWNRZFYZDIOBOGE4TQLRKHM3TCLRWHAXDGNBOFI5TOMJOGIYTILRRHA3C4KR3G4YS4MRUGAXDQOBOFI5TOMROGIZTCLRSGM2C4KR3G42S4NZTFYZTALRKHM3TMLRRGY3C4MRRGUXCUOZXGYXDCNZXFYZDMLRKHMYTSNBOGEZDMLRRHEZS4KR3GIYDSLRSGAYC4MBOGAXTCOB3GIYTMLRRGY3S4MJVGMXCUOZSGE3C4OBUFY2DSLRKHMYTGMJOGEYDILRKFYVDWMJUHEXDONROGE3DELRKHM3TILRXHEXDENBSFYVDWMRQGYXDCMBYFYZDKMZOFI5TMNROGE3DKLRSGA2S4KR3GIYDSLRRGE2S4MRVGUXCUOZSGE3C4MRSHEXDCOJTFYVDWMJVHAXDCMBTFYYC4KR3GE3DALRRGAXDOLRKHMZDANROGIZDKLRRGAZS4KR3G42C4MRSGIXDALRQF4ZDAOZSGEZS4NJSFYZDENZOFI5TEMJRFYYS4MJZGMXCUOZSGA3C4NZRFY2TMLRKHMYTENJOGAXDCNJRFYVDWMJSGEXDCMJXFY3TOLRKHM4DSLRSGQ2C4MRTGAXCUOZSGA3C4MRSGMXDCNJWFYVDWNRWFYYTGNJOGMZC4MBPGE4TWOBTFYZDGLRRG4ZS4KR3GY2S4MZZFYYTQNJOFI5TMNROGE4TILRRGU2S4KR3GIYTOLRXGUXDILRKHM4DCLRSGMXDIMBOGAXTEMB3HAYS4MRTFY2DQLRQF4ZDAOZSGA3S4MRUHAXDGMROGAXTEMB3GIYDKLRRGQ2C4MRRHAXCUOZYFYYTOLRQFYVDWMJZGYXDIMBOGEYC4KR3GE2TKLRUG4XDCNBZFYVDWOBTFY3TSLRRHA4C4KR3GEZDCLRRFY2TELRQF4ZDAOZSGAZS4MJRGEXDEMZSFYYC6MRRHM4DKLRRHAXCULRKHMYTSOJOGEZDALRTGEXCUOZSGA4S4MJUGUXDQOBOFI5TEMBXFYYTANROGEZDGLRKHMZDCNROGM3S4MRTG4XCUOZRGUZC4OJOGEYDELRKHMZDAMBOG4XDGMZOFI5TEMJSFY3TMLRTG4XCUOZSGE3C4NBWFYYTGMZOFI5TEMBYFYYTANZOGE3DILRKHMZDCNROGE3S4MJQGAXDALZSGI5TEMJWFYYTOLRRGA2C4MBPGIYTWMRRGYXDEOBOGMYS4KR3GI2C4MJQFYYTMMBOFI5TENBOGU3S4MRTFYVDWMRUFY2TOLRVGQXCUOZSGQXDCMRXFYYTKMJOFI5TMNZOGE4C4MBOGAXTCNJ3GY3C4MJZGUXDCNZRFYVDWMRQGQXDKMROGIYTKLRKHM4DSLRSGQ2C4MRSGEXCUOZYGQXDCOBRFYZDCMROFI5TENBOGIZDOLRSGIZC4KR3G4ZC4MJXGIXDQOBOGAXTEMR3G42C4MJRGAXDSOJOFI5TOMROGQYC4MJQG4XCUOZYHEXDENBUFYZDEMJOFI5TEMJTFYYTMNZOHE3C4KR3GEZTSLRXHAXDCMBOFI5TENBOGIYTGLRRGQ4S4KR3GYZS4NBXFYYTEOBOFI5TMNROGI2DMLRSGQ4C4KR3GE2DCLRRGE3S4MRYFYVDWMJTGEXDCNRSFYYTKMZOFI5TMMJOGYZS4NJRFYVDWNRRFY3DGLRSGUYS4KR3GY2S4MJXGEXDCNJRFYVDWNRYFYZTELRSGMXCUOZXGQXDENJUFY3TCLRKHMZDAMBOGEYTSLRSGE3S4KR3G4ZC4MJWFYZDCNROFI5TEMBXFY3S4MJTGUXCUOZSGE3C4MJVGAXDOOJOFI5TOMROGIZTMLRRHA2C4KR3GIYTMLRSGMZC4MRUGQXCUOZRGQ3S4MJZG4XDCOJQFYVDWOBOGE3S4MJOFI5TMMROGEYDCLRRGI2C4MBPGIZDWOBRFYZDAOBOGY2C4MBPGE4DWOBSFY4DGLRSGA4C4KR3HAYS4NZWFY4TCLRKHM3TSLRRGMXDMOBOFI5TQNJOGEZDILRRG4ZS4KR3HA4C4MJQGQXDMMZOFI5TQNBOGQYC4MRQGUXCUOZSGA4S4MRRG4XDEMRTFYVDWMRQHEXDEMBVFYZDINZOFI5TMNROGIZDKLRRGY4C4KR3GEZTQLRSGQZS4MRUGUXCUOZWGMXDCMBQFY2DILRKHM4DKLRRG4XDCOJOFI5TOMROGI2C4MRVGAXCUOZWHAXDQMJOHAZC4OJTHM3TOLRZG4XDEMJSFYVDWOBTFYZDELRRGU2S4KR3HAZS4MRQFYYTCMROFI5TCMRSFYZDELRTGYXCUOZYHAXDMMZOGIYTSLRKHMZDCMBOGEZTELRSGIYC4KR3HAZS4OBYFYZDAOBOFI5TONBOGIZDALRRG4ZS4KR3G42C4MJZGUXDKMJOFI5TONROGE4S4OBTFYVDWNZWFYZDKLRRGI3C4KR3G43C4OJYFYYTKMBOFI5TMNROGI3S4MJUHAXDCOBSHM4TALRSGQYC4MJYG4XDEMBWHM3TALRXGEXDCNRQFY4TCOZXGAXDMOJOGE4DALRKHMYTELRRHE3S4MRQGQXCUOZRGMYC4MJZGIXDCNJXFYVDWMRQGYXDCOBYFYYTEOJOFI5TEMJTFYYTIMBOGAXDALZRHE5TCOJWFYZDANZOGMZC4KR3GIYDKLRSGEYS4MJUGUXCUOZWGQXDEMBTFYYTSMJOFI5TENBOGEYTILRSGUZC4KR3G4ZC4MJWFYZDCNBOFI5TCNBUFY4DALRSGQ4C4KR3GIYTMLRSGEYS4NZTFYVDWMRUFY3DKLRXG4XCUOZWHEXDSMBOGI2DELRKHM3TELRTHEXDKMJOFI5TONBOGYYC4MJVHAXCUOZXGYXDONBOGEZDQLRQF4YTOOZXGIXDENBSFYZDGOJOFI5TQOJOGI2DMLRQFYYC6MJYHMZDCNROGE3TCLRRG43C4MBPGIYDWNZQFY2DILRYHAXCUOZSGQXDEMRZFYYTOOJOFI5TOMBOGE2S4OBQFYYC6MRRHM3DCLRSGEYS4MJTGEXCUOZRGQ2C4OBQFYYTGMROFI5TENBOGE2DILRRGQYC4KR3G4ZC4MJTFYYTIMZOFI5TOOBOGEYTCLRWGQXDALZSGA5TSMJOGM2C4MJZGIXDALZRHA5TEMJQFYYTSOJOHA3C4KR3GIYTCLRRFYZDCOJOFI5TCMRSFYZC4NJYFYVDWNRWFYYTKNJOGIYTCLRKHMYTKNROGM2C4MRXFYVDWNRUFY2TMLRWGQXDALZSGE5TEMJWFYZDIMBOGEZDQLRQF4YTSOZWGIXDEMJZFY3TSLRKHM3DQLRRHAYC4MBOGAXTEMR3GE2DELRRGYZC4KROFI5TMNROGIYDOLRSGU2C4KR3GIYDOLRRGM2C4OJOFI5TEMBTFY4DOLRRG43C4KR3GY3C4MRQGMXDCOBZFYVDWNRZFY3DMLRSGUZC4KR3GE4DSLRUFYYTQMZOFI5TEMJSFYYTEMBOGEYDSLRKHM3DOLRRG4ZC4MRZFYVDWNZRFY3DALRWGEXCUOZSGAYS4OJVFYYTEOBOFI5TEMBUFYYTCMJOFIXCUOZYGYXDCMBXFYZDEMJOFI5TCNJTFY2S4MRQFYVDWMRUFY4DALRRGE3C4KR3GY4C4OJWFYYTAMJOFI5TONJOGYZS4NZXFYVDWMJZGYXDEMBXFY2DKLRKHMZDCOBOGIZDGLRRHE3S4KR3GY2C4MRTGEXDEMBSFYVDWMRQGEXDSLRRGI4C4MBPGE3TWMRRHEXDCMBXFYYTEOBOFI5TCNBSFYYTMMZOFIXCUOZWGYXDEMJRFYYC4MBPGIYDWOBTFY3TSLRRGUZC4KR3GEZTILRRGI4S4KROFI5TEMBVFYYTINROGAXDALZRGY5TEMBQFY3TOLRRGYYS4KR3GIYTMLRRGU4S4MRQGEXCUOZSGAZS4OBUFYYTOMBOFI5TEMJWFYYTANJOGE4DILRQF4ZDEOZRGYYS4MZRFYZDENROFI5TCOJTFYYTEMBOGEYTMLRKHMZDCMZOGEZDCLRRGUYS4KR3GIYTMLRSGIYS4OJWFYVDWMRUFYZTMLRRGQYS4KR3G42C4MJZGQXDOMJOFI5TQMJOGE2TELRRGA2C4KR3HAZS4NZYFYYTKNJOFI5TQNJOGE3TMLRRGEYC4KR3GE4DSLRRGMXDCNBXFYVDWMRQGEXDKOBOGE3DILRKHMZDAMJOGEZS4NRVFYVDWNJYFYYTQOBOGEZS4KR3GU4C4OBYFY2DELRKHMYTSNJOGI2DELRRGUZS4KR3GIYDKLRRGI4S4MJWGQXCUOZRGQZC4MRQGQXDQNZOFI5TMNJOGEZDALRUGIXCUOZSGAYS4MJVGAXDOOJOFI5TENBOGEZTQLRRGI4C4MBPGE4DWNRVFYYTMLRWGEXCUOZWGUXDCOJOGEZDQLRQF4YTQOZSGE3C4MJTGAXDMNBOGAXTCOJ3G4ZC4MJXGIXDMNBOGAXTCOJ3GM4C4MBOGAXDALZYHM4DOLRRGE3S4MJZGIXDALZRHA5TMNROGE3DALRRGI4C4MBPGE4DWNRWFYYTMMBOGE4TELRQF4ZDAOZXHAXDCMRZFYYTEOBOGAXTCNZ3GIYDGLRYG4XDCOJSFYYC6MRQHMZDAMJOHEXDCMRQFYVDWNZSFYYTMNZOGAXDALZRGY5TEMJTFYZDELRRGYZC4KR3G42C4MRQHAXDALRQF4YTQOZWG4XDKNROGAXDALZRGU5TEMJTFYZDKNBOGIZTELRQF4ZDEOZSGAZS4OBXFYYTQNBOGAXTEMR3GEZDCLRVGQXDMNBOGAXTEMR3GIYDQLRZHEXDCOJSFYYC6MJZHMYTQOJOGUXDCNRQFYVDWNRWFYYTAMZOGMZC4MBPGE4TWNRUFYZDCMBOGEZDQLRQF4YTSOZWGYXDENJUFY4TMLRQF4YTSOZSGA4C4OJZFY3DILRQF4ZDAOZSGE3C4MJVGEXDCMRYFYYC6MJZHMYTEOJOGQ3S4MJSHAXDALZRG45TCMROGIYDELRQFYYC6MJXHMZDCNZOG42S4MJRGIXDALZSGA5TSMBOGE4TGLRYFYVDWNRUFY2S4NRUFYYC6MJYHMYTELRRFY2DELRQF4ZDGOZSGQXDEMZRFY3DILRQF4YTSOZXGEXDMLRRGI4C4MBPGE3TWMRQHEXDSNZOGE4TELRQF4YTSOZWHEXDCMBOGEZDQLRQF4YTSOZWGQXDINROGMZC4MBPGE4TWMRRGMXDINZOGI2DQLRQF4ZDEOZWGEXDEMRUFYZDEOBOFI5TQNJOGAXDQOBOFI5TCMRRFY2TILRQFYYC6MRSHMYTIOJOGI2TILRRHEZC4KR3GYYS4NRTFYZTELRQF4YTSOZXGUXDEMBZFY2TELRQF4ZDEOZYGMXDCMJQFYZDGMROFI5TCOBZFYYTGLRTGYXDALZSGI5TCNJWFYZTILRSGQYC4MBPGIZDWMJSGUXDCLRRGA4C4KR3G42C4MJTHEXDCNRSFYYC6MRTHM3TILRUGMXDEMRRFYVDWNRWFYYTOMJOG42C4MBPGIZTWMJSGQXDENBOGIZTALRQF4ZDGOZSGE3S4MJXGEXDCMRYFYYC6MRTHMZDCMZOGE3DELRWG4XCUOZXGUXDIMROGI4C4MRQHAXTEOJ3HAZC4MJQHEXDEMBXFYVDWMRQGAXDEMJQFYZDGNBOFI5TCOJVFYZDENROGIZDOLRKHM3DMLRSGEZC4MRSGQXDALZRHE5TMMBOGI2TILRSGEZS4KR3GIYDMLRRGMZC4NBYFYYC6MRQHMZDANROGEZTELRQFYYC6MJZHM4DCLRRHEYC4MJTHEXCUOZYGQXDCMBOGM3C4KR3HA4S4MRUGQXDENBQFYVDWNBWFY2DMLRKFYVDWMRQHAXDCMBQFYYC4MBPGE4DWMRRGYXDQNROGE2DILRQF4ZDAOZWG4XDEMBSFY3DILRQF4YTSOZSGE3C4MRQGUXDEMJXFYVDWOBZFYZDINBOGI2DALRKHM3TILRVGIXDALRQF4YTKOZXGQXDKNBOGAXDALZRGY5TOMROGIZC4MJZGIXDALZSGA5TEMJWFY3S4OBQFYYC6MRQHM3DSLRUGIXDCMRYFYYC6MJZHMZDCNROGE2TQLRRGI4C4MBPGE4TWOJRFYYTQOJOGEYDILRQF4ZDCOZXG4XDOOBOHE3C4KR3GY3C4MJVGQXDSNROGAXTCOJ3GEZDCLRTFYZTCLRKHMYTENJOGAXDALRQF4YTKOZWGMXDEMJWFYYC4MBPGEZTWMRQHEXDQLRQFYYC6MJVHMZDANZOGIZDMLRQFYYC6MJWHMZDANROGE3DCLRQFYYC6MJWHMZDANJOGE3TOLRQFYYC6MJWHM3DKLRXGIXDALRQF4YTMOZSGA2S4MRVGIXDALRQF4YTMOZSGA3S4MJXGYXDALRQF4YTOOZXHEXDCOBVFYYTANJOFI5TMNROGI4S4MBOGAXTCNZ3GY3C4MRUGYXDALRQF4YTMOZWGQXDENBXFYYC4MBPGE4DWMRRGYXDCMJYFY3DILRQF4YTQOZWGQXDEMJOGAXDALZRG45TMNBOGIYS4MJSHAXDALZRHA5TEMBZFYYTEMZOGAXDALZRGY5TEMBXFY4TSLRQFYYC6MJXHM3TALRUG4XDALRQF4YTMOZWG4XDCOJWFYYC4MBPGE3DWMJUGIXDCNRWFYYC4MBPGE2TWMJUGIXDCNRUFYVC4KR3GE2DELRRGYZS4KROFI5TOMROGEYS4MJSHAXDALZRHE5TQMROGM3S4OJRFYVDWNZZFYYTQNBOG4XCUOZXGQXDEMJWFYYC4MBPGE3DWMRQHEXDQNZOGI2DQLRKHM3DILRSGUXDCOBQFYVDWNRQFYYTSMBOGIZDALRQF4ZDGOZWGAXDCMROGE4DELRKHMYTENJOGIXDALRQF4YTKOZSGAZC4MRQHAXDGMROGAXTCOJ3GYYC4MJZGAXDEMRSFYYC6MRTHMYTIMROGE3TOLRSGM4S4MBPGIZDWMJUGIXDCNZXFYYTGNROGAXTEMR3GE3DQLRSGE2S4MJUGAXDALZSGM5TSMJOGM2C4MJZGEXCUOZSGAYS4OBTFYYTCOBOFI5TEMRRFYYTCNZOGIZTGLRKHMYTENBOHE3S4MZXFYVDWMRQHAXDALRSGMYS4KR3GE2DELRRG43S4OJQFYVDWMJUGIXDCNZXFYYTKNROFI5TCNBSFYYTONZOGIZTCLRKHM3DSLRYG4XDEMZQFYVDWOBRFYYTCMBOGE3DELRKHM3DILRSGUYS4MBOGAXTCOJ3G4ZC4MJVHAXDINROFI5TMOJOGE2DMLRRGQ2S4KR3HA2C4MJTGAXDCMJYFYVDWNRTFY3TSLRSGQ2S4KR3GY3C4NZRFYZDCOJOFI5TMNZOGE4TGLRUG4XCUOZSGQXDCNZSFY3DQLRKHM3DSLRRGE4S4MJRGEXCUOZRGQZC4MJXG4XDCOBVFYYC6MRRHM4DKLRRGIXDINROFI5TQNZOGEYDMLRWGMXCUOZXGIXDGNJOGIZDILRQF4ZDAOZSGA2C4MJQFY4DQLRQF4ZDCOZZGAXDEMBQFY3DMLRKHM3DGLRRGM4C4MBOF4YTKOZWGQXDENZOGAXDALZRHE5TOMBOHE4S4KROFI5TQMROHAYC4NBOFI5TMOJOGQ3C4MBOGAXTCOJ3GYZC4OJQFYYTOMROGAXTEMR3HA2S4MRRGQXDALRQF4YTKOZSGA4S4MRTG4XDEMRUFYYC6MRSHMZDAOJOGIZTOLRSGMZC4MBPGIZDWMRQHEXDEMZXFYZDIMBOGAXTEMB3GY3C4NZZFYYTMMBOGAXTCOJ3GIYDKLRSGA4S4MJSHAXDALZRHA5TOMROGAXDCMBVFYVDWNRWFY4TALRWGQXDALZRHA5TONBOGYZS4NRUFYYC6MJZHMZDANROG42S4NBTFYVDWMRQHAXDKNBOGI2DALRQF4ZDAOZWGUXDIOJOGAXDALZRHA5TOMROGUZC4NRUFYYC6MJYHMZDCNROGY3C4MBOGAXTCOB3GIYTMLRWGYXDMNBOGAXTCOJ3GIYDSLRVGEXDCNRQFYYC6MJZHM3DMLRSGIYC4MBOGAXTCOJ3GY2C4NZRFYYTEOBOGAXTCOB3GEZDCLRZGEXDSMJOFI5TSMROGQ4C4NRUFYYC6MJYHMZDAOBOHEXDCMJSFYYC6MRRHM4DCLRRGY4S4MJSHAXDALZSGA5TOMBOGEYTQLRZGAXDOOZXGAXDCMJYFY4TELRXGA5TQNROHE3C4MRSGYXCUDIKJVSXG43BM5SVGZLUORUW4Z3TFZHU6QSSMVSHK3TEMFXGG6J5ORZHKZINBJJVOVCCOJXXO43FOJJWK5DUNFXGO4ZOON3XIQTSN53XGZLSKRXW63DUNFYFA4TPHVGGKYLSNYQE233SMUXC4LQNBJOV2PR4F5YHE33QOM7DYL3TNFWXA4B6");
    }

    
    private class RequestHandler implements HttpClientListener {

        public boolean requestComplete(HttpUriRequest request, HttpResponse response) {
            LOG.debug("http request method succeeded");
            
            // remember we made an attempt even if it didn't succeed
            UpdateSettings.LAST_SIMPP_FAILOVER.setValue(clock.now());
            final byte[] inflated;
            try {
                if (response.getStatusLine().getStatusCode() < 200
                        || response.getStatusLine().getStatusCode() >= 300)
                    throw new IOException("bad code " + response.getStatusLine().getStatusCode());
    
                byte [] resp = null;
                if(response.getEntity() != null) {
                    resp = IOUtils.readFully(response.getEntity().getContent());
                }
                if (resp == null || resp.length == 0)
                    throw new IOException("bad body");
    
                // inflate the response and process.
                inflated = IOUtils.inflate(resp);
            } catch (IOException failed) {
                httpRequestControl.requestFinished();
                LOG.warn("couldn't fetch data ",failed);
                return false;
            } finally {
                httpExecutor.get().releaseResources(response);
            }
            
            // Handle the data in the background thread.
            backgroundExecutor.execute(new Runnable() {
                public void run() {
                    httpRequestControl.requestFinished();
                    
                    LOG.trace("Parsing new data...");
                    handleDataInternal(inflated, UpdateType.FROM_HTTP, null);
                }
            });
            
            return false; // no more requests
        }
        
        public boolean requestFailed(HttpUriRequest request, HttpResponse response, IOException exc) {
            LOG.warn("http failover failed",exc);
            httpRequestControl.requestFinished();
            UpdateSettings.LAST_SIMPP_FAILOVER.setValue(clock.now());
            
            httpExecutor.get().releaseResources(response);
            // nothing we can do.
            return false;
        }
    }
    
    /**
     * A simple control to let the flow of HTTP requests happen differently
     * depending on why it was requested.
     */
    private static class HttpRequestControl {
        private static enum RequestReason { MAX };
        
        private final AtomicBoolean requestQueued = new AtomicBoolean(false);
        private final AtomicBoolean requestActive = new AtomicBoolean(false);
        private volatile RequestReason requestReason;
        
        /** Returns true if a request is queued or active. */
        boolean isRequestPending() {
            return requestActive.get() || requestQueued.get();
        }
        
        /** Sets a queued request and returns true if a request is pending or active. */
        boolean requestQueued(RequestReason reason) {
            boolean prior = requestQueued.getAndSet(true);
            if(!prior || reason == RequestReason.MAX) // upgrade reason
                requestReason = reason;
            return prior || requestActive.get();
        }
        
        /** Sets a request to be active. */
        void requestActive() {
            requestActive.set(true);
            requestQueued.set(false);
        }
        
        /** Returns the reason the last request was queueud. */
        RequestReason getRequestReason() {
            return requestReason;
        }
        
        void cancelRequest() {
            requestQueued.set(false);
        }
        
        void requestFinished() {
            requestActive.set(false);
        }
    }
    
}
