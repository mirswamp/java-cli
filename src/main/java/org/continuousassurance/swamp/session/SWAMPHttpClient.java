package org.continuousassurance.swamp.session;

import edu.uiuc.ncsa.security.core.exceptions.GeneralException;
import edu.uiuc.ncsa.security.core.util.MyLoggingFacade;
import edu.uiuc.ncsa.security.core.util.Pool;
import edu.uiuc.ncsa.security.util.ssl.MyTrustManager;
import edu.uiuc.ncsa.security.util.ssl.SSLConfiguration;
import edu.uiuc.ncsa.security.util.ssl.VerifyingHTTPClientFactory;
import edu.uiuc.ncsa.security.util.ssl.VerifyingHTTPClientFactory.X509TrustManagerFacade;
import net.sf.json.JSON;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.http.*;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CookieStore;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.*;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.cookie.Cookie;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.util.EntityUtils;
import org.continuousassurance.swamp.exceptions.NoJSONReturnedException;
import org.continuousassurance.swamp.exceptions.SWAMPException;
import org.continuousassurance.swamp.session.util.Proxy;

import java.io.*;
import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.security.GeneralSecurityException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

/**
 * <p>Created by Jeff Gaynor<br>
 * on 10/2/14 at  1:38 PM
 */
public class SWAMPHttpClient implements Serializable {

    public static final int HTTP_STATUS_OK = HttpStatus.SC_OK;
    public static final String ENCODING = "UTF-8";

    transient private HttpClientContext context;
    private SSLConfiguration sslConfiguration;
    private String host;
    private Proxy proxy = null;
    
    private String refererHeader;
    private String originHeader;
    private String hostHeader;

    public class MySSLConfiguration extends SSLConfiguration implements Serializable {
        public MySSLConfiguration() {
        }
    }

    protected SSLConfiguration getSSLConfiguration() {
        return sslConfiguration;
    }
    
    public class MyPool<T extends HttpClient> extends Pool<T> implements Serializable {
        public MyPool() {
        }

        public MyPool(int maximumSize) {
            super(maximumSize);
        }

        transient VerifyingHTTPClientFactory f;

        public VerifyingHTTPClientFactory getF() {
            if (f == null) {
                f = new VerifyingHTTPClientFactory(new MyLoggingFacade(getClass().getSimpleName()), getSSLConfiguration());
                f.setStrictHostnames(false);
            }
            return f;
        }
        

        protected SSLContext getSSLContext() {
            SSLContextBuilder ssl_context_builder  = SSLContextBuilder.create();
            ssl_context_builder.setSecureRandom(new java.security.SecureRandom());
            
            if (getSSLConfiguration().getKeystore() != null) {
                try {
                    ssl_context_builder.loadTrustMaterial(new File (getSSLConfiguration().getKeystore()), 
                            getSSLConfiguration().getKeystorePasswordChars());
                } catch (NoSuchAlgorithmException | KeyStoreException | CertificateException | IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                    return null;
                }
            }
            
            try {
                return ssl_context_builder.build();
            } catch (KeyManagementException | NoSuchAlgorithmException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                return null;
            }
        }
        
        //@Override
        public T create() {

            SSLConnectionSocketFactory sslsf = new SSLConnectionSocketFactory(
                    getSSLContext(),
                    new String[] { getSSLConfiguration().getTlsVersion() },
                    null,
                    SSLConnectionSocketFactory.getDefaultHostnameVerifier());

            HttpClientBuilder http_client_builder = HttpClientBuilder.create().setSSLSocketFactory(sslsf);
            
            if (proxy.isConfigured()) {

                http_client_builder.setProxy(new HttpHost(proxy.getHost(), proxy.getPort(), proxy.getScheme()));

                if (proxy.getUsername() != null && proxy.getPassword() != null) {
                    CredentialsProvider credsProvider = new BasicCredentialsProvider();
                    credsProvider.setCredentials(
                            new AuthScope(proxy.getHost(), proxy.getPort()),
                            new UsernamePasswordCredentials(proxy.getUsername(), proxy.getPassword()));

                    http_client_builder = http_client_builder.setDefaultCredentialsProvider(credsProvider);
                }
            }
            return (T)http_client_builder.build();

        }
        
