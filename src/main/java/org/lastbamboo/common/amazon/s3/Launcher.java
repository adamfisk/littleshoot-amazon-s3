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
        System.out.println("Calling with args: "+args);
        if (args.length < 1)
            {
            throw new IllegalArgumentException("Need at least a method"); 
            }
        
        if (args[0].equals("putPublic"))
            {
            processPutPublic(args);
            }
        if (args[0].equals("putPrivate"))
            {
            processPutPrivate(args);
            }
        else if (args[0].equals("get"))
            {
            processGet(args);
            }
        else if (args[0].equals("delete"))
            {
            processDelete(args);
            }
        else if (args[0].equals("createBucket"))
            {
            processCreateBucket(args);
            }
        else if (args[0].equals("deleteBucket"))
            {
            processDeleteBucket(args);
            }
        
        else if (args[0].equals("list"))
            {
            processList(args);
            }
        }

    private static void processDelete(final String[] args)
        {
        final AmazonS3 s3 = setup(args, 3, 
            "Usage: shootDeleteFile.sh bucketName fileName");
        final String bucketName = args[1];
        final String file = args[2];
        try
            {
            s3.delete(bucketName, file);
            }
        catch (final IOException e)
            {
            System.out.println("Could not delete file.");
            e.printStackTrace();
            }
        }
    private static void processPutPrivate(final String[] args)
        {
        final AmazonS3 s3 = setup(args, 3,
                "Usage: shootPutPublic.sh bucketName fileName");
        final String bucketName = args[1];
        try
            {
            s3.createBucket(bucketName);
            }
        catch (final IOException e)
            {
            System.out.println("Could not create bucket.  Already exists?");
            e.printStackTrace();
            }
        final String fileString = args[2];
        final File file = new File(fileString);
        if (!file.isFile())
            {
            System.out.println("File not found: "+fileString);
            return;
            }

        try
            {
            s3.putPrivateFile(bucketName, file);
            }
        catch (final IOException e)
            {
            System.out.println("Could not upload file.  Error was: ");
            e.printStackTrace();
            }
        }
    
    private static void processPutPublic(final String[] args)
        {
        final AmazonS3 s3 = setup(args, 3,
                "Usage: shootPutPublic.sh bucketName fileName");
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

    private static void processGet(final String[] args)
        {
        final AmazonS3 s3 = setup(args, 3,
                "Usage: shootGet.sh bucketName fileName");
        
        final String bucketName = args[1];
        final String fileName = args[2];
        final File target = new File(fileName);
        try
            {
            s3.getPrivateFile(bucketName, fileName, target);
            }
        catch (final IOException e)
            {
            System.out.println("There was an error getting the file.");
            e.printStackTrace();
            }
        }

    private static void processList(final String[] args)
        {
        final AmazonS3 s3 = setup(args, 2,
            "Usage: shootListBucket.sh bucketName");
        final String bucketName = args[1];
        try
            {
            s3.listBucket(bucketName);
            }
        catch (final IOException e)
            {
            System.out.println("There was an error listing the bucket.");
            e.printStackTrace();
            }
        }

    private static void processCreateBucket(final String[] args)
        {
        final AmazonS3 s3 = setup(args, 2,
                "Usage: shootCreateBucket.sh bucketName");
        final String bucketName = args[1];
        try
            {
            s3.createBucket(bucketName);
            }
        catch (final IOException e)
            {
            System.out.println("There was an error creating the bucket.");
            e.printStackTrace();
            }
        }
    
    private static void processDeleteBucket(final String[] args)
        {
        final AmazonS3 s3 = setup(args, 2,
                "Usage: shootDeleteBucket.sh bucketName");
        final String bucketName = args[1];
        try
            {
            s3.deleteBucket(bucketName);
            }
        catch (final IOException e)
            {
            System.out.println("There was an error creating the bucket.");
            e.printStackTrace();
            }
        }
    
    private static AmazonS3 setup(final String[] args, final int length, final String message)
        {
        checkArgs(args, length, message);
        try
            {
            return new AmazonS3Impl();
            }
        catch (final IOException e)
            {
            System.out.println("Error loading props files...");
            throw new IllegalArgumentException("Error loading props files");
            }
        }
    
    private static void checkArgs(final String[] args, final int length, final String message)
        {
        if (args.length < length)
            {
            System.out.println(message);
            throw new IllegalArgumentException("Too few args.");
            }
        }
    }
