package backflow;

import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;

import java.util.Base64;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class BasicAuthHandler implements HttpHandler {

    private final HttpHandler next;
    private final Set<String> authentications;

    public BasicAuthHandler(Map<String, Object> users, HttpHandler next) {

        this.next = next;
        authentications = users.entrySet().stream()
                .map(e -> (e.getKey() + ":" + e.getValue()).getBytes())
                .map(Base64.getEncoder()::encode)
                .map(auth -> "Basic " + new String(auth))
                .collect(Collectors.toSet());
    }

    @Override
    public void handleRequest(HttpServerExchange req) throws Exception {
        String auth = req.getRequestHeaders().getFirst("Authorization");

        if(authentications.contains(auth)) {
            next.handleRequest(req);
        } else {
            req.setStatusCode(401);
            req.getResponseSender().send("Unauthorized");
        }
    }
}
