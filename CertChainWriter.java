import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.Base64.Encoder;
import java.net.Socket;

public class CertChainWriter {

  final String beginCert = "-----BEGIN CERTIFICATE-----";
  final String endCert = "-----END CERTIFICATE-----";
  final String certFilenameFormat = "cert%d.pem";
  final String protocol = "TLS";

  String tunnelHost;
  Integer tunnelPort;
  SSLSocket socket;

  public void makeConnection(String host, int port) {
    try {
      TrustManager[] trustAllCerts = new TrustManager[] {
        new X509TrustManager() {
          public java.security.cert.X509Certificate[] getAcceptedIssuers() {
            return null;
          }
          public void checkClientTrusted(X509Certificate[] certs, String authType) {}
          public void checkServerTrusted(X509Certificate[] certs, String authType) {}
        }
      };
      SSLContext sc = SSLContext.getInstance(protocol);
      sc.init(null, trustAllCerts, new java.security.SecureRandom());

      HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
      SSLSocketFactory factory = HttpsURLConnection.getDefaultSSLSocketFactory();

      tunnelHost = System.getProperty("https.proxyHost");
      tunnelPort = Integer.getInteger("https.proxyPort");
      if (tunnelHost != null && tunnelPort != null) {
        Socket tunnel = new Socket(tunnelHost, tunnelPort.intValue());
        doTunnelHandshake(tunnel, host, port);
        socket = (SSLSocket) factory.createSocket(tunnel, host, port, true);
      } else {
        socket = (SSLSocket) factory.createSocket(host, port);
      }

      socket.startHandshake();
      SSLSession session = socket.getSession();
      java.security.cert.Certificate[] servercerts = session.getPeerCertificates();

      for (int i = 1; i < servercerts.length; i++) {
        Encoder encoder = Base64.getEncoder();
        System.out.println(beginCert);
        System.out.println(encoder.encodeToString(servercerts[i].getEncoded()));
        System.out.println(endCert);
        BufferedWriter writer = new BufferedWriter(new FileWriter(String.format(certFilenameFormat, i)));
        writer.write(beginCert);
        writer.newLine();
        writer.write(encoder.encodeToString(servercerts[i].getEncoded()));
        writer.newLine();
        writer.write(endCert);
        writer.newLine();
        writer.close();
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
  public static void main(String args[]) throws Exception {
    String host = args[0];
    int port = Integer.parseInt(args[1]);
    new CertChainWriter().makeConnection(host, port);
  }

  /*
  * Tell our tunnel where we want to CONNECT, and look for the
  * right reply.  Throw IOException if anything goes wrong.
  */
  private void doTunnelHandshake(Socket tunnel, String host, int port) throws IOException {
    OutputStream out = tunnel.getOutputStream();
    String msg = "CONNECT " + host + ":" + port + " HTTP/1.0\n"
                  + "User-Agent: https://github.com/ctcampbell/certchainwriter"
                  + "\r\n\r\n";
    byte b[];
    try {
        /*
        * We really do want ASCII7 -- the http protocol doesn't change
        * with locale.
        */
        b = msg.getBytes("ASCII7");
    } catch (UnsupportedEncodingException ignored) {
        /*
        * If ASCII7 isn't there, something serious is wrong, but
        * Paranoia Is Good (tm)
        */
        b = msg.getBytes();
    }
    out.write(b);
    out.flush();

    /*
    * We need to store the reply so we can create a detailed
    * error message to the user.
    */
    byte            reply[] = new byte[200];
    int             replyLen = 0;
    int             newlinesSeen = 0;
    boolean         headerDone = false;     /* Done on first newline */

    InputStream     in = tunnel.getInputStream();
    boolean         error = false;

    while (newlinesSeen < 2) {
        int i = in.read();
        if (i < 0) {
            throw new IOException("Unexpected EOF from proxy");
        }
        if (i == '\n') {
            headerDone = true;
            ++newlinesSeen;
        } else if (i != '\r') {
            newlinesSeen = 0;
            if (!headerDone && replyLen < reply.length) {
                reply[replyLen++] = (byte) i;
            }
        }
    }

    /*
    * Converting the byte array to a string is slightly wasteful
    * in the case where the connection was successful, but it's
    * insignificant compared to the network overhead.
    */
    String replyStr;
    try {
        replyStr = new String(reply, 0, replyLen, "ASCII7");
    } catch (UnsupportedEncodingException ignored) {
        replyStr = new String(reply, 0, replyLen);
    }

    /* We asked for HTTP/1.0, so we should get that back */
    if (!replyStr.startsWith("HTTP/1.0 200")) {
        throw new IOException("Unable to tunnel through "
                + tunnelHost + ":" + tunnelPort
                + ".  Proxy returns \"" + replyStr + "\"");
    }

    /* tunneling Handshake was successful! */
  }
}
