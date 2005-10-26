import java.io.IOException;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpConnection;
import org.apache.commons.httpclient.HttpConnectionManager;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.SimpleHttpConnectionManager;
import org.apache.commons.httpclient.methods.PostMethod;

public class HttpConnectTest {

	public static void main(String args[]){
		new HttpConnectTest();
	}

	public HttpConnectTest() {
		try {
			HttpClient client = new HttpClient();
			
			PostMethod post = new PostMethod("http://www.archive.org:80/create.php");
			post.addRequestHeader("Content-type","application/x-www-form-urlencoded");
			post.addRequestHeader("Accept","text/plain");
			
			NameValuePair[] nameVal = new NameValuePair[] {
					new NameValuePair("xml","1"),
					new NameValuePair("user","markk03@gmail.com"),
					new NameValuePair("identifier","markk03test4")};
			
			post.addParameters(nameVal);
			client.executeMethod(post);
			System.out.println(post.getResponseBodyAsString());
		} catch (HttpException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}		
	}
}
