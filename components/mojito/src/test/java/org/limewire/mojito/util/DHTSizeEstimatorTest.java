package org.limewire.mojito.util;

import java.math.BigInteger;
import java.net.InetSocketAddress;
import java.net.SocketAddress;

import junit.framework.TestSuite;

import org.limewire.mojito.MojitoTestCase;
import org.limewire.mojito.concurrent.DHTFutureAdapter;
import org.limewire.mojito.result.PingResult;
import org.limewire.mojito2.KUID;
import org.limewire.mojito2.routing.Contact;
import org.limewire.mojito2.routing.ContactFactory;
import org.limewire.mojito2.routing.RouteTable;
import org.limewire.mojito2.routing.RouteTableImpl;
import org.limewire.mojito2.routing.Vendor;
import org.limewire.mojito2.routing.Version;
import org.limewire.mojito2.routing.RouteTable.ContactPinger;
import org.limewire.mojito2.util.DHTSizeEstimator;


public class DHTSizeEstimatorTest extends MojitoTestCase {
	
    /*static {
        System.setProperty("org.apache.commons.logging.Log", "org.apache.commons.logging.impl.NoOpLog");
    }*/
    
    public static final String LOCAL_NODE_ID = "8A82F518E1CD6E7D56F965D65CE5FCAA6261DEA4";
    
