package org.lastbamboo.common.amazon.s3;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.security.Security;

import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.StatusLine;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.InputStreamRequestEntity;
import org.apache.commons.httpclient.methods.PutMethod;
import org.apache.commons.httpclient.methods.RequestEntity;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.lastbamboo.common.amazon.stack.AmazonWsUtils;
import org.lastbamboo.common.util.FileInputStreamHandler;
import org.lastbamboo.common.util.HttpUtils;
import org.lastbamboo.common.util.InputStreamHandler;
import org.lastbamboo.common.util.NoOpInputStreamHandler;


/**
 * Class implementing REST calls to Amazon's S3 service.
 */
public class AmazonS3Impl implements AmazonS3
    {

    private static final Log LOG = LogFactory.getLog(AmazonS3Impl.class);
    
    private final String m_accessKeyId;
    private final String m_secretAccessKey;

    /**
     * Creates a new S3 instance.
     * 
     * @param accessKeyId Your Amazon S3 access key ID.
     * @param secretAccessKey Your Amazon S3 secret key.
     */
    public AmazonS3Impl(final String accessKeyId, final String secretAccessKey)
        {
        this.m_accessKeyId = accessKeyId;
        this.m_secretAccessKey = secretAccessKey;
        configureDns();
        }
    
    private static void configureDns()
        {
        // We modify the permanent DNS caching for successful lookups because
        // Amazon periodically changes the machines buckets point to.  See:
        // http://java.sun.com/j2se/1.5.0/docs/api/java/net/InetAddress.html
        // and "DNS Considerations" under "Using Amazon S3" at:
        // http://docs.amazonwebservices.com/AmazonS3/2006-03-01/
        final int cacheSeconds = 1000 * 60 * 60;
        Security.setProperty("networkaddress.cache.ttl", 
            Integer.toString(cacheSeconds));
        }
    
    public void getFile(final String bucketName, final String fileName, 
        final File target) throws IOException
        {
        final String fullPath = 
            this.m_accessKeyId + "-" + bucketName + "/" + fileName;
        final String url = "https://s3.amazonaws.com:443/" + fullPath;
        LOG.debug("Getting file from URL: "+url);
        final GetMethod method = new GetMethod(url);
        normalizeRequest(method, "GET", fullPath, false, true);
        
        final InputStreamHandler handler = 
            new FileInputStreamHandler(target);
        sendRequest(method, handler);
        }
    
    public void getPublicFile(final String bucketName, final String fileName, 
        final File target) throws IOException
        {
        final String fullPath = 
            this.m_accessKeyId + "-" + bucketName + "/" + fileName;
        final String url = "http://s3.amazonaws.com/" + fullPath;
        LOG.debug("Getting file from URL: "+url);
        final GetMethod method = new GetMethod(url);
        
        // This is a public file, so we don't send the authentication token.
        normalizeRequest(method, "GET", fullPath, false, false);
        final InputStreamHandler handler = 
            new FileInputStreamHandler(target);
        sendRequest(method, handler);
        }
    
    public void putFile(final String bucketName, final File file) 
        throws IOException
        {
        try
            {
            final InputStream is = new FileInputStream(file);
            final RequestEntity re = new InputStreamRequestEntity(is);
            put(bucketName+"/"+file.getName(), re, false);
            }
        catch (final FileNotFoundException e)
            {
            LOG.error("File Not Found: "+file, e);
            }
        }
    
    public void putPublicFile(final String bucketName, final File file) 
        throws IOException
        {
        try
            {
            final InputStream is = new FileInputStream(file);
            
            // TODO: This should really use FileRequestEntity because otherwise it loads
            // the whole file into memory to determine the content length.
            final RequestEntity re = new InputStreamRequestEntity(is);
            
            put(bucketName+"/"+file.getName(), re, true);
            }
        catch (final FileNotFoundException e)
            {
            LOG.error("File Not Found: "+file, e);
            }
        }

    public void createBucket(final String bucketName) throws IOException
        {
        put(bucketName, null, false);
        }
    
    private void put(final String relativePath, final RequestEntity re, 
        final boolean isPublic) throws IOException
        {
        final String fullPath = this.m_accessKeyId + "-"+relativePath;
        final String url = "https://s3.amazonaws.com:443/" + fullPath;
        LOG.debug("Sending to URL: "+url);
        final PutMethod method = new PutMethod(url);

        if (re != null)
            {
            method.setRequestEntity(re);
            }
        else
            {
            method.setRequestHeader("Content-Type", "");
            }
        
        final InputStreamHandler handler = new NoOpInputStreamHandler();
        normalizeRequest(method, "PUT", fullPath, isPublic, true);
        sendRequest(method, handler);
        
        }

    public void normalizeRequest(final HttpMethod method, 
        final String methodString, final String fullPath, 
        final boolean addPublicHeader, final boolean useAuth)
        {
        final Header dateHeader = new Header("Date", HttpUtils.createHttpDate());
        method.setRequestHeader(dateHeader);
        if (addPublicHeader)
            {
            final Header publicHeader = new Header("x-amz-acl", "public-read");
            method.setRequestHeader(publicHeader);
            }
        
        if (useAuth)
            {
            final Header auth = createAuthHeader(method, methodString, fullPath);
            method.setRequestHeader(auth);
            }
        }

    private void sendRequest(final HttpMethod method, 
        final InputStreamHandler handler) throws IOException
        {
        final HttpClient client = new HttpClient();
        
        if (LOG.isDebugEnabled())
            {
            printHeaders(method.getRequestHeaders());
            }
        try
            {
            client.executeMethod(method);
            
            final StatusLine statusLine = method.getStatusLine();
            
            final int code = statusLine.getStatusCode();
            if (code < 200 || code > 299)
                {
                LOG.warn("Did not receive 200 level response: "+statusLine);
                final Header[] responseHeaders = method.getResponseHeaders();
                printHeaders(responseHeaders);
                
                // S3 likely returned some sort of error in the response
                // body, so print it out.
                final String response = method.getResponseBodyAsString();
                LOG.warn("Got response: "+response);
                throw new IOException("Error accessing S3");
                }
            else
                {
                final InputStream body = method.getResponseBodyAsStream();
                handler.handleInputStream(body);
                }
            }
        finally
            {
            // Don't forget to release it!
            method.releaseConnection();
            }
        }

    /**
     * Create the S3 authorization header.
     * 
     * @param method The HttpClient {@link HttpMethod}.
     * @param methodString The method identifier string, such as "GET" or "PUT.
     * Amazon's auth token algorithm uses this string.
     * @param fullPath The full path to the resource, such as the bucket or the
     * file.  For example:<p>
     * https://s3.amazonaws.com:443/YOUR_BUCKET_ID-YOUR_BUCKET_NAME/yourFile.txt
     * @return The HTTP header.
     */
    private Header createAuthHeader(final HttpMethod method, 
        final String methodString, final String fullPath)
        {
        final String canonicalString =
            AmazonS3Utils.makeCanonicalString(methodString, fullPath, 
                method.getRequestHeaders());
        final String encodedCanonical = 
            AmazonWsUtils.encode(this.m_secretAccessKey, canonicalString, false);
        
        final String authValue = 
            "AWS " + this.m_accessKeyId + ":" + encodedCanonical;
        final Header auth = new Header("Authorization", authValue);
        return auth;
        }

    private void printHeaders(final Header[] headers)
        {
        for (int i = 0; i < headers.length; i++)
            {
            final Header rh = headers[i];
            LOG.debug("Using header: "+rh);
            }
        }
    }
