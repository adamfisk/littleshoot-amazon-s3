package org.lastbamboo.common.amazon.s3;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

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
        }
    
    public void getFile(final String bucketName, final String fileName, 
        final File target) throws IOException
        {
        final String fullPath = 
            this.m_accessKeyId + "-" + bucketName + "/" + fileName;
        final String url = "https://s3.amazonaws.com:443/" + fullPath;
        LOG.debug("Getting file from URL: "+url);
        final GetMethod method = new GetMethod(url);
        
        final InputStreamHandler handler = 
            new FileInputStreamHandler(target);
        sendRequest(method, "GET", fullPath, handler);
        }
    
    public void putFile(final String bucketName, final File file) 
        throws IOException
        {
        try
            {
            final InputStream is = new FileInputStream(file);
            final RequestEntity re = new InputStreamRequestEntity(is);
            put(bucketName+"/"+file.getName(), re);
            }
        catch (final FileNotFoundException e)
            {
            LOG.error("File Not Found: "+file, e);
            }
        }

    public void createBucket(final String bucketName) throws IOException
        {
        put(bucketName, null);
        }
    
    private void put(final String relativePath, final RequestEntity re) 
        throws IOException
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
        
        sendRequest(method, "PUT", fullPath);
        }

    private void sendRequest(final HttpMethod method, final String methodString,
        final String fullPath) throws IOException
        {
        final InputStreamHandler handler = new NoOpInputStreamHandler();
        sendRequest(method, methodString, fullPath, handler);
        }

    private void sendRequest(final HttpMethod method, final String methodString, 
        final String fullPath, final InputStreamHandler handler) 
        throws IOException
        {
        final HttpClient client = new HttpClient();
        
        final Header dateHeader = new Header("Date", 
            HttpUtils.createHttpDate());
        method.setRequestHeader(dateHeader);
        final Header auth = createAuthHeader(method, methodString, fullPath);
        method.setRequestHeader(auth);
        
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
                if (LOG.isDebugEnabled())
                    {
                    LOG.debug("Received status line: "+statusLine);
                    final Header[] responseHeaders = 
                        method.getResponseHeaders();
                    printHeaders(responseHeaders);
                    
                    // S3 likely returned some sort of error in the response
                    // body, so print it out.
                    final String response = method.getResponseBodyAsString();
                    LOG.debug("Got response: "+response);
                    }
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
            AmazonS3Utils.encode(this.m_secretAccessKey, canonicalString, false);
        
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
