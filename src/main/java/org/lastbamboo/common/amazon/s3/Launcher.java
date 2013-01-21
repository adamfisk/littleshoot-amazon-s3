package org.lastbamboo.common.amazon.s3;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.prefs.Preferences;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.lang.StringUtils;
import org.lastbamboo.common.amazon.stack.AwsUtils;
import org.littleshoot.util.Pair;
import org.littleshoot.util.PairImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Accepts arguments to create s3 buckets, upload files, etc.
 */
public class Launcher {

    private static final Logger LOG = LoggerFactory.getLogger(Launcher.class);
    private static final String BUCKETS_KEY = "littleShootS3Buckets";

    private static final class Command {
        private final Collection<Pair<Option, ArgsProcessor>> optionsPairs = 
            new ArrayList<Pair<Option, ArgsProcessor>>();
        private final Options options = new Options();

        private Command() {
        }

        private void add(final Option opt, final String desc,
                final int numArgs, final ArgsProcessor processor) {
            add(opt, desc, numArgs, processor, false);
        }

        private void add(final Option opt, final String desc,
                final int numArgs, final ArgsProcessor processor,
                final boolean optional) {
            addOpt(opt, desc, numArgs, optional);
            optionsPairs.add(new PairImpl<Option, ArgsProcessor>(opt,
                    processor));
        }

        private void addOpt(final Option opt, final String desc, 
            final int numArgs, final boolean optional) {
            opt.setArgs(numArgs);
            opt.setValueSeparator(' ');
            opt.setArgName(desc);
            opt.setOptionalArg(optional);
            options.addOption(opt);
        }

        private void execute(final String[] args) {
            final String bucketFile = "bucket, file";
            final String bucketDir = "bucket, dir";
            final String bucket = "bucket";
            options.addOption("h", "help", false, "Print this message.");
            
            final Option get = new Option("get", "getfile", true,
                    "Gets the specified file.");
            add(get, bucketFile, 2, new Get());

            final Option putPrivate = new Option("put", "putprivate", true,
                    "Adds the specified file to S3 as a private file.");
            add(putPrivate, bucketFile, 2, new PutPrivate());
            
            final Option putPublic = new Option("putp", "putpublic", true,
                    "Adds the specified file to S3 as publicly readable.");
            add(putPublic, bucketFile, 2, new PutPublic());

            final Option putPublicMime = new Option("putpm", "putpublicmime",
                    true,
                    "Adds the specified file to S3 as publicly readable with a custom mime type.");
            add(putPublicMime, bucketFile, 3, new PutPublicMime());

            final Option putAllPrivate = new Option("puta", "putall", true,
                    "Adds all files in the specified directory as private files.");
            add(putAllPrivate, bucketDir, 2, new PutAllPrivate());

            final Option putAllPublic = new Option("putap", "putallpublic",
                    true,
                    "Adds all files in the specified directory as public files.  Does not add "
                            + "directories or recurse.");
            add(putAllPublic, bucketDir, 2, new PutAllPublic());

            final Option proxy = new Option("x", "proxy", true,
                    "Sets the proxy to use.");
            
            // This is redundant given the later custom proxy config, but oh
            // well.
            add(proxy, "host:port", 1, new ProxyProcessor());
            
            final Option delete = new Option(
                    "rm",
                    "delete",
                    true,
                    "Removes the file in the specified bucket with the specified name.  Note that "
                            + "a '*' at the beginning or the end acts as a wildcard.  For example,"
                            + "'aws -rm littleshoot *.sh' removes all .sh files in the littleshoot "
                            + "bucket.  Use the star functionality with some caution, of course.  It only works"
                            + " at the beginning or end of the file name.");
            add(delete, bucketFile, 2, new Delete());

            final Option deleteBucket = new Option("rmdir", "deletebucket",
                    true, "Removes the specified bucket if it's empty.");
            add(deleteBucket, bucket, 1, new DeleteBucket());

            final Option newBucket = new Option("mkdir", "makebucket", true,
                    "Creates the specified bucket.");
            add(newBucket, bucket, 1, new CreateBucket());

            final Option listBucket = new Option(
                    "ls",
                    "listbucket",
                    true,
                    "Lists all the files in the specified bucket.  Lists all " +
                    "buckets if no bucket name is given.");
            // This makes the bucket name optional.
            add(listBucket, bucket, 1, new ListBucket(), true);

            final Option verbose = new Option("v", "verbose", false,
                    "Provides verbose output.");
            options.addOption(verbose);
            final CommandLineParser parser = new GnuParser();

            try {
                final CommandLine cmd = parser.parse(options, args);

                if (cmd.hasOption(verbose.getOpt())) {
                    //Logger.getRootLogger().setLevel(Level.ALL);
                }
                
                if (cmd.hasOption(proxy.getOpt())) {
                    final String[] values = convertVals(cmd, proxy);
                    final ArgsProcessor processor = new ProxyProcessor();
                    processor.processArgs(values);
                }

                for (final Pair<Option, ArgsProcessor> optionPair : optionsPairs) {
                    final Option opt = optionPair.getFirst();
                    if (cmd.hasOption(opt.getOpt())) {
                        final ArgsProcessor processor = optionPair.getSecond();
                        final String[] values = convertVals(cmd, opt);
                        processor.processArgs(values);
                    }
                }
                if (args.length == 0 || cmd.hasOption("h")) {
                    printHelp();
                } else if (cmd.getOptions().length == 0) {
                    System.err.println("Could not understand the options you specified.  "
                                    + "Printing help.");
                    printHelp();

                    System.out.println();
                    System.err.println("Could not understand the options you specified.  "
                                    + "See above help.");
                }
            } catch (final ParseException e) {
                System.err.println(e.getMessage());
            }
        }


