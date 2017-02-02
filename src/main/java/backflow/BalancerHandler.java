package backflow;

import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.proxy.LoadBalancingProxyClient;
import io.undertow.util.Headers;
import org.andrejs.json.Json;
import org.jboss.logging.Logger;

import java.net.URI;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;


public class BalancerHandler implements HttpHandler {

    private static final Logger log = Logger.getLogger(BalancerHandler.class.getSimpleName());

    private LoadBalancingProxyClient lb;
    private Set<String> backends = new ConcurrentSkipListSet<>();

    public BalancerHandler(LoadBalancingProxyClient lb, String... backendHosts) {
        this.lb = lb;
        for (String backend : backendHosts) {
            addBackend(backend);
        }
    }

    private void addBackend(String backend) {
        try {
            lb.addHost(new URI(backend));
            backends.add(backend);
            log.info("Added load balancer backend: " + backend);
        } catch (Exception e) {
            log.error("Error adding backend: " + backend, e);
            throw new IllegalArgumentException(e);
        }
    }

    private void removeBackend(String backend) {
        try {
            if (backends.remove(backend)) {
                lb.removeHost(new URI(backend));
                log.info("Removed load balancer backend: " + backend);
            }
        } catch (Exception e) {
            log.error("Error removing backend: " + backend, e);
            throw new IllegalArgumentException(e);
        }
    }

    @Override
    public void handleRequest(HttpServerExchange req) throws Exception {
        req.getResponseHeaders().put(Headers.CONTENT_TYPE, "application/json");
        switch (req.getRequestMethod().toString()) {
            case "GET":
                req.getResponseSender()
                        .send(new Json("backends", backends).toString());
                break;
            case "POST":
                req.getRequestReceiver().receiveFullString((exch, body) -> {
                    addBackend(body);
                    req.getResponseSender().send(new Json("status", "ok").toString());
                });
                break;
            case "DELETE":
                req.getRequestReceiver().receiveFullString((exch, body) -> {
                    removeBackend(body);
                    req.getResponseSender().send(new Json("status", "ok").toString());
                });
                break;
            default:
                req.setStatusCode(405);
                req.getResponseSender().send("error");
        }
    }
}
