package com.netflix.governator.guice.jetty;

import com.google.inject.AbstractModule;
import com.google.inject.multibindings.Multibinder;
import com.sun.jersey.core.util.Base64;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.util.ssl.SslContextFactory;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.security.KeyFactory;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.PKCS8EncodedKeySpec;

public class JettySslModule extends AbstractModule {

    // Generated with
    //   openssl req -x509 -newkey rsa:2048 -keyout key.pem -out cert.pem -days 365000 -subj '/CN=localhost/' -nodes

    private static final String CERTIFICATE =
            "MIIC/TCCAeWgAwIBAgIJAICIMvFHSibpMA0GCSqGSIb3DQEBCwUAMBQxEjAQBgNV" +
            "BAMMCWxvY2FsaG9zdDAgFw0xNjA5MDYyMDM3NDJaGA8zMDE2MDEwODIwMzc0Mlow" +
            "FDESMBAGA1UEAwwJbG9jYWxob3N0MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIB" +
            "CgKCAQEAx22xi43/Y/DwPJczTXjs8fQFOdH5PmtNCteaJMAG14wb3LZd55EEstJ6" +
            "lKy9LqTOywlsiIFMWDvOs5yUxFeak5OwJ8/84xfblCLyQZ7CHnkBcvXpx3jM934j" +
            "rdgP+X2GeGj3QC8u/Qs3vlFaOoAB/k/sLm15VO3j9b4bp0E76joWOhwQieBe5/Qc" +
            "ZvXUhfL+5DGoO9ROejP3+M9TM74L+ceQpN/8m71lUDiTOk13UhcqlFD7YSxWIzGq" +
            "6maHa8z54L+Zuis6juHsgQHASZsYhN7nynW3+KBSNrs859x/WSPmy/zYwh08W8lm" +
            "4kdjVE5TXw/NDjFnngwv16vtejh8gwIDAQABo1AwTjAdBgNVHQ4EFgQU4cWlr1ut" +
            "ZZu4Qp3WVAzd7a1R5LAwHwYDVR0jBBgwFoAU4cWlr1utZZu4Qp3WVAzd7a1R5LAw" +
            "DAYDVR0TBAUwAwEB/zANBgkqhkiG9w0BAQsFAAOCAQEAHsClc/3ftd9A/95Xj6H0" +
            "g10hlqb0EAFRM97/DiDwt9ESdFoBnE51Zv7BmcPN0CBCyHMj/MnaB/BijuDR0N8/" +
            "7h2R97rsXbNJkSFtLeV7cXBD/sXi+e8INX3rqADWQJ24Buv55miyR6M3CarY6yNl" +
            "i+gnulM4jFRSq7jvfYUbzA9mi/fCqGI9F4poS2QKxMiyx5xcU57u3mHJYD+JbS+W" +
            "UywJfv9PXkBkHxO9yEYJ3DG78CPUv45CsBCmoBRN7TDndpwDCqpXbXlxME2i8ljL" +
            "yKg6WQUrig6xdE0yRcLoY8n3ibuNghDPBTEjhQ4UTs2JfISaBB/T2fZP8PQXqUgm" +
            "Ag==";
    private static final String PRIVATE_KEY =
            "MIIEvgIBADANBgkqhkiG9w0BAQEFAASCBKgwggSkAgEAAoIBAQDHbbGLjf9j8PA8" +
            "lzNNeOzx9AU50fk+a00K15okwAbXjBvctl3nkQSy0nqUrL0upM7LCWyIgUxYO86z" +
            "nJTEV5qTk7Anz/zjF9uUIvJBnsIeeQFy9enHeMz3fiOt2A/5fYZ4aPdALy79Cze+" +
            "UVo6gAH+T+wubXlU7eP1vhunQTvqOhY6HBCJ4F7n9Bxm9dSF8v7kMag71E56M/f4" +
            "z1Mzvgv5x5Ck3/ybvWVQOJM6TXdSFyqUUPthLFYjMarqZodrzPngv5m6KzqO4eyB" +
            "AcBJmxiE3ufKdbf4oFI2uzzn3H9ZI+bL/NjCHTxbyWbiR2NUTlNfD80OMWeeDC/X" +
            "q+16OHyDAgMBAAECggEBAKOXWA0ibl2NR4Rsg6kJiVTw11iW5d5OJuS997Qt0W7/" +
            "f9uNvXo3e6M1BVjwWj/o8bmcAWv4pKe8Z9LunxpwwlxMyjPeaZPf/j+GazNpB9P3" +
            "bzjegOcgMQLUdnAkzPXcAnLDqA7+pYztpsx374wNdZUn+pYbN2xzuIvdZtHMsVlw" +
            "2ZSd/ZU/e3++jpDAqgdpul6TdMGcSxqjYyuwPWQhYx00qBIJNZig9phQKACxNYxR" +
            "yyqdRv3t/csFhK17qnFgVf424nXFbg7ELoLpCujkwXkMQWkr8Z44DtKNJspJlST9" +
            "zoVaTRhV5p7tXged/JUt8FG5nvgu4snlnMhn/K1n0AECgYEA7iK0sJaCwLtWnB46" +
            "diUOkhANHrAe+XTXl1nwQQkD7vUHyrlukJDliEh1NhmJMRxPQbHf9RrUhLhgLBRg" +
            "2ge67w6kGj2Agyx3WF2oblbD2OAHCe2Rrs5fF69mqMpvbqZZIWD+yFiBJp79bin5" +
            "jN+uOhiABh1yfw/EjV0eU+XWyA8CgYEA1mOiaXWDVB7t495MJ8mi5I+BZcnMbvgI" +
            "4hKWpZMdIGCsjEmvQJrjXzIXyATpznNOzrEup6tFjX0WmgWQOuQE5wSi9iGoaZLw" +
            "YdGPgH3j2MwXlplSiWviibdZfIli28C4i3+FmGZlO5THHB/xK4uVtezDDMxpwFyQ" +
            "SeDuL2ussE0CgYEA0uQLbwOsAfEmb5XZoj2JHNN4OwAwPi06rH/q5D2OrTV01BTK" +
            "FN8tVzcMDoAo3kQ68GwNcWx0XqFGEmNtrkkARKuLqu1ifUiI3Mn82tKeGNe1hBZP" +
            "WSbMUhZ07PByJOTOtF/I4zZ2EfTlbYVgymBhVHPUFRZJCru1DpgzvosiXgMCgYBV" +
            "sAjwAan1607FrsnddTgIBlt/pYJyL+zM/wT7NKuFj14nzCOhvMZ3+/uJVH1mqKus" +
            "7SBqn4fzHzXzZZnaD9ztwOqpWZaIa9RsJGgowShaNGiRJsLYbihjRscbgYXjs0mP" +
            "Z+6rlPGNOM/EK/gmoWm7BuCGswTpf5WkEaThizXAWQKBgCgV3hc6wDbD84h6XGFF" +
            "cNPJ1fiBdZflQ5P21QRGTe5tsIJPnZ3qj2JSsY4GODCfkzBOK+7VzGMCAi7fWlUt" +
            "drUEPaOCDY7/9JkKgu8uPDYsUb22Q1BZi2vfbsfXytkna8bi4cXKJkeoNEKimJni" +
            "jZIzPiAGS5/h4W+2PcfvygvD";