        /*
        //@Override
        public T createOld() {
            try {
                return (T) getF().getClient(host); // have to have for SSL resolution.
            } catch (IOException e) {
                throw new GeneralException("Error getting https-aware client");
            } 
        }*/

        @Override
        public void destroy(HttpClient httpClient) {
        }
    }

    protected Pool<HttpClient> clientPool = new MyPool<>();


    /**
     * Basic default service client that uses the java truststore only.
     *
     * @deprecated
     */
    public SWAMPHttpClient(String host) {
        this.host = host;
        MySSLConfiguration sslConfiguration1 = new MySSLConfiguration();
        sslConfiguration1.setUseDefaultJavaTrustStore(true);
        if (System.getProperty("keystore-path") != null) {
            sslConfiguration1.setKeystore(System.getProperty("keystore-path"));
            sslConfiguration1.setKeystorePassword(System.getProperty("keystore-password"));
            sslConfiguration1.setKeyManagerFactory("SunX509");
            sslConfiguration1.setKeystoreType("JKS");
        }
        this.sslConfiguration = sslConfiguration1;
    }


    public SWAMPHttpClient(String host, SSLConfiguration sslConfiguration, Proxy proxy) {
        this.host = host;
        this.sslConfiguration = sslConfiguration;
        if(proxy == null){
        		proxy = new Proxy();
        }else {
            this.proxy = proxy;
        }
    }

    public static String encode(String x) throws UnsupportedEncodingException {
        return URLEncoder.encode(x, ENCODING);
    }

    public static String decode(String x) throws UnsupportedEncodingException {
        return URLDecoder.decode(x, ENCODING);
    }

    protected List<NameValuePair> convertMap(Map<String, Object> map) {
        List<NameValuePair> nvps = new ArrayList<NameValuePair>();

        if (map != null && !map.isEmpty()) {
            BasicNameValuePair b = null;
            for (String key : map.keySet()) {
                Object value = map.get(key);
                if (value != null) {
                    b = new BasicNameValuePair(key, value.toString());
                }
                // say(this, b.toString());
                nvps.add(b);
            }
        }
        return nvps;
    }

    public String getRefererHeader() {
        return refererHeader;
    }

    public void setRefererHeader(String refererHeader) {
        this.refererHeader = refererHeader;
    }

    public String getOriginHeader() {
        return originHeader;
    }

    public void setOriginHeader(String originHeader) {
        this.originHeader = originHeader;
    }

    public String getHostHeader() {
        return hostHeader;
    }

    public void setHostHeader(String hostHeader) {
        this.hostHeader = hostHeader;
    }

    /**
     * Earlier versions of the SWAMP had very several requirements on the headers and were very finicky. After about version 1.26
     * these were mostly relaxed. Just in case though, this method is still here.
     *
     * @param request
     */
    protected void setHeaders(HttpUriRequest request) {
        //      request.setHeader("Referer", getRefererHeader());
        //      request.setHeader("Origin", getOriginHeader());
        //       request.setHeader("Host", getHostHeader());
        request.setHeader("Accept", "application/json, text/javascript, */*; q=0.01");
        //request.setHeader("Accept", "*/*");
        //     request.setHeader("Accept-Language", "en-US, en;q=0.5");
        //     request.setHeader("Accept-Encoding", "gzip, deflate");
        //request.setHeader("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
        //     request.setHeader("User-Agent", "Mozilla/5.0 (X11; Ubuntu; Linux x86_64; rv:45.0) Gecko/20100101 Firefox/45.0 Jeff-test/edu.uiuc.ncsa.swamp.test");
    }

    /**
     * Get with no parameters
     *
     * @param url
     * @return
     */
    public MyResponse rawGet(String url) {
        return rawGet(url, null);
    }

    public MyResponse rawGet(String url, boolean isStreamable) {
        return rawGet(url, null, isStreamable);
    }

