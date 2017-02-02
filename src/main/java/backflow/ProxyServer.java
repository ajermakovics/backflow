package backflow;

import io.undertow.Handlers;
import io.undertow.Undertow;
import io.undertow.server.HttpHandler;
import io.undertow.server.handlers.ResponseCodeHandler;
import io.undertow.server.handlers.proxy.LoadBalancingProxyClient;
import io.undertow.server.handlers.proxy.ProxyHandler;
import org.andrejs.json.Json;
import org.jboss.logging.Logger;
import org.xnio.Options;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyStore;

import static backflow.ProxyConfig.loadConfig;

public class ProxyServer {

    private static final Logger log = Logger.getLogger(ProxyServer.class.getSimpleName());
    private Undertow undertow;

    public static void main(final String[] args) throws Exception {
        new ProxyServer().start();
    }

    public void start() throws Exception {

        Json config = loadConfig();

        Json serverConfig = config.at("server");
        String host = serverConfig.get("host", "0.0.0.0");
        String backends = serverConfig.get("backends", "");
        int port = serverConfig.get("port", 8000);
        int maxRequestTime = serverConfig.get("maxRequestTime", 30000);
        int connectionsPerThread = serverConfig.get("connectionsPerThread", 20);
        int ioThreads = serverConfig.get("ioThread", 4);
        int workerThreads = serverConfig.get("workerThreads", Runtime.getRuntime().availableProcessors() * 8);
        int workerTaskMaxThreads = serverConfig.get("workerTaskMaxThreads", workerThreads);
        int backlog = serverConfig.get("backlog", 1000);
        boolean rewriteHostHeader =serverConfig.get("rewriteHostHeader", false);
        boolean reuseXForwarded = serverConfig.get("reuseXForwarded", true);

        LoadBalancingProxyClient loadBalancer = new LoadBalancingProxyClient()
                    .setConnectionsPerThread(connectionsPerThread);

        HttpHandler loadBalancerApi = new BalancerHandler(loadBalancer, backends.split("\\s+"));
        ProxyHandler proxyHandler = new ProxyHandler(loadBalancer, maxRequestTime, ResponseCodeHandler.HANDLE_404, rewriteHostHeader, reuseXForwarded);

        HttpHandler handler = Handlers.path()
                .addPrefixPath("/lb", loadBalancerApi)
                .addPrefixPath("/", proxyHandler);

        Undertow.Builder proxyBuilder = Undertow.builder()
                .addHttpListener(port, host)
                .setIoThreads(ioThreads)
                .setWorkerThreads(workerThreads)
                .setWorkerOption(Options.WORKER_TASK_MAX_THREADS, workerTaskMaxThreads)
                .setSocketOption(Options.BACKLOG, backlog)
                .setHandler(handler);

        if(serverConfig.containsKey("sslPort")) {
            int sslPort = Integer.parseInt(serverConfig.get("sslPort", "443"));
            String keystorePassword = serverConfig.get("keystorePassword", "");
            KeyStore keyStore = loadKeyStore(serverConfig.get("keystore", ""), keystorePassword);
            SSLContext sslContext = newSslContext(keyStore, keystorePassword);
            proxyBuilder.addHttpsListener(sslPort, host, sslContext);
        }

        this.undertow = proxyBuilder.build();
        undertow.start();
        log.info("Started proxy on " + host + ":" + port);
    }

    public void stop() {
        undertow.stop();
    }

    private static SSLContext newSslContext(final KeyStore keyStore, String keyStorePw) throws Exception {
        KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        keyManagerFactory.init(keyStore, keyStorePw.toCharArray());
        KeyManager[] keyManagers = keyManagerFactory.getKeyManagers();

        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(keyManagers, new TrustManager[]{}, null);

        return sslContext;
    }

    private static KeyStore loadKeyStore(String storeLoc, String storePw) throws Exception {
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
