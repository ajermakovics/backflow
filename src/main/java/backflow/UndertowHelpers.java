package backflow;


import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyStore;

public class UndertowHelpers {

    public static SSLContext newSslContext(final KeyStore keyStore, String keyStorePw) throws Exception {
        KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        keyManagerFactory.init(keyStore, keyStorePw.toCharArray());
        KeyManager[] keyManagers = keyManagerFactory.getKeyManagers();

        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(keyManagers, new TrustManager[]{}, null);

        return sslContext;
    }

    public static KeyStore loadKeyStore(String storeLoc, String storePw) throws Exception {
        InputStream stream = Files.newInputStream(Paths.get(storeLoc));
        if(stream == null) {
            throw new IllegalArgumentException("Could not load keystore");
        }
        try(InputStream is = stream) {
            KeyStore loadedKeystore = KeyStore.getInstance("JKS");
            loadedKeystore.load(is, storePw.toCharArray());
            return loadedKeystore;
        }
    }
}