    /**
     * Returns the response as a string.
     *
     * @param url
     * @param map
     * @return
     */
    public MyResponse rawGet(String url, Map<String, Object> map) {
        return rawGet(url, map, false);
    }
    public class Stuff{
        HttpHost target;
        HttpRequestBase request;
        List<NameValuePair> nvp;

        public Stuff(String url, Map<String, Object> map, int action) {
            doStuff(url, map, action);
        }

        public static final int DO_GET = 1;
        public static final int DO_POST = 10;
        public static final int DO_PUT = 100;
        public static final int DO_DELETE = 1000;
        public void doStuff(String url, Map<String, Object> map, int action){
            if (map != null) {
                nvp = convertMap(map);
            }
            URI parsedURI = URI.create(url);
            switch(action){
                case DO_GET:
                    request =  new HttpGet(parsedURI.getPath());
                    break;
                case DO_PUT:
                    request = new HttpPut(parsedURI.getPath());
                    break;
                case DO_POST:
                    request = new HttpPost(parsedURI.getPath());
                    break;
                case DO_DELETE:
                    request = new HttpDelete(parsedURI.getPath());
                    break;
                default:
                    throw new GeneralException("Error: unknown/unsupported HTTP method");

            }
            target = new HttpHost(parsedURI.getHost(), parsedURI.getPort(), parsedURI.getScheme());
            if (proxy.isConfigured()) {
                HttpHost proxy1 = new HttpHost(proxy.getHost(), proxy.getPort(), proxy.getScheme());
                RequestConfig config = RequestConfig.custom()
                        .setProxy(proxy1)
                        .build();
                setHeaders(request);

                request.setConfig(config);
            } else {
                setHeaders(request);
            }

        }

    }

    /**
     * Returns the response as a stream in the {@link MyResponse} object;
     *
     * @param url
     * @param map
     * @param isStreamable
     * @return
     */
    public MyResponse rawGet(String url, Map<String, Object> map, boolean isStreamable) {
        Stuff stuff = new Stuff(url, map, Stuff.DO_GET);
        HttpClient client = clientPool.pop();

        try {
            HttpResponse response = client.execute(stuff.target, stuff.request, getContext());
            HttpEntity entity = response.getEntity();
            JSON json = null;
            List<Cookie> cookies = getContext().getCookieStore().getCookies();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            // We can consume the contents once. To get a string out of it,we have to construct one from the byte array.
            entity.writeTo(baos);
            String raw = new String(baos.toByteArray());

            MyResponse myResponse = null;
            try {
                json = toJSON(raw);
            } catch (Throwable t) {
                // not an issue if it is not json
            }
            myResponse = new MyResponse(json, cookies);
            myResponse.setHttpResponseCode(response.getStatusLine().getStatusCode());

            if (isStreamable) {
                myResponse.setOutputStream(baos);
            }
            releaseConnection(client, response);
            return myResponse;

        } catch (IOException e) {
            e.printStackTrace();     // TODO: don't print the stack trace
            throw new GeneralException("Error invoking http client", e);
        } finally {
            clientPool.destroy(client);
        }

    }

    protected JSON toJSON(String raw) {
        if (raw == null || raw.length() == 0) {
            return null;
        }
        JSON json;
        if (raw.charAt(0) == '[') {
            json = JSONArray.fromObject(raw);
        } else if (raw.charAt(0) == '{') {
            json = JSONObject.fromObject(raw);
        } else {
            throw new NoJSONReturnedException(raw);
        }
        return json;
    }

