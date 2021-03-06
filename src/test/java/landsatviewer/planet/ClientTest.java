package landsatviewer.planet;

import java.io.InputStream;
import java.util.Scanner;

import org.junit.Before;
import org.junit.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.spy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.anything;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

public class ClientTest {
    private MockRestServiceServer server;
    private RestTemplate restTemplate;

    @Before
    public void setUp() {
        restTemplate = spy(RestTemplate.class);
        server = MockRestServiceServer.createServer(restTemplate);
    }

    @Test
    public void fetchTile_callsCorrectURL() throws Exception {
        server
                .expect(requestTo("https://tiles.planet.com/data/v1/Landsat8L1G/test-scene-id/56/12/34.png"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("test-data".getBytes(), MediaType.IMAGE_PNG));

        createClient().fetchTile("test-scene-id", 12, 34, 56);

        server.verify();
    }

    @Test
    public void fetchTile_returnsValidStream() throws Exception {
        server
                .expect(anything())
                .andRespond(withSuccess("test-data".getBytes(), MediaType.IMAGE_PNG));

        final InputStream inputStream = createClient().fetchTile("test-scene-id", 12, 34, 56);

        assertEquals("test-data", new Scanner(inputStream).next());
    }

    @Test(expected = Client.Error.class)
    public void fetchTile_gracefullyHandlesPlanetAPIError() throws Exception {
        server
                .expect(requestTo("https://tiles.planet.com/data/v1/Landsat8L1G/test-scene-id/56/12/34.png"))
                .andRespond(withServerError());

        createClient().fetchTile("test-scene-id", 12, 34, 56);

        server.verify();
    }

    @Test
    public void getScene_callsCorrectURL() throws Exception {
        server
                .expect(requestTo("https://api.planet.com/data/v1/item-types/Landsat8L1G/items/test-scene-id"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

        createClient().getScene("test-scene-id");

        server.verify();
    }

    @Test(expected = Client.Error.class)
    public void getScene_gracefullyHandlesPlanetAPIError() throws Exception {
        server
                .expect(anything())
                .andRespond(withServerError());

        createClient().getScene("test-scene-id");

        server.verify();
    }

    @Test
    public void search_callsCorrectURL() throws Exception {
        server
                .expect(requestTo("https://api.planet.com/data/v1/quick-search"))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

        createClient().search(12.34, 45.56, 789);

        server.verify();
    }

    @Test
    public void search_sendsCorrectHeaders() throws Exception {
        server
                .expect(header("Content-Type", "application/json"))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

        createClient().search(12.34, 45.56, 789);

        server.verify();
    }

    @Test
    public void search_sendsCorrectCriteria() throws Exception {
        server
                .expect(jsonPath("item_types").value("Landsat8L1G"))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

        createClient().search(12.34, 45.56, 789);

        server.verify();
    }

    @Test(expected = Client.Error.class)
    public void search_gracefullyHandlesPlanetAPIError() throws Exception {
        server
                .expect(anything())
                .andRespond(withServerError());

        createClient().search(12.34, 45.56, 789);

        server.verify();
    }

    private Client createClient() {
        return new Client(restTemplate);
    }
}