    @Override
    protected void configure() {
        final KeyStore keyStore;
        try {
            keyStore = loadSelfSignedKeyStore();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        bind(KeyStore.class).toInstance(keyStore);
        Multibinder.newSetBinder(binder(), JettyConnectorProvider.class).addBinding()
                .toInstance(new SslJettyConnectorProvider(keyStore));
    }

    private static final class SslJettyConnectorProvider implements JettyConnectorProvider {

        private final KeyStore keyStore;

        private SslJettyConnectorProvider(KeyStore keyStore) {
            this.keyStore = keyStore;
        }

        @Override
        public Connector getConnector(Server server) {
            try {
                SSLContext sslContext = SSLContext.getInstance("TLS");

                KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
                kmf.init(keyStore, "password".toCharArray());
                sslContext.init(kmf.getKeyManagers(), null, null);

                SslContextFactory sslContextFactory = new SslContextFactory();
                sslContextFactory.setSslContext(sslContext);

                ServerConnector serverConnector = new ServerConnector(server, sslContextFactory);
                // In a real module, this would be configurable; for tests always use an ephemeral port
                serverConnector.setPort(0);
                return serverConnector;
            } catch (RuntimeException e) {
                throw e;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    private static KeyStore loadSelfSignedKeyStore() throws Exception {
        KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
        keyStore.load(null, null);

        X509Certificate[] chain = new X509Certificate[1];
        try (InputStream inputStream = new ByteArrayInputStream(Base64.decode(CERTIFICATE))) {
            chain[0] = (X509Certificate) CertificateFactory.getInstance("X509").generateCertificate(inputStream);
        }
        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(Base64.decode(PRIVATE_KEY));
        KeyFactory kf = KeyFactory.getInstance("RSA");
        PrivateKey privateKey = kf.generatePrivate(spec);

        keyStore.setKeyEntry("1", privateKey, "password".toCharArray(), chain);

        return keyStore;
    }
}
