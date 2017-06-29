package landsatviewer;

import landsatviewer.planet.Client;
import org.junit.Before;
import org.junit.Test;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import javax.servlet.ServletContext;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import static junit.framework.TestCase.*;
import static org.mockito.Mockito.*;

public class PlanetControllerTest {
    private Client client;
    private ServletContext servletContext;

    @Before
    public void setUp() {
        client = mock(Client.class);
        servletContext = mock(ServletContext.class);
    }


    @Test
    public void healthCheck_responseIncludesUptime() {
        Map<String, Double> response = createController().healthCheck();

        assertTrue(response.containsKey("uptime"));
        assertNotNull(response.get("uptime"));
    }


    @Test
    public void search_requestsCorrectX() throws Client.Error {
        double n = Math.random();

        createController().search(n, 34.0, 56);

        verify(client).search(eq(n), anyDouble(), anyInt());
    }

    @Test
    public void search_requestsCorrectY() throws Client.Error {
        double n = Math.random();

        createController().search(12.0, n, 56);

        verify(client).search(anyDouble(), eq(n), anyInt());
    }

    @Test
    public void search_requestsCorrectNumberOfDays() throws Client.Error {
        int n = (int)(Math.random() * 1000);

        createController().search(12.0, 34.0, n);

        verify(client).search(anyDouble(), anyDouble(), eq(n));
    }

    @Test
    public void search_doesntRequestInvalidX() throws Client.Error {
        createController().search(null, 34.0, 56);

        verify(client, never()).search(anyDouble(), anyDouble(), anyInt());
    }

    @Test
    public void search_rejectsInvalidX() throws Client.Error {
        ResponseEntity response = createController().search(null, 34.0, 56);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    public void search_rejectsInvalidY() throws Client.Error {
        ResponseEntity response = createController().search(12.0, null, 56);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    public void search_doesntRequestInvalidY() throws Client.Error {
        createController().search(12.0, null, 56);

        verify(client, never()).search(anyDouble(), anyDouble(), anyInt());
    }

    @Test
    public void search_gracefullyHandlesSearchFailure() throws Client.Error {
        when(client.search(anyDouble(), anyDouble(), anyInt())).thenThrow(Client.Error.class);

        ResponseEntity response = createController().search(12.0, 34.0, 56);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
    }


    @Test
    public void getScene_requestsCorrectSceneId() throws Client.Error {
        createController().getScene("test-scene-id");

        verify(client).getScene("test-scene-id");
    }

    @Test
    public void getScene_gracefullyHandlesSceneNotFound() throws Client.Error {
        when(client.getScene("test-scene-id")).thenThrow(Client.NotFound.class);

        ResponseEntity response = createController().getScene("test-scene-id");

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    public void getScene_gracefullyHandlesRetrievalFailure() throws Client.Error {
        when(client.getScene("test-scene-id")).thenThrow(Client.Error.class);

        ResponseEntity response = createController().getScene("test-scene-id");

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
    }


    @Test
    public void tiles_requestsCorrectSceneId() throws Client.Error {
        when(client.fetchTile(anyString(), anyInt(), anyInt(), anyInt()))
                .thenReturn(new ByteArrayInputStream("test-data".getBytes()));

        createController().tiles("test-scene-id", 123, 456, 789);

        verify(client).fetchTile(eq("test-scene-id"), anyInt(), anyInt(), anyInt());
    }

    @Test
    public void tiles_requestsCorrectX() throws Client.Error {
        when(client.fetchTile(anyString(), anyInt(), anyInt(), anyInt()))
                .thenReturn(new ByteArrayInputStream("test-data".getBytes()));

        createController().tiles("test-scene-id", 123, 456, 789);

        verify(client).fetchTile(anyString(), eq(123), anyInt(), anyInt());
    }

    @Test
    public void tiles_requestsCorrectY() throws Client.Error {
        when(client.fetchTile(anyString(), anyInt(), anyInt(), anyInt()))
                .thenReturn(new ByteArrayInputStream("test-data".getBytes()));

        createController().tiles("test-scene-id", 123, 456, 789);

        verify(client).fetchTile(anyString(), anyInt(), eq(456), anyInt());
    }

    @Test
    public void tiles_requestsCorrectZ() throws Client.Error {
        when(client.fetchTile(anyString(), anyInt(), anyInt(), anyInt()))
                .thenReturn(new ByteArrayInputStream("test-data".getBytes()));

        createController().tiles("test-scene-id", 123, 456, 789);

        verify(client).fetchTile(anyString(), anyInt(), anyInt(), eq(789));
    }

    @Test
    public void tiles_gracefullyHandlesProxyError() throws Client.Error {
        when(servletContext.getResourceAsStream(anyString()))
                .thenReturn(new ByteArrayInputStream("test-data".getBytes()));
        when(client.fetchTile(anyString(), anyInt(), anyInt(), anyInt()))
                .thenThrow(Client.Error.class);

        ResponseEntity<InputStreamResource> response = createController().tiles("test-scene-id", 123, 456, 789);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
    }

    @Test
    public void tiles_rendersPlaceholderOnError() throws Client.Error, IOException {
        InputStream expectedStream = new ByteArrayInputStream("test-data".getBytes());
        when(servletContext.getResourceAsStream(anyString())).thenReturn(expectedStream);
        when(client.fetchTile(anyString(), anyInt(), anyInt(), anyInt())).thenThrow(Client.Error.class);

        ResponseEntity<InputStreamResource> response = createController().tiles("test-scene-id", 123, 456, 789);

        assertSame(response.getBody().getInputStream(), expectedStream);
        assertEquals(MediaType.IMAGE_PNG, response.getHeaders().getContentType());
        verify(servletContext).getResourceAsStream(eq("/tile-error.png"));
    }

    private PlanetController createController() {
        return new PlanetController(client, servletContext);
    }
}
