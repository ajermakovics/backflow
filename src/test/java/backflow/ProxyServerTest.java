package backflow;


import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static io.restassured.RestAssured.given;
import static io.restassured.RestAssured.when;
import static org.hamcrest.CoreMatchers.hasItem;

public class ProxyServerTest  {

    ProxyServer proxyServer = new ProxyServer();

    @Before
    public void startProxy() throws Exception {
        proxyServer.start();
    }

    @After
    public void stopProxy() throws Exception {
        proxyServer.stop();
    }

    @Test
    public void returnsInitialBackend() {
        when().
                get("/lb").
        then().
                body("backends", hasItem("http://127.0.0.1:9090")).
                statusCode(200);
    }

    @Test
    public void postAddsNewBackend() {
        given().
                body("http://localhost:9000").post("/lb");

        when().
                get("/lb").
                then().
                body("backends", hasItem("http://localhost:9000")).
                statusCode(200);
    }

    @Test
    public void deleteRemovesBackend() {
        given().
                body("http://127.0.0.1:9090").delete("/lb");

        when().
                get("/lb").
        then().
                body("backends", Matchers.empty()).
                statusCode(200);
    }
}
