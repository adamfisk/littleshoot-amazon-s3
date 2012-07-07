package org.lastbamboo.common.amazon.s3;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.Writer;

import javax.activation.MimetypesFileTypeMap;

import junit.framework.TestCase;

/**
 * This test needs to be run separately from the command line so we can pass
 * in our keys on the command line instead of checking them in to the 
 * repository.
 */
public class AmazonS3Test extends TestCase {

    private void mimeTypes() throws Exception {
        final MimetypesFileTypeMap map = new MimetypesFileTypeMap();
        map.addMimeTypes("application/x-apple-diskimage dmg\n");
        assertEquals("application/x-apple-diskimage",
                map.getContentType("test.dmg"));
        // System.out.println(map.getContentType("test.exe"));
        // System.out.println(map.getContentType("test.xml"));
        // System.out.println(map.getContentType("test.txt"));
        // System.out.println(map.getContentType("test.html"));
        // final RequestEntity re = new FileRequestEntity(file, mimeType);

    }
    
    public void testAws() throws Exception {
        // final String accessKeyId = AwsUtils.getAccessKeyId();//args[0];
        // final String secretAccessKey = AwsUtils.getAccessKey();//args[1];

        final AmazonS3 amazon = new AmazonS3Impl();

        final String bucketName = "installersssssssss";
        amazon.createBucket(bucketName);

        //final File testFile = new File("test.file");
        System.out.println("MADE BUCKET!!");
        final File testFile = new File("test.txt");
        testFile.deleteOnExit();
        if (testFile.isFile()) {
            testFile.delete();
        }

        final Writer fw = new FileWriter(testFile);
        final String fileText = "Amazon S3 Test File";
        fw.write(fileText);
        fw.close();

        amazon.putPublicFile(bucketName, testFile);

        System.out.println("Downloading file...");
        final File file = new File("s3File.test");
        file.deleteOnExit();

        amazon.getPublicFile(bucketName, testFile.getName(), file);
        assertEquals(testFile.length(), file.length());

        final BufferedReader br = new BufferedReader(new FileReader(file));
        final String content = br.readLine();

        System.out.println("Got file Content: " + content);
        assertEquals(fileText, content);
    }
}
