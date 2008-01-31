package org.lastbamboo.common.amazon.s3;

import java.io.File;
import java.io.IOException;

/**
 * Accepts arguments to create s3 buckets, upload files, etc.
 */
public class Launcher
    {

    /**
     * Called from the command line.
     * 
     * @param args The command line arguments.
     */
    public static void main (final String[] args)
        {
        if (args.length < 1)
            {
            throw new IllegalArgumentException("Need at least a method"); 
            }
        
        if (args[0].equals("put"))
            {
            processPut(args);
            }
        
        else if (args[0].equals("get"))
            {
            
            }
        else if (args[0].equals("list"))
            {
            
            }
        }
    
    private static void processPut(final String[] args)
        {
        if (args.length < 4)
            {
            throw new IllegalArgumentException("Too few args.");
            }
        
        final AmazonS3 s3;
        try
            {
            s3 = new AmazonS3Impl();
            }
        catch (final IOException e)
            {
            System.out.println("Error loading props files...");
            return;
            }
        final String bucketName = args[2];
        try
            {
            s3.createBucket(bucketName);
            }
        catch (final IOException e)
            {
            System.out.println("Could not create bucket.  Already exists?");
            e.printStackTrace();
            }
        final String fileString = args[3];
        final File file = new File(fileString);
        if (!file.isFile())
            {
            System.out.println("File not found: "+fileString);
            return;
            }
        
        final String restrictions = args[1];
        if (restrictions.trim().equalsIgnoreCase("public"))
            {
            try
                {
                s3.putPublicFile(bucketName, file);
                }
            catch (final IOException e)
                {
                System.out.println("Could not upload file.  Error was: ");
                e.printStackTrace();
                }
            }
        else if (restrictions.trim().equalsIgnoreCase("private"))
            {
            try
                {
                s3.putFile(bucketName, file);
                }
            catch (final IOException e)
                {
                System.out.println("Could not upload file.  Error was: ");
                e.printStackTrace();
                }
            }
        else
            {
            throw new IllegalArgumentException("Must specify public or private...");
            }
        }
    }
