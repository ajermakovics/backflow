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

import javax.net.ssl.SSLContext;
import java.security.KeyStore;

import static backflow.ProxyConfig.loadConfig;

public class ProxyServer {

    private static final Logger log = Logger.getLogger(ProxyServer.class.getSimpleName());

    private Undertow undertow;
    private final Json config;

    public ProxyServer(Json config) {
        this.config = config;
    }

    public static void main(final String[] args) throws Exception {
        new ProxyServer(loadConfig()).start();
    }

    public void start() throws Exception {
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

        Json users = config.at("users");
        HttpHandler loadBalancerApi = new BasicAuthHandler(users, new BalancerHandler(loadBalancer, backends.split("\\s+")));
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
            KeyStore keyStore = UndertowHelpers.loadKeyStore(serverConfig.get("keystore", ""), keystorePassword);
            SSLContext sslContext = UndertowHelpers.newSslContext(keyStore, keystorePassword);
            proxyBuilder.addHttpsListener(sslPort, host, sslContext);
        }

        this.undertow = proxyBuilder.build();
        undertow.start();
        log.info("Started proxy on " + host + ":" + port);
    }

    public void stop() {
        undertow.stop();
    }

}
