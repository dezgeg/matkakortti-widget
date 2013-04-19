package fi.iki.dezgeg.matkakorttiwidget;

import static com.gistlabs.mechanize.document.html.query.HtmlQueryBuilder.*;

import java.io.*;
import java.net.*;
import java.security.*;
import java.security.cert.*;
import java.util.*;
import java.util.regex.*;

import javax.net.ssl.*;

import org.apache.http.*;
import org.apache.http.conn.*;
import org.apache.http.conn.scheme.*;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.impl.client.*;
import org.apache.http.impl.conn.tsccm.*;
import org.apache.http.params.*;
import org.apache.http.protocol.*;
import org.json.*;

import android.util.*;

import com.gistlabs.mechanize.*;
import com.gistlabs.mechanize.document.*;
import com.gistlabs.mechanize.document.html.*;
import com.gistlabs.mechanize.document.html.form.*;

public class MatkakorttiApi
{
    private static class NonverifyingSSLSocketFactory extends SSLSocketFactory
    {
        SSLContext sslContext = SSLContext.getInstance("TLS");

        public NonverifyingSSLSocketFactory(KeyStore truststore) throws Exception
        {
            super(truststore);

            TrustManager tm = new X509TrustManager() {
                public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException
                {
                }

                public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException
                {
                }

                public X509Certificate[] getAcceptedIssuers()
                {
                    return null;
                }

            };
            sslContext.init(null, new TrustManager[] { tm }, null);
        }

        @Override
        public Socket createSocket(Socket socket, String host, int port, boolean autoClose) throws IOException,
                UnknownHostException
        {
            return sslContext.getSocketFactory().createSocket(socket, host, port, autoClose);
        }

        @Override
        public Socket createSocket() throws IOException
        {
            return sslContext.getSocketFactory().createSocket();
        }
    }

    private static AbstractHttpClient createNonverifyingHttpClient() throws Exception
    {

        KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
        trustStore.load(null, null);

        SSLSocketFactory sf = new NonverifyingSSLSocketFactory(trustStore);
        sf.setHostnameVerifier(SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);

        HttpParams params = new BasicHttpParams();
        HttpProtocolParams.setVersion(params, HttpVersion.HTTP_1_1);
        HttpProtocolParams.setContentCharset(params, HTTP.UTF_8);

        SchemeRegistry registry = new SchemeRegistry();
        registry.register(new Scheme("http", PlainSocketFactory.getSocketFactory(), 80));
        registry.register(new Scheme("https", sf, 443));

        ClientConnectionManager ccm = new ThreadSafeClientConnManager(params, registry);

        return new DefaultHttpClient(ccm, params);
    }

    public static double getMoney(String username, String password) throws Exception
    {
        AbstractHttpClient httpClient = createNonverifyingHttpClient();

        MechanizeAgent agent = new MechanizeAgent(httpClient);
        Document page = agent.get("https://omamatkakortti.hsl.fi/Login.aspx");

        Form loginForm = page.forms().get(byId("aspnetForm"));
        String prefix = "Etuile$MainContent$LoginControl$LoginForm$";
        loginForm.get(prefix + "UserName").set(username);
        loginForm.get(prefix + "Password").set(password);

        loginForm.submit((SubmitButton) loginForm.get(prefix + "LoginButton"));
        // TODO: Follow redirects
        HtmlDocument response = agent.get("https://omamatkakortti.hsl.fi/Basic/Cards.aspx");

        List<HtmlElement> scripts = response.htmlElements().getAll(byTag("script"));
        Pattern jsonPattern = Pattern.compile(".*?parseJSON\\('(.*)'\\).*", Pattern.DOTALL);

        for (HtmlElement script : scripts) {
            Matcher matcher = jsonPattern.matcher(script.getInnerHtml());
            if (matcher.matches()) {
                JSONArray cards = new JSONArray(matcher.group(1));
                if (cards.length() == 0)
                    return -1;
                return cards.getJSONObject(0).getDouble("RemainingMoney");
            }
        }
        return -2;
    }
}
