package landsatviewer.planet;

import com.mashape.unirest.http.Unirest;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.impl.DefaultHttpResponseFactory;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Scanner;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

public class ClientTest {

    private HttpClient transport;
    private HttpRequestBase request;
    private HttpResponse response;

    @Before
    public void setUp() throws IOException {
        transport = mock(HttpClient.class);
        response = new DefaultHttpResponseFactory().newHttpResponse(HttpVersion.HTTP_1_1, 200, null);

        when(transport.execute(any())).then(m -> {
            request = m.getArgument(0);
            return response;
        });

        Unirest.setHttpClient(transport);
    }

    @Test
    public void fetchTile_sendsCorrectAuthorization() throws Exception {
        Client client = new Client("test-api-key");

        client.fetchTile("test-scene-id", 12, 34, 56);

        assertEquals("Basic dGVzdC1hcGkta2V5Og==", request.getFirstHeader("Authorization").getValue());
    }

    @Test
    public void fetchTile_requestsCorrectURL() throws Exception {
        Client client = new Client("test-api-key");

        client.fetchTile("test-scene-id", 12, 34, 56);

        assertEquals(new URI("https://tiles.planet.com/data/v1/Landsat8L1G/test-scene-id/56/12/34.png"), request.getURI());
    }

    @Test(expected = Client.Error.class)
    public void fetchTile_gracefullyHandlesPlanetAPIError() throws Exception {
        response.setStatusCode(500);
        Client client = new Client("test-api-key");

        client.fetchTile("test-scene-id", 12, 34, 56);
    }

    @Test
    public void fetchTile_returnsBodyAsStream() throws Exception {
        HttpEntity entity = mock(HttpEntity.class);
        response.setEntity(entity);
        when(entity.getContent()).thenReturn(new ByteArrayInputStream("test-data".getBytes()));
        Client client = new Client("test-api-key");

        InputStream stream = client.fetchTile("test-scene-id", 12, 34, 56);

        assertEquals("test-data", new Scanner(stream).next());
    }


    @Test
    public void getScene_sendsCorrectAuthorization() throws Exception {
        Client client = new Client("test-api-key");

        client.getScene("test-scene-id");

        assertEquals("Basic dGVzdC1hcGkta2V5Og==", request.getFirstHeader("Authorization").getValue());
    }

    @Test
    public void getScene_requestsCorrectURL() throws Exception {
        Client client = new Client("test-api-key");

        client.getScene("test-scene-id");

        assertEquals(new URI("https://api.planet.com/data/v1/item-types/Landsat8L1G/items/test-scene-id"), request.getURI());
    }

    @Test(expected = Client.Error.class)
    public void getScene_gracefullyHandlesPlanetAPIError() throws Exception {
        Client client = new Client("test-api-key");

        response.setStatusCode(500);

        client.getScene("test-scene-id");
    }


    @Test
    public void search_sendsCorrectAuthorization() throws Exception {
        Client client = new Client("test-api-key");

        client.search(12.34, 45.56, 789);

        assertEquals("Basic dGVzdC1hcGkta2V5Og==", request.getFirstHeader("Authorization").getValue());
    }

    @Test
    public void search_requestsCorrectURL() throws Exception {
        Client client = new Client("test-api-key");

        client.search(12.34, 45.56, 789);

        assertEquals(new URI("https://api.planet.com/data/v1/quick-search"), request.getURI());
    }

    @Test(expected = Client.Error.class)
    public void search_gracefullyHandlesPlanetAPIError() throws Exception {
        Client client = new Client("test-api-key");

        response.setStatusCode(500);

        client.search(12.34, 45.56, 789);
    }
}