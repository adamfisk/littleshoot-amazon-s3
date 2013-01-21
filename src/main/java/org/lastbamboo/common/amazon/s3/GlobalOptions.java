package org.lastbamboo.common.amazon.s3;

public class GlobalOptions {

    private static String proxyHost;
    
    private static int proxyPort;

    public static String getProxyHost() {
        return proxyHost;
    }

    public static void setProxyHost(String proxyHost) {
        GlobalOptions.proxyHost = proxyHost;
    }

    public static int getProxyPort() {
        return proxyPort;
    }

    public static void setProxyPort(int proxyPort) {
        GlobalOptions.proxyPort = proxyPort;
    }
}
