package landsatviewer;

import landsatviewer.planet.Client;
import org.junit.Before;
import org.junit.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.io.ByteArrayInputStream;
import java.util.Map;

import static org.mockito.Mockito.*;
import static junit.framework.TestCase.*;

public class ApplicationTest {
    private Client client;

    @Before
    public void setUp() {
        client = mock(Client.class);
    }


    @Test
    public void healthCheck_responseIncludesUptime() {
        Map<String, Double> response = createApplication().healthCheck();

        assertTrue(response.containsKey("uptime"));
        assertNotNull(response.get("uptime"));
    }


    @Test
    public void search_requestsCorrectX() throws Client.Error {
        double n = Math.random();

        createApplication().search(n, 34.0, 56);

        verify(client).search(eq(n), anyDouble(), anyInt());
    }

    @Test
    public void search_requestsCorrectY() throws Client.Error {
        double n = Math.random();

        createApplication().search(12.0, n, 56);

        verify(client).search(anyDouble(), eq(n), anyInt());
    }

    @Test
    public void search_requestsCorrectNumberOfDays() throws Client.Error {
        int n = (int)(Math.random() * 1000);

        createApplication().search(12.0, 34.0, n);

        verify(client).search(anyDouble(), anyDouble(), eq(n));
    }

    @Test
    public void search_doesntRequestInvalidX() throws Client.Error {
        createApplication().search(null, 34.0, 56);

        verify(client, never()).search(anyDouble(), anyDouble(), anyInt());
    }

    @Test
    public void search_rejectsInvalidX() throws Client.Error {
        ResponseEntity search = createApplication().search(null, 34.0, 56);

        assertEquals(search.getStatusCode(), HttpStatus.BAD_REQUEST);
    }

    @Test
    public void search_rejectsInvalidY() throws Client.Error {
        ResponseEntity search = createApplication().search(12.0, null, 56);

        assertEquals(search.getStatusCode(), HttpStatus.BAD_REQUEST);
    }

    @Test
    public void search_doesntRequestInvalidY() throws Client.Error {
        createApplication().search(12.0, null, 56);

        verify(client, never()).search(anyDouble(), anyDouble(), anyInt());
    }

    @Test
    public void search_gracefullyHandlesSearchFailure() throws Client.Error {
        when(client.search(anyDouble(), anyDouble(), anyInt())).thenThrow(Client.Error.class);

        ResponseEntity response = createApplication().search(12.0, 34.0, 56);

        assertEquals(response.getStatusCode(), HttpStatus.INTERNAL_SERVER_ERROR);
    }


    @Test
    public void getScene_requestsCorrectSceneId() throws Client.Error {
        createApplication().getScene("test-scene-id");

        verify(client).getScene("test-scene-id");
    }

    @Test
    public void getScene_gracefullyHandlesSceneNotFound() throws Client.Error {
        when(client.getScene("test-scene-id")).thenThrow(Client.NotFound.class);

        ResponseEntity response = createApplication().getScene("test-scene-id");

        assertEquals(response.getStatusCode(), HttpStatus.NOT_FOUND);
    }

    @Test
    public void getScene_gracefullyHandlesRetrievalFailure() throws Client.Error {
        when(client.getScene("test-scene-id")).thenThrow(Client.Error.class);

        ResponseEntity response = createApplication().getScene("test-scene-id");

        assertEquals(response.getStatusCode(), HttpStatus.INTERNAL_SERVER_ERROR);
    }


    @Test
    public void tile_requestsCorrectSceneId() throws Client.Error {
        when(client.fetchTile(anyString(), anyInt(), anyInt(), anyInt()))
                .thenReturn(new ByteArrayInputStream("test-data".getBytes()));

        createApplication().tile("test-scene-id", 123, 456, 789);

        verify(client).fetchTile(eq("test-scene-id"), anyInt(), anyInt(), anyInt());
    }

    @Test
    public void tile_requestsCorrectX() throws Client.Error {
        when(client.fetchTile(anyString(), anyInt(), anyInt(), anyInt()))
                .thenReturn(new ByteArrayInputStream("test-data".getBytes()));

        createApplication().tile("test-scene-id", 123, 456, 789);

        verify(client).fetchTile(anyString(), eq(123), anyInt(), anyInt());
    }

    @Test
    public void tile_requestsCorrectY() throws Client.Error {
        when(client.fetchTile(anyString(), anyInt(), anyInt(), anyInt()))
                .thenReturn(new ByteArrayInputStream("test-data".getBytes()));

        createApplication().tile("test-scene-id", 123, 456, 789);

        verify(client).fetchTile(anyString(), anyInt(), eq(456), anyInt());
    }

    @Test
    public void tile_requestsCorrectZ() throws Client.Error {
        when(client.fetchTile(anyString(), anyInt(), anyInt(), anyInt()))
                .thenReturn(new ByteArrayInputStream("test-data".getBytes()));

        createApplication().tile("test-scene-id", 123, 456, 789);

        verify(client).fetchTile(anyString(), anyInt(), anyInt(), eq(789));
    }

    @Test
    public void tile_gracefullyHandlesProxyError() throws Client.Error {
        when(client.fetchTile(anyString(), anyInt(), anyInt(), anyInt())).thenThrow(Client.Error.class);

        ResponseEntity response = createApplication().tile("test-scene-id", 123, 456, 789);

        assertEquals(response.getStatusCode(), HttpStatus.INTERNAL_SERVER_ERROR);
    }


    private Application createApplication() {
        return new Application(client);
    }
}
