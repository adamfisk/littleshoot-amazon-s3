package org.lastbamboo.common.amazon.s3;

import java.io.File;
import java.io.IOException;

/**
 * Interface for interacting with an Amazon S3 repository.
 */
public interface AmazonS3
    {

    /**
     * Creates a new bucket.
     * 
     * @param name The name of the bucket.
     * @throws IOException If we could either could not make a network 
     * connection to S3 or could not understand the HTTP exchange.
     */
    void createBucket(String name) throws IOException;

    /**
     * Uploads a file to S3.
     *
     * @param bucketName The name of the bucket.
     * @param file The file to upload.
     * @throws IOException If we could either could not make a network 
     * connection to S3 or could not understand the HTTP exchange.
     */
    void putFile(String bucketName, File file) throws IOException;

    /**
     * Downloads a file from the specified bucket and file name to the 
     * specified local path.
     * 
     * @param bucketName The name of the Amazon S3 bucket.
     * @param fileName The name of the file within the bucket.
     * @param target The name of the local file to download to.
     * @throws IOException If we could either could not make a network 
     * connection to S3 or could not understand the HTTP exchange.
     */
    void getFile(String bucketName, String fileName, File target)
        throws IOException;
    }
