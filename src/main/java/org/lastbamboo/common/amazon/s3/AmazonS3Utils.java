//  This software code is made available "AS IS" without warranties of any
//  kind.  You may copy, display, modify and redistribute the software
//  code either by itself or as incorporated into your code; provided that
//  you do not remove any proprietary notices.  Your use of this software
//  code is at your own risk and you waive any claim against Amazon
//  Digital Services, Inc. or its affiliates with respect to your use of
//  this software code. (c) 2006 Amazon Digital Services, Inc. or its
//  affiliates.

package org.lastbamboo.common.amazon.s3;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Iterator;
import java.util.SortedMap;
import java.util.TreeMap;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.httpclient.Header;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Utility methods for using Amazon S3.  This is a modified version of the
 * utilities class that Amazon distributes as example code.
 */
public class AmazonS3Utils 
    {
    
    private static final Log LOG = LogFactory.getLog(AmazonS3Utils.class);
    
    static final String METADATA_PREFIX = "x-amz-meta-";
    static final String AMAZON_HEADER_PREFIX = "x-amz-";
    static final String ALTERNATIVE_DATE_HEADER = "x-amz-date";
    static final String DEFAULT_HOST = "s3.amazonaws.com";
    static final int SECURE_PORT = 443;
    static final int INSECURE_PORT = 80;
    
    /**     
     * HMAC/SHA1 Algorithm per RFC 2104.     
     */    
    private static final String HMAC_SHA1_ALGORITHM = "HmacSHA1";

    static String makeCanonicalString(String method, String resource, 
        Header[] headers) 
        {
        return makeCanonicalString(method, resource, headers, null);
        }

    private static String makeCanonicalString(String method, 
        final String resource, final Header[] headers, final String expires)
        {
        final StringBuilder buf = new StringBuilder();
        buf.append(method + "\n");

        // Add all interesting headers to a list, then sort them.  "Interesting"
        // is defined as Content-MD5, Content-Type, Date, and x-amz-
        final SortedMap<String, String> interestingHeaders = 
            new TreeMap<String, String>();
        if (headers != null) 
            {
            for (int i = 0; i < headers.length; i++)
                {
                final Header curHeader = headers[i];
                final String lk = curHeader.getName().toLowerCase();
                // Ignore any headers that are not particularly interesting.
                if (lk.equals("content-type") || lk.equals("content-md5") || 
                    lk.equals("date") ||
                    lk.startsWith(AMAZON_HEADER_PREFIX))
                    {
                    final String headersList = curHeader.getValue();
                    LOG.debug("Our headers list: "+headersList);
                    interestingHeaders.put(lk, headersList);
                    }
                }
            }

        if (interestingHeaders.containsKey(ALTERNATIVE_DATE_HEADER)) 
            {
            interestingHeaders.put("date", "");
            }

        // if the expires is non-null, use that for the date field.  this
        // trumps the x-amz-date behavior.
        if (expires != null) 
            {
            interestingHeaders.put("date", expires);
            }

        // these headers require that we still put a new line in after them,
        // even if they don't exist.
        if (! interestingHeaders.containsKey("content-type")) 
            {
            interestingHeaders.put("content-type", "");
            }
        if (! interestingHeaders.containsKey("content-md5")) 
            {
            interestingHeaders.put("content-md5", "");
            }

        // Finally, add all the interesting headers (i.e.: all that startwith x-amz- ;-))
        for (Iterator<String> i = interestingHeaders.keySet().iterator(); 
            i.hasNext();) 
            {
            final String key = i.next();
            if (key.startsWith(AMAZON_HEADER_PREFIX)) 
                {
                buf.append(key).append(':').append(interestingHeaders.get(key));
                } 
            else 
                {
                buf.append(interestingHeaders.get(key));
                }
            buf.append("\n");
            }

        // don't include the query parameters...
        int queryIndex = resource.indexOf('?');
        if (queryIndex == -1) 
            {
            buf.append("/" + resource);
            } 
        else 
            {
            buf.append("/" + resource.substring(0, queryIndex));
            }

        // ...unless there is an acl or torrent parameter
        if (resource.matches(".*[&?]acl($|=|&).*")) 
            {
            buf.append("?acl");
            } 
        else if (resource.matches(".*[&?]torrent($|=|&).*")) 
            {
            buf.append("?torrent");
            } 
        else if (resource.matches(".*[&?]logging($|=|&).*")) 
            {
            buf.append("?logging");
            }

        return buf.toString();
        }
    
    /**
     * Calculate the HMAC/SHA1 on a string.
     * @param data Data to sign
     * @param passcode Passcode to sign it with
     * @return Signature
     * @throws RuntimeException If the algorithm does not exist or if the key
     * is invalid -- both should never happen.
     */
    static String encode(final String awsSecretAccessKey, 
        final String canonicalString, final boolean urlencode)
        {
        // The following HMAC/SHA1 code for the signature is taken from the
        // AWS Platform's implementation of RFC2104 (amazon.webservices.common.Signature)
        //
        // Acquire an HMAC/SHA1 from the raw key bytes.
        final SecretKeySpec signingKey =
            new SecretKeySpec(awsSecretAccessKey.getBytes(), HMAC_SHA1_ALGORITHM);

        // Acquire the MAC instance and initialize with the signing key.
        final Mac mac;
        try 
            {
            mac = Mac.getInstance(HMAC_SHA1_ALGORITHM);
            } 
        catch (final NoSuchAlgorithmException e) 
            {
            // should not happen
            throw new RuntimeException("Could not find sha1 algorithm", e);
            }
        try 
            {
            mac.init(signingKey);
            } 
        catch (final InvalidKeyException e) 
            {
            // also should not happen
            throw new RuntimeException("Could not initialize the MAC algorithm", e);
            }

        // Compute the HMAC on the digest, and set it.
        final String b64 = 
            Base64.encodeBytes(mac.doFinal(canonicalString.getBytes()));

        if (urlencode) 
            {
            return urlEncode(b64);
            } 
        else 
            {
            return b64;
            }
        }
    
    private static String urlEncode(final String unencoded) 
        {
        try
            {
            return URLEncoder.encode(unencoded, "UTF-8");
            }
        catch (final UnsupportedEncodingException e)
            {
            // should never happen
            throw new RuntimeException("Could not url encode to UTF-8", e);
            }
        }
    }