        private static String[] convertVals(CommandLine cmd, Option opt) {
            final String[] values = cmd.getOptionValues(opt.getOpt());
            if (values == null) {
                return new String[0];
            }
            return values;
        }
        
        private void printHelp() {
            final HelpFormatter formatter = new HelpFormatter();
            // formatter.setLeftPadding(2);
            // formatter.setDescPadding(2);
            // formatter.setWidth(80);
            formatter.printHelp("aws", options);
        }
    }

    /**
     * Called from the command line.
     * 
     * @param args The command line arguments.
     */
    public static void main(final String[] args) {
        final Command bean = new Command();
        bean.execute(args);
    }

    private static interface ArgsProcessor {
        void processArgs(final String[] values);
    };

    private static class PutAllPrivate implements ArgsProcessor {
        public void processArgs(final String[] args) {
            final AmazonS3 s3 = setup(args, 2, "bucketName directoryPath");
            final String bucketName = args[0];

            final File dir = new File(args[1]);

            if (!dir.isDirectory()) {
                System.out.println(dir
                        + " does not appear to be a valid directory.");
                return;
            }
            try {
                s3.putPrivateDir(bucketName, dir);
            } catch (final IOException e) {
                System.out.println("Could not put all files.");
                e.printStackTrace();
            }
        }
    };
    
    private static class ProxyProcessor implements ArgsProcessor {

        @Override
        public void processArgs(final String[] values) {
            LOG.debug("Processing proxy");
            final String hostPort = values[0];
            final String host = StringUtils.substringBefore(hostPort, ":");
            if (StringUtils.isBlank(host)) {
                System.err.println("Format: host:port");
                return;
            }
            final String portStr = StringUtils.substringAfter(hostPort, ":");
            if (!StringUtils.isNumeric(portStr)) {
                System.err.println("Format: host:port");
                return;
            }
            final int port = Integer.parseInt(portStr);
            LOG.debug("Setting proxy data");
            GlobalOptions.setProxyHost(host);
            GlobalOptions.setProxyPort(port);
        }
    }

    private static class PutAllPublic implements ArgsProcessor {
        public void processArgs(final String[] args) {
            final AmazonS3 s3 = setup(args, 2, "bucketName directoryPath");
            final String bucketName = args[0];

            final File dir = new File(args[1]);

            if (!dir.isDirectory()) {
                System.out.println(dir
                        + " does not appear to be a valid directory.");
                return;
            }
            try {
                s3.putPublicDir(bucketName, dir);
            } catch (IOException e) {
                System.out.println("Could not put all files.");
                e.printStackTrace();
            }
        }
    }

    private static class Delete implements ArgsProcessor {
        public void processArgs(final String[] args) {
            final AmazonS3 s3 = setup(args, 2,
                    "bucketName name where name is has a '*' prefix or suffix, or both");
            final String bucketName = args[0];
            final String regEx = args[1];
            try {
                s3.deleteStar(bucketName, regEx);
            } catch (final IOException e) {
                System.out.println("Could not delete regex: " + regEx);
                e.printStackTrace();
            }
        }
    }

    private static class PutPrivate implements ArgsProcessor {
        public void processArgs(final String[] args) {
            final AmazonS3 s3 = setup(args, 2, "bucketName fileName");
            final String bucketName = args[0];
            createBucket(bucketName, s3);
            final String fileString = args[1];
            final File file = new File(fileString);
            if (!file.isFile()) {
                System.out.println("File not found: " + fileString);
                System.exit(1);
            }
            try {
                s3.putPrivateFile(bucketName, file);
            } catch (final IOException e) {
                System.out.println("Could not upload file.  Error was: ");
                e.printStackTrace();
                System.exit(1);
            }
        }
    }

    private static class PutPublic implements ArgsProcessor {

        public void processArgs(final String[] args) {
            final AmazonS3 s3 = setup(args, 2, "bucketName fileName");
            final String bucketName = args[0];
            createBucket(bucketName, s3);
            final String fileString = args[1];
            final File file = new File(fileString);
            if (!file.isFile()) {
                System.out.println("File not found: " + fileString);
                return;
            }
            try {
                s3.putPublicFile(bucketName, file);
            } catch (final IOException e) {
                System.out.println("Could not upload file.  Error was: ");
                e.printStackTrace();
                System.exit(1);
            }
        }
    }