    /**
     * The assumption is that this downloading a file from a server to the given targetFile. If
     * no target file is specified, a temporary one will be created. The file returned is either that
     * or the targetFile.
     *
     * @param url
     * @param targetDir
     * @return
     */
    public File getFile(String url, File targetDir, String targetName) {
        Stuff stuff = new Stuff(url, null, Stuff.DO_GET);
        stuff.request.setHeader("Accept", "application/json, text/javascript, /; q=0.01");
        HttpResponse response = null;

        try {
            if (targetDir == null) {
                targetDir = File.createTempFile("swamp-temp", "");
                if (!targetDir.mkdirs()) {
                    if (!targetDir.exists())
                        throw new GeneralException("Unable to create a temporary directory for download");
                }
            }
            HttpClient client = clientPool.pop();

            response = client.execute(stuff.target, stuff.request, getContext());
            HttpEntity entity1 = response.getEntity();

            if (entity1 == null) {
                releaseConnection(client, response);
                return targetDir;
            }

            InputStream is = entity1.getContent();
            File targetFile = new File(targetDir, targetName);
            FileOutputStream fos = new FileOutputStream(targetFile);
            int inByte;
            while ((inByte = is.read()) != -1) fos.write(inByte);
            is.close();
            fos.close();

            releaseConnection(client, response);
            return targetFile;
        } catch (IOException e) {
            e.printStackTrace();     // TODO: don't print the stack trace
            throw new GeneralException("Error invoking http client", e);
        }

    }

    public HttpClientContext getContext() {
        if (context == null) {
            CookieStore cookieStore = new BasicCookieStore();
            context = HttpClientContext.create();
            context.setCookieStore(cookieStore);

        }
        return context;
    }

    protected MyResponse makeRequest(boolean doPost,
                                     String url,
                                     Map<String, Object> map) {
        return makeRequest(doPost, url, map, null);
    }

    /**
     * Make a request. If this is a post with a payload, a file list will result in a multipart POST.
     *
     * @param doPost
     * @param url
     * @param map
     * @param files
     * @return
     * @throws UnsupportedEncodingException
     */
    protected MyResponse makeRequest(boolean doPost,            // TODO: can this method be simplified?
                                     String url,
                                     Map<String, Object> map,
                                     List<File> files) {
        HttpClient client = clientPool.pop();
        Stuff stuff = null;
        if (doPost) {
            stuff = new Stuff(url, map, Stuff.DO_POST);
            HttpPost post = (HttpPost) stuff.request;
            try {
                if (map instanceof JSONObject) {

                    //DebugUtil.say(this, ".makeRequest: got to json");

                    JSONObject json = (JSONObject) map;

                    //DebugUtil.say(this, ".makeRequest: json=" + json);

                    StringEntity entity = new StringEntity(json.toString(), HTTP.UTF_8);
                    entity.setContentType("application/json");
                    post.setEntity(entity);
                } else {
                    UrlEncodedFormEntity urlEncodedFormEntity = new UrlEncodedFormEntity(convertMap(map));
                    urlEncodedFormEntity.setContentType("application/x-www-form-urlencoded; charset=UTF-8");

                    post.setEntity(urlEncodedFormEntity);
                    post.addHeader("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
                }

            } catch (UnsupportedEncodingException e) {
                throw new SWAMPException("Unsupported encoding", e);
            }
            if (files != null && !files.isEmpty()) {
                post = getHttpPost(post, map, files, client);
            }
            //req = post;
        } else {
            //HttpPut put = new HttpPut(url);
            stuff = new Stuff(url, map, Stuff.DO_PUT);

            HttpPut put = (HttpPut)stuff.request;
            setHeaders(put);
            try {
                UrlEncodedFormEntity urlEncodedFormEntity = new UrlEncodedFormEntity(convertMap(map));
                urlEncodedFormEntity.setContentType("application/x-www-form-urlencoded; charset=UTF-8");
                put.setEntity(urlEncodedFormEntity);
                /* application/x-www-form-urlencoded; charset=UTF-8 */
            } catch (UnsupportedEncodingException usx) {
                throw new SWAMPException("Unsupported encoding", usx);
            }
            put.setHeader("Accept", "application/json, text/javascript, /; q=0.01");
        }
        HttpResponse response = null;
        try {
            try {
                //response = client.execute(req, getContext());
                response = client.execute(stuff.target, stuff.request, getContext());
                if (response == null) {
                    releaseConnection(client, response);
                    throw new GeneralException("Error: null response from server. Do you have an internet connection?");
                }
            } catch (javax.net.ssl.SSLHandshakeException xx) {
                System.out.println("Error connecting to " + url);
                throw xx;
            } catch (Throwable t) {     // TODO: do not catch Throwable
                // t.printStackTrace();    // TODO: do not print strack trace
                throw new GeneralException("Error contacting server", t);
            }
            // TODO: what if the repsonse is null
            if (response.getStatusLine().getStatusCode() != HTTP_STATUS_OK) {
                releaseConnection(client, response);
                throw new HTTPException(response.getStatusLine().getReasonPhrase() + " code=" + response.getStatusLine().getStatusCode() +
                        ". error connecting to " + url,
                        response.getStatusLine().getStatusCode());
            }

            HttpEntity entity1 = response.getEntity();
            String x0 = EntityUtils.toString(entity1);
            releaseConnection(client, response);
            return new MyResponse(toJSON(x0), getContext().getCookieStore().getCookies());
        } catch (IOException e) {
            releaseConnection(client, response);
            // e.printStackTrace();      //TODO: do not print stack trace
            throw new GeneralException("Error invoking http client", e);
        }
    }

    private HttpPost getHttpPost(HttpPost post, Map<String, Object> map, List<File> files, HttpClient client) {
        setHeaders(post);
        MultipartEntityBuilder meb = MultipartEntityBuilder.create();
        meb.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);
        MultipartEntityBuilder reqEntity = MultipartEntityBuilder.create();
        for (File f : files) {
            ContentType contentType = ContentType.DEFAULT_BINARY;
            reqEntity.addBinaryBody("file", f, contentType, f.getName()); // it is required that this be named "file".
        }
        for (String key : map.keySet()) {
            reqEntity.addTextBody(key, map.get(key).toString());
        }
        HttpEntity x = reqEntity.build();
        post.setHeader("Content-Type", x.getContentType().getValue());
        post.setEntity(x);
        return post;
    }