    public static final String[] NODE_IDS = {
        "8A82F518E1CD6E7D56F965D65CE5FCAA6261DEAF", "ED1E8511CAA7DDA36395FF402EE05BC62BA0429F", "FB3617BA541D522F6A5E7A2E188CB0A99FBA7DFB", 
        "6DD888A15B61F4B9F9A1117641A3E30F05F43475", "8CD86D31AA23C4060D9C523C62C5661C9087BC7C", "D813FE7931C4E6D6AF38EFA3D8558A798A31A8F1", 
        "F64B5A1EB2CD0F02CA1D743599FF2ADA64996F0F", "E6AA2F4531F9FF2D4CB058F9A5CA3CAB3A589941", "6AB9D487B9B27C08763A3939E4CC672B44039B78", 
        "5D82073E909518144D96EFC35A2661FFC69DEAA8", "0329BC6103E4A3952E6D8729E54DD7DE1A3CA6A4", "B3026D558E4064201E3D85B96E9DA5181F62B41E", 
        "18ECDD3A1E6001529C04CAA2F9FA56B479B16A18", "8D184CA7ED96E4D1AEB8BA7DF7BD3A8936D9D042", "FDB3FD14332A44632FD6462DB565AF40107D05A6", 
        "88C8F7A03FDED08D5CB9CC6B0B33054585494413", "B45CE2F6E3D8585EEBCF101BA721DE4D07770C5A", "4152DFFD31895DC9EC9603E80B8C3715C4388795", 
        "01712154E84214495097837C133FF2AAD6BE2D2F", "0FA789B7274A8ABA05D774FB5A498DC496F92398", "950D5705D3CEB7276B43BE4BB4011DCB916E0A28", 
        "BA51592895D1B0F37C8C5DDA96EE24F4FB9005A9", "1AFFAAFA751CDABF22B16ADA0B96D90A581EF53B", "4F447B9004EC922B233D9E0D637550C583686C16", 
        "612AE9D44C270E4A1CF2BC8F1D6865B20325B522", "239D8496CDF8D3EB46EF59A721BDBCFDD6C848CD", "FCBE8FF1C2690060F7AC4248B2D4359100FB8759", 
        "6A0E55D5F323AD0C6AC8CC331A5C8775A5011A1C", "BFCB6285069364336CA63E0822FF5B9E1FA3D8CB", "334637FBCF974FE0CF83074FD98654C84D425B8C", 
        "83AAF50957B62D62087A499B65F90A224B7DF438", "3438E4A541827B5CAAC2E834EDA5C871056B2FC1", "CDC71E0A754EB2B76DA1F51457F71330AEDA6A3B", 
        "D218E517E152B9CDDB733C35EBAB4BC55FB379DE", "AE4E2A2221B90A09B81E3AA2858D5A3EDA9387B6", "41808DADAC7D7FE83CBCADCBF4B454969A60CADF", 
        "73BA33A0B2AD107712A01860ABF3E9FB0749F6E3", "78AC8A387877D6AC5AC947499BEE85067CD64CC3", "D6C433FC285A32954030D3A5C3E9233B98EDD945", 
        "D3FE8A8A290D62744E0FEA14BF66868954053A7F", "42D39D25CC057BC5B266971A9EF6F5D38A812005", "6C2098192ED6C6E75435264C3EEF74653E1F664C", 
        "DCEF3F9F27009F0CE1CA3DEF337BDCE6AC6C200D", "020A51774C917D151BBAB50B0E59D118DC6FE1DF", "E148D2EB08C48A66B43A6C61520AE3ACAA9D3870", 
        "47CD71E93BA0B0148468E9C1E2D3459E5CBEDA71", "971DC8A03DC51301932157F3ED37E0BFCE9E1021", "D1222EAE390958B6E0A4D13F1E4355FCCC69AFBE", 
        "511A9D36DFB3193230362C389451AD1D980B5C52", "1F25A532DA36BABFC99ADF037A593A1AA7830696", "BBB67BCC1C3ADEED621AAF6CDEA2663AB8AFC8DD", 
        "149805EECF0E70593FDF87C3226800033586EC30", "36A258FFD4036B180766F349B7BC52A268E30A42", "C55A99469D260146198C08236803FE7116E4B027", 
        "6999AFB325B79F3F99D179A7B3E9D4670C142F96", "B6D8516D751D4BD877F73AF46F39673066468830", "E3A3C5BCFF21D3052C57138A9C4D323B8BF7294D", 
        "E11CBA176BA4D0EA10684BB9B5A400BB8687E9AA", "A47D4A4FCF88DE1B205B90BF5F88B031D5518B3A", "63BB06973831EC7BA0A9F1FF4C7D542B28DFEF9C", 
        "903A8B76DC2E67493002ABE765D7B1B0522AA906", "518B6336A39D703DE91DD5DF9F5B24DAC30EFA71", "A5A21B55DA5A2BC80A0C0F78D40ED1584460AC6A", 
        "DE86FF95391CC39C7550F246D5BEA89EC418A605", "0FBB83A17E46999183F8640BB99C7D7C81752EBA", "4F6A268D7CDE38985D0C25592E0B5481BC36DDCA", 
        "FE7DF24AE1B5898CA5A6F85151ADFB3AE02385F0", "4A204686979E7186ACA78A5335742413E6FD520F", "685CFAEFC9D32B5CDBC6DC375D0C9424F8F28F8C", 
        "A2E36C3A8FC1CBCB8C0F419F123FA3B24B964A0D", "EA82B7CCC289C5922578990140FF1CE270657CD8", "03707AA5B3CAAC0BA774309AB0ECDC6827F279E9", 
        "E2BA700C6F7FD2441CDCA8804A2E958C8AFAFB9C", "C3394C31CEC26A265347176A0B338DA96E82C4B6", "297467782F443800FD490721C66EE9E12FB6EC43", 
        "4FF7AA03D417BB321D03FB12F380DBF7366633C6", "37CC4983FAD520661A2D7405F60F156CE1F558E4", "056F54DA06789B9F263DBDDCBB5917B6AAEA2818", 
        "03BF578A9DE6014DB44BAD5FAF6ED9FEFE84A659", "B6BFE7064498177086A7B8B3ACB536CC54946F97", "C55F1C1051E65A8BAB557E78DBE6805E2000D8AE", 
        "EBC2E723AE8A786D4758AB055A2EB5E368D943D6", "46DC5A083F4D3E594D87426476D0F4798EB9B171", "B9BB6A64B0EBE4943A81934826D2CCDECA614412", 
        "2AB36C0272E2D3CA2CDB2AC62434B6A3EA137F8A", "74A4ADEF8F1A67DAF412681B863DB9D6CC48F610", "CF972B35170F896331CCB2227996AB444FC69891", 
        "0C8A86C8C6A9AEEEC01EF73AE4D8DB9D7E0BCD27", "8AD7F6A71C2C524FEBC2AC3F9DB82A3BE58B8D16", "056DD6ED2B8346224A818A62E825AA4ED67A8E13", 
        "FEC2EAC2CF8020F7A7B7EA9F086C3E366426E80B", "5E6080A9359CE08CE91A9063DFD937FA7F4CCFE2", "551FB177BFC2AB9F4447288EA40B23CC30AD2AB9", 
        "908871AE12E2A6917D657673A255073639406AE6", "D44F9A3AC70E7A278240D8D593492AFFBC13009F", "D1AB290C8D8533AA52748AEAEC67895BA8DE8B6E", 
        "EF1A81CE8DF594B7323E0F5B7EE80520096BFA94", "8AD851144E7766E0F34B4D3605F43DF1404A4205", "A599F474D7595B694061F17DD34B6A3FA124B352", 
        "33CA08FF3E71DC5F831DB28E8E5B3409CFD34FE6", "A3DD3AB7DBC57E23EE2A0428EB60474660DAEDCC", "476061AC711B8455370ECBDD712FF4BD693B865F", 
        "4F8C2A3F2C70F4D3BD587EFE646FA213EAD48183", "40C927905D4D6F3258C99AB0B4DAA28484259689", "8BC718F2597B91C6C8503E3FEA5881103C0D07B8", 
        "7AEBE33F377D4A11C7C62E9CF8D242E632D6C1CB", "A64B0BF0983604562905DE51F2185613DB80A2C9", "F1240E8EA61C4AEF001E0DD56098C4CE3BF29E88", 
        "7D7A1BF268AA6790FFE1D70EF847FB190A78E6BA", "6D324C63F1B0C8182BE1F21A2536D20CE6536448", "1340C82290F2BB52D49056EEB61C0137DB15CA79", 
        "8F78373E87346ADF0CCE98B98FC59E6F3796EC36", "CCE08F4FB0AB160F3D220877D9491940DEA9804A", "023CFEF7D41305F467E0A610F4C394205DA25B44", 
        "869C7166CEC3EAF1D0603760ED493DBDA5162F25", "B78FA9B78B272086D4A3BED994D61CD69B82B96E", "CB35A4CEACCA6DCFCABD532D7C371EEF3D91D3B5", 
        "46A137E684F6CD3D247889F68C225B6E468D45F4", "A9A30CECF79E9B27DEE09CCF3AD7658FBF7EB6D8", "204C35852557F982378F096F6CE2FEB98758AE7B", 
        "BC570304C48EDD2702A3688E71CD7914F8B4ACF4", "70961A40A8F098CFFE8D85D235EC92EC31D8304B", "DB1835F9D08A306A8B6CE00A21F48A1F8844F883", 
        "4D92067ACDD52706A0D6B868EF775B7008B2C5FB", "BBC88203770B64C1768771B3E0392A27428E3FD0", "D0199C837B2FB0EF6951024C1CD3D9F3F620E529", 
        "F7AD540C1263AFABEC4C684762B1C6C33E4AFC10", "A888212CE989F7C75043312AD0BE5217B5863C80", "7CA8F2CE2D5C9698D3F0A0CAF7EFCCCE5D7F046B", 
        "35945D310CA3818BEEDDE59E3AC11FAA0D4A69E0", "341CE8FFAEA453828A91CBFF0D20548EBC13B9C3", "D6DFCCFA90CD8B326489075EF437B5A07F81CA17", 
        "C40774501B67FDFA6DA71CCB48422B75A2D06061", "E106DFBCD216AC8AD6AB9FBE9EDB6437F17CB292", "A3685F15474A27B1474064E2A469647F11DEC888", 
        "BDBD8AF3EC0EAC924523E3840C40806E58FADB90", "02B5FFC3133CAB4E7BA71A18E4093333692EF256", "D514FB159FD3A308CFF79272592982B4DF3DE9F4", 
        "8DCCF6F17D29F7B009CAE577619205C6E35772C5", "22FA4130F115030E2105B7C2FD3D280AEB238191", "403868346DB3097BA55F3AF4D47092AE87C65A92", 
        "93237748087E392297F32744813610B7A0BB3A0C", "DE6C446E8C0A55BA09A428BC1FC5039F9BA519C3", "16830DBE0B07B7855C9076B2A465C64E99508D16", 
        "568B17788E9BBA9530FC0A192F627934771BCB08", "3F87284644D61FD48B8D8955545E700A4C709364", "5AB8C3EB523C29E5BE4E98518AFC30A53050FDB9", 
        "1776912D4D1D23D0158C824AEBC70C885D89F80E", "65682535800A97D7945BC686C4C6F13DEF130B0E", "C3212DE7D69E5CA1584B1B7DB8EEC9C330BDA434", 
        "5C57C88709A2D6FC3DB48CFFFD1A8588D728E4F0", "22BBBB90EEE87B80689959E96BEDB576A4D1AE4C", "A0F4098B98990B4C3E0F226F185A2854600D44BE", 
        "C203B3EB82893312AE3C7A74E9ACA4DBBCF849E3", "796E3003FEFEDDD493CB96B2EC9D4E542077FD97", "B7A034211AB83C55029AEE5E5C3253F5C35A13F8", 
        "E6A9D463AC48A8B72AAAD5BBEBAB87DD7DC3A96A", "7BDA7ACD1BFE68DB1BDE9B29BBC856349F6F0387", "4359C2AF990102185E7C63DE547162146173DCB6", 
        "DC59C80837CEAB7F7C161C4BD5974AEECABFFD29", "C8461BBA28FDBB5223E5BE663997E97A74F20704", "A7B2E5FBB3A3754DBE807F5EA155F9042BF307EE", 
        "8B284F1B486CD6FFAC1FFBDD35E08134AA253BF7", "1CB5AF230B61BA1D02CD7781908E791EF12B6F53", "19F0EBD1117762A180142AD78CB8562ED241C110", 
        "B1CA9E7EBA43D8605B851D140FBC738E7C9C8790", "09A5C5CEFD71B45084E1C76C00AF5E2E95628B97", "5068BF0396BA0D4C19E16C671A4469056AE9CA81", 
        "BFE2FAC88ACD414181273B286D83D79FFEFB4D80", "E7BC357DB0CDEA952616EF08293EE1D00889BBF4", "4B7EE44A9C67B38A7B65C1D67D97881FDD243501", 
        "5370474AA1815B7CE1F4243EC87351813771FED2", "5EAE5262208BD41B727823ACB3C615534CD854C0", "F5CC3644162F669B7FDD6209077B815FEC4AA4AA", 
        "ABF4ABBF2A553D1D1D9BAC3DA6324D7A93B0170D", "8FA4A690EFE8BC13A6565F59D3F23286C15E1192", "004F20E13F443DF628769E2799EAB32E87B4DFB1", 
        "7DC94D16E2A0EDDBFF50F58C579E6C1C9CBF2045", "6770B3ABCC8B07994BDB3541906C5BCE4C2AF6DE", "79870CCEFCE5684D7A8756FB014D2D8CEC54B39A", 
        "CECD4F7508084659FD830A7129F2E3FE7130B41B", "661187481AF07CE9FA5CD103C0CF0CC750DF389A", "4A5A857C61EA107C6FA217773BDF3BD3A0A217B7", 
        "961FC6A6397D25B61355AB3396294A9F7EFF672A", "921F44A7F2CA1D436B3E58DDFD6E35AFF3256F54", "C0FAD5122E09BE3C752C6F19FE47A421C3E68E53", 
        "79C76E8D863D7DC443628D7923CDABD69D99F275", "F8A33C08523393FF6179D043269C53921DB712D5", "C5A36378BA08D9F48A56311149471133DA38628C", 
        "2B44B07FB9027ACED14277A6815715E09B3D9F05", "4ABD3BC9BB996619166620F1DBCE5A84881D7AAA", "482C1A5C87EF9EE2F6D4A26D6BE01ADA69111E0A", 
        "92657C002D154119FFFBC2D5C29396D2B4162212", "56F0EE7F181B441FB92FB57FC6C399E82EC073D4", "CE16FDC0EE212A5A5319D613CD375C1D8C7EE3A8", 
        "E82A6833119A0F97BD3E7393BA6DF9C2B19EFE35", "C9A5B8886DD509232C28E7EBBC9835177B774F08", "913B3E9CD6E2746F1537D558325D8D79995DC7D5", 
        "7994ADEA5D679DDA4F4988F61B45593EE652C9CC", "49C66C84A447DCBE6BF3049D579953AFF2EC0538", "CEB238CA2B0F033F42A7E50076BCA97654667639", 
        "C473B4150161D7CBAEA60580D55D11E0C0B075AF", "3C7E1F7576DF4195B810858DF1431C764CCE08E3", "E1C96F4FC8D99FAD9E01B420C09B31FD467D75A6", 
        "1841C8C8BA7F1A45D28AF3BBEF0E8CF07FFF7728", "BCAEE1CE8DF4B4D0C8020FE625C211C938A437BA", "33122A7F19CAA484E877AB49C9CE103BAFCC2AFE", 
        "B941F30919B5FC9031FE3897AC7B12164438880B", "9BECA42575C58434E5983F14D576DE273F9DB583", "FE6F24203F6854450E2C3A5B3B33CA6D5EAB19AB", 
        "0DE3E83532642AFE5EF345D8935262B31DD20F77", "E046FF7ACC63904527C3BB7A38003614E66A3CE3", "B39A3BC0A94AFCCD5D5D0A68919BEB8AFB9A84D2", 
        "5EC247F185A2007EEA71C580B37A678AEFBC1278", "81B5742FD77C366BF2D51C490BCA4930CA786D05", "7D3152CD2C603EA2C1E2222E692E99905A17950F", 
        "A62ADD9262F5B58E5E889ED0F0CA79BCAE0A4A2E", "4151B427E62ECF1EB8FFE31CEE504241AB8B7F48", "C8111A37CC59C7E8A8F38CDD5B90FC23FADCEA4D", 
        "20E18C5F16BC86964BADF67BE3C1E552CACD579C", "2AE6E37978E0D60575F386D9A76FA32BDBE2E294", "CBE98D3BC171C1EC5E5E949283D5AF15EFE7FEF6", 
        "B7F94AD5425124D74DFFB81BB0047BA491169456", "521F654ED798C9EE8941CB2FC06B9FE025F7B5DC", "912A22721378C25A98633304F7F5ABADC90BC2E6", 
        "67DA17154FF3E48D1E6EB51E99A1D474B5775FF7", "DFAAC3B5185284B73CA8194B8723CE74F00DB36F", "EC42DC108492E06677FCF028C6D0B77182C28369", 
        "15ADB9B19B7BF3E8939F5024D5390A85E843F7CF", "50555EE2E3BD0C2778075B2561DFFF67D7892FB2", "2F02849C8F17583AE80C770288136BA045098101", 
        "6B1CF3B0072D617170D6F3E0A5C436B844A96C07", "D1023001792DC010247633EAE9B0E258A339EF8A", "0A7EECC20A17D683D33E06A75F868BA4137FB18E", 
        "C4FBC98A03F5BD44EC51C557C96A584BEC830140", "81568FFA4350B4B3C8A52CFE98C9EC174BB29A0C", "22FD39020064D9DB66404EF658DB38EE3732313F", 
        "848E15DF32375DC4143B17C6A3B7EA8F1102F82D", "6A1578C034A5242FA806C4E439F26938A185ED6D", "D8C43246867B103B5100916B0F0CB0C820D7C3FD", 
        "E173BC92B860898CBB72E24047DB569B6C74B881", "AD8443AD411A49A410CB99017B0A0E10EDFA8508", "DF5EFC66BC3C73C5D0E77E29E6687289A5124024", 
        "BBEB72B33E6E49837FB1192EADD2483D46903A52", "B5817F64BB690AFB39B93F7D22B6CDF9680C6668", "7FA551E71F1DBCBDED3ABEB76731B6BD40D71232", 
        "65D75DC8F82487941627C4B2AF844D00100A2D29", "AAE09863A03D3ED294F82FD742032D206E44FC8B", "9B52A29116D2887328B728797A7101DEEA69699A", 
        "22C31D357AE94C8DC1C5CA183DB521EF72D80DF6", "A3DA3208F7FB073C3B87795022BFBB2FD51B987D", "E40CB8C41C13DC7445C62227D3B0E564CAC35B05", 
        "35F6453FB8B65B70D285D21C382A232DA147CE9B", "D3D5E87A28F61B056F04CE8C440C8EAB7F17E741", "EF045677B8BE635D0F3D5D45FD6253B9E5571568", 
        "CB186C81D0685A6EF94F7F02D40445755ABB2469", "22C21FBA1649483734FCCE1002FFF0B81DD92025", "6DD0FA1E5D7BFD190BF8F74E3DA55E4C941B8E22", 
        "ADED4D7F4DB962819F8A9BABED6A2E71EF1F25FC", "480C1732F58D6A772E0D233FBD89C73344A2394D", "575C4AEB25CB9FE5608E11F08E1407E0F87F9663", 
        "F94746ACD31650627E2337BC0B2318006BF1E063", "1B5834D7E5CE4125ADC6E13E750C68AFC3190F0C", "2160E38117DCB16326F923642D60DD31265FBFDE", 
        "FFFF849A466E307AB0166F6D83A102C631728952", "88565C93E8A9565564EBA6406F35289943877B7D", "68C0B488E8030052C4CC8AE67000C83D8D547776", 
        "9BEDEFDA3E47265322FEBCB85145B4BABA613133", "EF59B2EEC2A7FBA05C5A47CD5A250820E9C9503B", "E087002692F4AB6B437F73B098B6EB459A445B7F", 
        "F1B503A5A3C5BF60D8AC0316490EE99036363259", "FBDD342E0FDEAC03D324770672905EBDBAA81F16", "E299644F1998AA9068C3F512EA0C344BA2A172BD", 
        "539BB7BF5D52883E925AD3B2D0CB8EF3318CD189", "F4C54F68914DD83537EDA8BAF4DB0DCEBEBAE1A3", "13ECDA076A17163BE460C352190D1AC4E91A6675", 
        "AEE7ED92F1D41D202203C2E0FF2B2F27D5D855E1", "249E237B57114A283A36793417C22A757FAA339E", "BDEDA457F71CDE9237B4D911A2BE3B846E96D0F2", 
        "8606E8D6E1E051BFCCB51AAFACEF4636A4D5F798", "FED05091EA74AD2142E3085BFB3B34471DC163B0", "C7440719B391E7F0209ED4823717428D68D06937", 
        "030942885F9D3F0432CDCE92454232150F3A0FBD", "16718E82C0AA8FE4F3F672D725A3A9DD14325533", "D78CDEB76C6E41534CBC5B3D584C58B78FE1E273", 
        "A8D1BD474F6C6BFD6D0D21307EB87DDA538FCCFB", "4D19577B1A5DF3E57D7368C6F328C3EF5E1B8BDB", "578FBE6573600B85610C274766C050F88A3CCFB0", 
        "F7C49E32FC4F7782E3F520993BFA610E987E0B11", "433EF9E1FABDDA9D28F12BFA85B1D0D6F1C74E67", "F367FB3B69DE570FF2FF6A12F9F6FB3300D53C5D", 
        "8FFECF7B49E5DDBB69359F641481BA07694A8F7A", "A5318C9DA941A0DEB2467F031E2B12C487005F2D", "63119B057458AB09F1E33C29BCBE29FB9BDB7115", 
        "0FECE1050083A4D521681E2D5BACBD00EB1F6E39", "7B1D5D08955AF139BCC20D8F136C0C4953F7AF40", "17A8F2FE28E1174D1EE0D8A69BDA2EC8513C55A7", 
        "52E9165A9157D7F293BF1838827F09137BD7C4BD", "3D5A66BBB69FC1ACC36E49310C3D651484C5C9B0", "AB18120DD616A26A082F37C919BE105B4D619B45", 
        "A4C280FBD2EA81687AF5BCBCC08293B514298661", "25C188A3AD830712F2786191D444911573C59BD6", "9D3A1F06A1CE8DC21847022B93EBB0CC4B280550", 
        "688BF07347CB36EC079C8FDE8615CF00E33969D1", "3FDDC88D32084B12B5A35139ED5ABD8F79A695A4", "5D617B50E76CC346FC13B4D9BC89CEA266ED9C74", 
        "4FD8FE0AAAED625453F1CA90A65E36D877DA8B8D", "4133884486626DA02F7F8F61DF5EFB98FCBC5FF1", "5073D3B4B1E450D6D06CE0BBAF9775845695A14E", 
        "CDCEF78E6DACE781AF619927094C217DD874BCAB", "40A5CA247C25C54EA7DF83EE1E3CBF13F672CD7D", "DE6B6F655AFF6D3029C9D9078165CCB0E7BD1DD6", 
        "097FBF008622D90F3CC1DE45A4C967E7F3AD04F7", "53E7D84011F8916259F662CC5B64C4757753CE8A", "49F224F23D6B01BCE987B0AF02FF541E2356686A", 
        "52ED0CE2752687564B1E1AB35473A23E834575D9", "5B69A5B8771E478BDF203DD09A75E821D55779BD", "E65172E2D6D21AF7A60D50DD75B8CDAC4D0227C2", 
        "6DF0E42DF03464F8DFEE3FFA6CED4E077DCD4AD1", "0707ABCC84D9DFAF71A6D36EEE1D244E74793534", "E40BDAF5819DABF4AC4FFCEA3936346F40BB96A8", 
        "47E4AFB67C47133DFA6084CB7B6A2BA4CF1F95F7", "752A8DEDF4B9045C751647CF7CFB662FCF31CFEC", "D4B73AD91836BF6B4A5379170B62CF859901D66F", 
        "7852EFCDDF5082C2FF35D10EE9743396DF650ED1", "4764699E421F81D81E0D94E89B6006A9DD036575", "C6784B0307B9E38F2A80229E84FB7AAB9A809ED3", 
        "A80E2DD142A1175A32816494ED83AF3E9A7D1E26", "27D06EAB7B28E9B39D7C05823FEA70E31BCADC33", "BB6BB1EF9205CF8FF4AD73E5AB7BC29033A96A7B", 
        "F65D714CBB8DC6C4BD8B9C7DB67720C9176FA378", "3032B120A7878CE5449F1C6F0FBC2F5BBE79C3DE", "01AD538A75EAB9D62F3B627F4FCF0DFC4C74CC90", 
        "6ABFC5982DE211065CC21EFF4A73A663DAA3A9F2", "5E3F1A31C22B0C2016659CFC7150F6198AABCC55", "E0183CD31BB5FD24877A687518A1BFE831C17C57", 
        "F2DF59C22FE8AED608D82D5C43051278F5F99A60", "588A0054232D2EB4A144B79215823DC141676573", "E76AE5D862B47A8AC1DB87B6C380CE66BB4F9713", 
        "6F41FD0647E30D51DB51E347A079FF06828D0D94", "556D9995D80432A63FE89D00838C90E0E55F74CC", "3C2B69BF168623C5DE322EE290821658FDA1C62C", 
        "01696DDDAFA04F3B9CC57B1D81CDFDC459BBA6BA", "8C267E3570306B577A4AB7F5733996FD7156C42A", "AB8FFA4D88460B2C5FB9793F5E4E89E4FB28AF0B", 
        "BC4F8A42556E2FE039E06BB8C1E11CB8F4BB6250", "48AC85A15D698BAA8A7B75CF58E2DA11186395FC", "442610F4988EFBC508A6A54E3898A8D556332EC2", 
        "9FBCD8A6CA39C264284E8439DEAF28BECA0D4E60", "391BF50F2F22A18474BC31D0FD0738D36DC69533", "DC610971A75AEDD9A6638E58B67ED181527E3487", 
        "7C5BBC64B6F89690A1D6910451D6C73CF5D17ED2", "5354C5FB3F04CD68E8DABCACEC77E4D081B02FCD", "C892958B0E6796D94071186CBF85E3A75542FA2C", 
        "0AEB652696B59D68CC03E72B9D59482E4FDC58D9", "106AA22FA35F68616774E0655CAFDFF6E923FCB6", "47813112214FD47E3ADD2089B0FAF750D3389F9C", 
        "19998C930ADA8A21AE1B0568FBA2F845EEFB9A7D", "30BC5F3488EF75E5B3C3C100F46EEB5AFE170894", "71423EF86EF237C709BB3CBFC9430423DDA9D7B6", 
        "813A7B3B8DEB821327082DB19E9B929ABB9D78EB", "46B354583EB517398621440B505A1A044CE43E85", "FC35B483258F264610046D18A53BC5F54C8EF0D5", 
        "558AD6EC267B2B6AB07EEB0FA570F45E7449B950", "145C2C9C467409AD47AB7789105408C0C0D7C2D3", "6064105974B515A241D726E708673EC42C680D7B", 
        "56EB5BF062A0C6F294F24454F5853AC7935B6218", "069ABA556913DFF3B7FA74AEAFAAFF801AABFB11", "1CFA421556B06FED986BF677FDC0B621A521CA95", 
        "23E02DEADF26161CFD44689A46C2F7C132EFE30E", "0BDB0BDFEA0EA620C0E6A42B53F00EBD1C769012", "4D622E0C5EF330736F9635005EB7DFB0C5C67908", 
        "0C0D4CBF5D4629B3F1BAB730FD0234A848066398", "577EDC4197474D5F2021DD8C0CFE7BA4DE3F6159", "A1D97E931B9EF2AD0AF4DB954D4430000C5AD7E0", 
        "83B6B1C7559008A4DA2842701267BDAD108F21B9", "A0D60C574402EA6BE1E0DF0F3BC33B1BF72E2B03", "9355385AE0A847EA17EE39D224D6FB06B92A6B3B", 
        "B7F2708A355D037779A660B846EB520C5F1FF0FC", "79098B75D4ED4CDFA3CF93886C99AE3248EDC430", "A5FF6CA4050CA2E16E0DDC342A914E4136E2E69B", 
        "339A6AA57415049883C63E2727B796D9A74C3BD3", "B0A3944AB2F52B803E05903D9C6980B58D7B0909", "387381DCE00D005432A41FAFF32F1573CC149162", 
        "5098FD237B7126970E56FC7D81FE6ED3F48CB866", "773B198C4B43746CF2807B95AF6FA59DA3960098", "C539296F5F4697AAE54049FFFCA29133B2B375FF", 
        "E4B3E7A6C89F772FCB8623F9D8A2E97D8D90B8BE", "C87C8EB63FF9A1F041FC21E3CA433279230AB682", "9FC197ABB05429D705DF1C55C2994F1F68F56E44", 
        "86446BE1C963FA3C44E951F12530F401C853403C", "9C6596753754671C657CCB237F76D9310161C008", "2A99A35253E37D705251178DC39C541E01871C31", 
        "DB65F84E8B55498FAEE5376DE6382418B75E50D4", "B8976C704CD347F7AE56B3BBBE8B7419B238E99E", "2670A41DA1C246AFF55BD38C6FF4204DF66D70F1", 
        "42A1F7931D4205F30FB8F0B51A76A77F5C393077", "2A06F9C78E588CD725DD0122650A8ADECC75C1A4", "72AB8EC0D29F4FC4ED9DFB0828AC323FD1177BCE", 
        "1F92B2E13B7F449F6CFB7935CF07160E197751A9", "AF7AC42F74DFFA177A64D8CE522B06CBAA28FE54", "6A72900664ECB80E3EE036BAFAB8446B3E1D5DDB", 
        "A466D1B39846B3681BE3D1BAFA202DB8441B277B", "012F6CEFCFEB6909B720614BAF27A8F18698453A", "45FADCC1484753B485B27E03F0B0B9C2F6BC4761", 
        "8E6E824876B39B432CEE28985F14008DE6B8025F", "D7CB7E90BBB32C7C948288FA57C01B43F612BF54", "6AC92B8D025802FA05A0BFD242435476447938D5", 
        "45A5F53CBEA613E169E9F718312AB01E70B909FA", "0A50A222F4838D2F1290A638CE6F7583D0DCC478", "7D71A5A14E9F42596EE0E200173FF7B32FAE4D7E", 
        "867DC89141735E9109ED3A5562A8C8AFE0C7B149", "89EB2A7AC14B7EC40F428673EA63D2BEEA7931B6", "095106B1318AEBC29AABF3AC2C050F89B9900D5A", 
        "CF68509ED241FA575C625396F018EF6FC5380509", "AFDB045131A4213866038E38E47C9B1B68EA92D6", "8FB94E705D84A8678E607C8376E71690032A70B9", 
        "983968D516A34136CB476509098C3A3BA4C82D5E", "740113BE3166D75BA3328B93C523A2F2088DD265", "DA50FD33C8CCEE0B7B2B5BF0918CEE075ABE5A76", 
        "71D9A19EF96928C73F92AD51AB8D7FD269C922BF", "8F0E874914600751293970219F1677E8CBE7C811", "D1AF356EB66083301A49BF5BF18972AB0266B519", 
        "961589FCEAD235CB7CC2DE2257927B66AB04D10D", "715B7714C0CDE7F8C850B4BFD40590186C520483", "A04C532E814206154D164DE88C2E73FECB3C6C3A", 
        "167BED259ACC671B2F6FD092677B6428F2317606", "AE195F9BBDC1BDFBE47FFDCEFACA385807707B0E", "96F28639211E5EE1FB0C2B694A6809FA68824A99", 
        "73C18E609416C33BF72B834A39EAC50CC05DEA4C", "37DE3B512AC64DA841BAF0546D2730C56D8C1807", "95294CE7C3D16F7EA1C0CB468DAA306AD364EC01", 
        "DB501E74FB1F997226E0F288537F71408B6E1883", "99C6899AA6982C7DB9797CE67E3284B55CDF6529", "C13ED568D24B84BF51B1FD74377B24921E08C137", 
        "D2B0E404691BE991CB6D0E3F9B1E516C6E48A6EF", "42304A2F291932AD4D23DBE680B8785BAE55E9AA", "3CE7B1B0978685033492EA23841CBC1EB6407148", 
        "A3198CEFCD3897705099A26CE73397C73A90724C", "9A2AD891B46B4451C456A3D96627B9C738CC2E31", "5966C8CEE7FE20C93E4540F59FC11CF88F4EC63E", 
        "1867B6CCEC74DDDC1016B2E6FBB0B86A44C2D896", "3E868C4BE68712CEF8F8155A18AE4F8966D92F9F", "5B5276D303369799A7B1587515BC3406C2C6E87C", 
        "7C903ECA12BCD5EBCAD6F32E2B9A2E2E101C9212", "121809C55D0B458900BE2C07479EF0F3C56CF11F", "A1983097B2F716B74FAAB55BFBF9A6AB4D6A684D", 
        "4D3D7F5C03B9F7C423E577F76B26DDFA9EC3F316", "D448F0C73263BF46E277DFDA29E1BBC927C19DC8", "920B9955B1210385569165798C2D9DE3D12656F8", 
        "6BEE719E667200CCC76FA1522644A046581BA085", "8DF4E6E532368996AEED1DAA40C09B576C556B9D", "3952AEF12A6859AA00A467D308EA1FF713C0B432", 
        "3DAEE780199CC6107338EBF3274CABD468985E0D", "0204BEF0D841A1906C97CCEE642E2A4FBC8B6AF9", "098A2C9788784029CB16F9FFD9AAD130682A65D4", 
        "C09E74804AE42A24621CB0C3CA8E847B1C20D1A1", "2D5729DE5836A50308C4C5BB283D00FD9E48B246", "39F122ADFF431B6FA34890429A463982602E1E91", 
        "5D3A9A0991E6BB43251C5A1DB77B4261BAC30267", "E9CF9253CEFE9C22ADC94004DD9ADDF22D293181", "520F09FA283C8ACD18CBCE960D831261A6EDD7B4", 
        "18D9A28E94DF7E168811DF6AAB3FFDD1DF0E38BF", "64804ECBC8E26C5D6962A43A2C3999D2E94600D6", "79EE7BB60C76EDCD26F13D3F68F6F56209702D00", 
        "9B49C48A22F4CE5EFC86824B149205D53DAA3F86", "F1BFE29EF3A226FF73C96DB5A389B17B584319E3", "63836B3B15D062EF046366B94CFB05892DFD000B", 
        "B945B8A1F148A5965DEE2D3B3E53C3457236FE12", "BB2278F785CD50C47467E1DBA57C609B9F3E9245", "BB6FD528FB64219F71F304A17684E188E8CF486A", 
        "51AF9D7677557A6E2518126EBBBE043867908039", "79DE7E786DFB37769EC9D8E5DF7E1E4DE3C69CE1", "644D9529E1B90C450991AF15B4D30B62DF25DC19", 
        "D811970876886F87922794B330D7EFCC4E13AC4F", "AF2C1FC439541D492B5D2B90C600B5EC87870C11", "66F2B7DF57995FE493AFC3BE7CDF61B30548F3A1", 
        "6BB9D5EABC6B6F0B99FD3E4194934ABC5332648C", "1E85AB4465031753E155DA569272ED8F3F5A107C", "A54D2EA0B828FEF4D12342F198C5A524C3D94A2F", 
        "8E291AF1FFC50DECE585CA29D14EC71B2059A693", "499AD6EA9469DC044DD6978CA89B7BD707C27D11", "151102AE1AC20E73A0F5766EB3EF6B9A25813D91", 
        "8EE78CCCFD89E7B942A5277C7CB4CDCBE0A1827C", "6B610636E39F9466E16721BFADB01F400E984349", "3B2CFD7D420438C2114CDD67FB2E12E2F0D3CA89", 
        "6A0D382821D0474B6133F44A5B6E3C5597BA20F4", "B687B332960C5FF01026A2EA8C6B0D110F303B5C", "A6A73B1F8B0DFD4F8251C46EBCE90748FEDCCDAE", 
        "195AB7BC21F9EB60B3FA22BCD48B8B9B33FC5D12", "A0BA5747A06AAAC8A074D5A7CA5FE436F6C454E8", "1004E647CDA06F2E413B70F45176B63A4B209F9B", 
        "CAA762F76BA6027BD29A4A81BDC2B29D9B6DAF23", "7AFD673D4C9A95A48BAA804479D093BB04C5B5EC", "99C8754E987118269E632CCB65455A567D16DC09", 
        "AFA887F577B1BBE33E6686B02D0A2139FF0E1A5C", "3D89E78CA51AF79937A86E95BFF76CF2A558CC8A", "6A8ACBBF2DA54401E3067E2930BCC9D86FD5BCA8", 
        "D6A0E5F04BF7D041A410CAC145B84A35E0688FB7", "62F64D1ABC0A688911738380881B3CFC851BD711"
    };
    