    private static class PutPublicMime implements ArgsProcessor {

        public void processArgs(final String[] args) {
            final AmazonS3 s3 = setup(args, 3, "bucketName fileName mimeType");
            final String bucketName = args[0];
            createBucket(bucketName, s3);
            final String fileString = args[1];
            final File file = new File(fileString);
            if (!file.isFile()) {
                System.out.println("File not found: " + fileString);
                return;
            }
            final String mimeType = args[2];
            try {
                s3.putPublicFile(bucketName, file, mimeType);
            } catch (final IOException e) {
                System.out.println("Could not upload file.  Error was: ");
                e.printStackTrace();
                System.exit(1);
            }
        }
    }

    private static void createBucket(final String bucketName, final AmazonS3 s3) {
        try {
            if (AwsUtils.getUrlBase().contains("archive.org")) {
                System.out.println("Not creating bucket for URL base: "
                        + AwsUtils.getUrlBase());
                return;
            }
        } catch (final IOException e1) {
            e1.printStackTrace();
        }
        final Preferences prefs = Preferences.userRoot();
        final String createdBuckets = prefs.get(BUCKETS_KEY, "");
        if (!createdBuckets.contains("," + bucketName + ",")
                && !createdBuckets.endsWith("," + bucketName)) {
            try {
                s3.createBucket(bucketName);
                prefs.put(BUCKETS_KEY, createdBuckets + "," + bucketName);
            } catch (final IOException e) {
                System.out.println("Could not create bucket. Already exists?");
                e.printStackTrace();
            }
        }
    }

    private static void deleteBucket(final String bucketName, final AmazonS3 s3) {
        final Preferences prefs = Preferences.userRoot();
        final String createdBuckets = prefs.get(BUCKETS_KEY, "");

        try {
            s3.deleteBucket(bucketName);
            if (createdBuckets.contains("," + bucketName + ",")) {
                prefs.put(BUCKETS_KEY,
                        createdBuckets.replace("," + bucketName + ",", ","));
            } else if (createdBuckets.endsWith("," + bucketName)) {
                final String strippedBuckets = StringUtils.substringBeforeLast(
                        createdBuckets, "," + bucketName);
                prefs.put(BUCKETS_KEY, strippedBuckets);
            }
        } catch (final IOException e) {
            System.out.println("Could not delete bucket.");
            e.printStackTrace();
        }
    }

    private static class Get implements ArgsProcessor {
        public void processArgs(final String[] args) {
            final AmazonS3 s3 = setup(args, 2, "bucketName fileName");

            final String bucketName = args[0];
            final String fileName = args[1];
            final File target = new File(fileName);
            try {
                s3.getPrivateFile(bucketName, fileName, target);
            } catch (final IOException e) {
                System.out.println("There was an error getting the file.");
                e.printStackTrace();
                System.exit(1);
            }
        }
    }

    private static class ListBucket implements ArgsProcessor {
        public void processArgs(final String[] args) {
            final AmazonS3 s3 = setup(args, 0, "bucketName");
            if (args.length == 0) {
                try {
                    s3.listBuckets();
                } catch (final IOException e) {
                    System.out
                            .println("There was an error listing the bucket.");
                    e.printStackTrace();
                }
            } else {
                final String bucketName = args[0];
                try {
                    s3.listBucket(bucketName);
                } catch (final IOException e) {
                    System.out
                            .println("There was an error listing the bucket.");
                    e.printStackTrace();
                }
            }
        }
    }

    private static class CreateBucket implements ArgsProcessor {
        public void processArgs(final String[] args) {
            final AmazonS3 s3 = setup(args, 1, "bucketName");
            final String bucketName = args[0];
            try {
                s3.createBucket(bucketName);
            } catch (final IOException e) {
                System.out.println("There was an error creating the bucket.");
                e.printStackTrace();
            }
        }
    }

    private static class DeleteBucket implements ArgsProcessor {
        public void processArgs(final String[] args) {
            final AmazonS3 s3 = setup(args, 1, "bucketName");
            final String bucketName = args[0];
            deleteBucket(bucketName, s3);
        }
    }

    private static AmazonS3 setup(final String[] args, final int length,
            final String message) {
        checkArgs(args, length, message);
        try {
            return new AmazonS3Impl();
        } catch (final IOException e) {
            System.out.println("Error loading props files...");
            throw new IllegalArgumentException("Error loading props files", e);
        }
    }

    private static void checkArgs(final String[] args, final int length,
            final String message) {
        if (args.length < length) {
            final StringBuilder sb = new StringBuilder();
            sb.append("Too few args.  Expected " + length);
            sb.append(" but found " + args.length);
            sb.append(" in ");
            for (final String arg : args) {
                sb.append(arg);
                sb.append(" ");
            }
            System.err.println(sb);
            System.exit(1);
        }
    }

}
