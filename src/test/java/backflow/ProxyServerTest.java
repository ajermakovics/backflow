package backflow;


import io.restassured.specification.RequestSpecification;
import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static backflow.ProxyConfig.loadConfig;
import static io.restassured.RestAssured.given;
import static io.restassured.RestAssured.when;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.hasItem;

public class ProxyServerTest  {

    ProxyServer proxyServer = new ProxyServer(loadConfig());

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
        givenAuth().
        when().
                get("/lb").
        then().
                statusCode(200).
                body("backends", hasItem("http://127.0.0.1:9090"));
    }

    @Test
    public void postAddsNewBackend() {
        givenAuth().
                body("http://localhost:9000").post("/lb");

        givenAuth().
        when().
                get("/lb").
        then().
                statusCode(200).
                body("backends", hasItem("http://localhost:9000"));
;
    }

    @Test
    public void deleteRemovesBackend() {
        givenAuth().
                body("http://127.0.0.1:9090").delete("/lb");

        givenAuth().
        when().
                get("/lb").
        then().
                statusCode(200).
                body("backends", Matchers.empty());
    }

    @Test
    public void failesWithoutAuthentication() {
        when().
                get("/lb").
        then().
                statusCode(401).
                body(equalTo("Unauthorized"));
    }

    private RequestSpecification givenAuth() {
        return given().auth().preemptive().basic("test", "secret");
    }
}