    public MyResponse rawPut(String url, Map<String, Object> map) {
        return makeRequest(false, url, map);
    }

    public MyResponse rawPost(String url, Map<String, Object> map) {
        return makeRequest(true, url, map);
    }

    public MyResponse rawPost(String url, Map<String, Object> map, List<File> files) {
        return makeRequest(true, url, map, files);
    }

    public MyResponse delete(String url) {
        Stuff stuff = new Stuff(url, null, Stuff.DO_DELETE);
        stuff.request.setHeader("Accept", "application/json, text/javascript, */*; q=0.01");
        HttpClient client = clientPool.pop();
        HttpResponse response = null;

        try {
            try {
                response = client.execute(stuff.target, stuff.request, getContext());
            } catch (Throwable t) {
                t.printStackTrace();
            }
            if (response.getStatusLine().getStatusCode() != HTTP_STATUS_OK) {
                releaseConnection(client, response);
                //      say("warning: got a status of " + response.getStatusLine() + " for address " + url);
                throw new HTTPException(response.getStatusLine().getReasonPhrase() + " code=" + response.getStatusLine().getStatusCode(),
                        response.getStatusLine().getStatusCode());
            }


            HttpEntity entity1 = response.getEntity();
            JSONObject json = null;
            String x0 = EntityUtils.toString(entity1);
            try {
                json = JSONObject.fromObject(x0);
            } catch (Throwable x) {
                //   say(this, "Exception:" + x.getClass().getSimpleName() + "message=" + x0);
            }
            releaseConnection(client, response);

            return new MyResponse(json, getContext().getCookieStore().getCookies());

        } catch (IOException e) {
            e.printStackTrace();
            throw new GeneralException("Error invoking http client", e);
        }

    }


    protected void releaseConnection(HttpClient client, HttpResponse response) {
        // This is necessary to close the underlying stream in a controlled way.
        // If the stream has already been consumed, this method discards the resulting
        // IOException. Failure to do this will result in an exception:

        // java.lang.IllegalStateException: Invalid use of BasicClientConnManager: connection still allocated.
        // Make sure to release the connection before allocating another one.
        if (response != null) {
            EntityUtils.consumeQuietly(response.getEntity());
        }
        clientPool.push(client);
    }
}