    public DHTSizeEstimatorTest(String name) {
        super(name);
    }

    public static TestSuite suite() {
        return buildTestSuite(DHTSizeEstimatorTest.class);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }
    
    public void testEstimateSize() {
        setLocalIsPrivate(false);
        
    	RouteTable routeTable = new RouteTableImpl(LOCAL_NODE_ID);
        routeTable.setContactPinger(new ContactPinger() {
            public void ping(Contact node,
                    DHTFutureAdapter<PingResult> listener) {
            }
        });
        
    	for (String id : NODE_IDS) {
            KUID nodeId = KUID.createWithHexString(id);
            SocketAddress addr = new InetSocketAddress("localhost", 5000);
            routeTable.add(ContactFactory.createLiveContact(
                    addr, Vendor.UNKNOWN, Version.ZERO, nodeId, addr, 0, Contact.DEFAULT_FLAG));
    	}
    	
    	assertEquals(494, routeTable.size());
    	
    	DHTSizeEstimator estimator = new DHTSizeEstimator();
    	
    	int[] remote = {525, 601, 310, 750, 455, 654, 512, 210, 497, 101 };
    	for (int size : remote) {
            estimator.addEstimatedRemoteSize(BigInteger.valueOf(size));
    	}
    	
    	assertEquals(BigInteger.valueOf(486), estimator.getEstimatedSize(routeTable));
    }
}
