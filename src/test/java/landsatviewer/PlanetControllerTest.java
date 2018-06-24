package landsatviewer;

import java.io.ByteArrayInputStream;
import javax.servlet.ServletContext;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.any;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.mockito.Mockito.anyDouble;
import static org.mockito.Mockito.anyInt;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import landsatviewer.planet.Client;

@RunWith(SpringJUnit4ClassRunner.class)
@WebMvcTest(PlanetController.class)
public class PlanetControllerTest {
    @MockBean
    private Client client;

    @MockBean
    private ServletContext servletContext;

    @Autowired
    private MockMvc mvc;

    @Test
    public void healthCheck__ResponseIncludesUptime() throws Exception {
        mvc.perform(get("/"))
                .andExpect(jsonPath("uptime", greaterThan(0.0)));
    }


    @Test
    public void search__RequestsCorrectCoordinates() throws Exception {
        double expectedX = Math.random() * 180;
        double expectedY = Math.random() * 90;

        mvc.perform(get("/scenes?x={x}&y={y}&days_ago=56", expectedX, expectedY));

        verify(client).search(eq(expectedX), eq(expectedY), anyInt());
    }

    @Test
    public void search__RequestsCorrectNumberOfDays() throws Exception {
        int expectedDays = (int) (Math.random() * 1000);

        mvc.perform(get("/scenes?x=12&y=34&days_ago={days_ago}", expectedDays));

        verify(client).search(anyDouble(), anyDouble(), eq(expectedDays));
    }

    @Test
    public void search__RequestsCorrectFallbackForNumberOfDays() throws Exception {
        mvc.perform(get("/scenes?x=12&y=34"));

        verify(client).search(anyDouble(), anyDouble(), anyInt());
    }

    @Test
    public void search__RejectsInvalidX() throws Exception {
        mvc.perform(get("/scenes?x=&y=34&days_ago=56"))
                .andExpect(status().is(400))
                .andExpect(jsonPath("error", any(String.class)));
    }

    @Test
    public void search__DoesntRequestInvalidX() throws Exception {
        mvc.perform(get("/scenes?x=&y=34&days_ago=56"));

        verify(client, never()).search(anyDouble(), anyDouble(), anyInt());
    }

    @Test
    public void search__RejectsInvalidY() throws Exception {
        mvc.perform(get("/scenes?x=12&y=&days_ago=56"))
                .andExpect(status().is(400))
                .andExpect(jsonPath("error", any(String.class)));
    }

    @Test
    public void search__DoesntRequestInvalidY() throws Exception {
        mvc.perform(get("/scenes?x=12&y=&days_ago=56"));

        verify(client, never()).search(anyDouble(), anyDouble(), anyInt());
    }

    @Test
    public void search__GracefullyHandlesSearchFailure() throws Exception {
        when(client.search(anyDouble(), anyDouble(), anyInt()))
                .thenThrow(new Client.Error("test-error"));

        mvc.perform(get("/scenes?x=12&y=34&days_ago=56"))
                .andExpect(status().is(500))
                .andExpect(jsonPath("error", equalTo("Search error: test-error")));
    }


    @Test
    public void getScene__RequestsCorrectSceneId() throws Exception {
        mvc.perform(get("/scenes/test-scene-id"));

        verify(client).getScene("test-scene-id");
    }

    @Test
    public void getScene__GracefullyHandlesSceneNotFound() throws Exception {
        when(client.getScene("test-scene-id"))
                .thenThrow(Client.NotFound.class);

        mvc.perform(get("/scenes/test-scene-id"))
                .andExpect(status().is(404))
                .andExpect(jsonPath("error", equalTo("Scene 'test-scene-id' not found")));
    }

    @Test
    public void getScene__GracefullyHandlesRetrievalFailure() throws Exception {
        when(client.getScene("test-scene-id"))
                .thenThrow(new Client.Error("test-error"));

        mvc.perform(get("/scenes/test-scene-id"))
                .andExpect(status().is(500))
                .andExpect(jsonPath("error", equalTo("Scene fetch error: test-error")));
    }


    @Test
    public void tiles__RequestsCorrectSceneId() throws Exception {
        when(client.fetchTile(anyString(), anyInt(), anyInt(), anyInt()))
                .thenReturn(new ByteArrayInputStream("test-data".getBytes()));

        mvc.perform(get("/tiles/test-scene-id/789/123/456.png"));

        verify(client).fetchTile(eq("test-scene-id"), anyInt(), anyInt(), anyInt());
    }

    @Test
    public void tiles__RequestsCorrectX() throws Exception {
        when(client.fetchTile(anyString(), anyInt(), anyInt(), anyInt()))
                .thenReturn(new ByteArrayInputStream("test-data".getBytes()));

        mvc.perform(get("/tiles/test-scene-id/789/123/456.png"));

        verify(client).fetchTile(anyString(), eq(123), anyInt(), anyInt());
    }

    @Test
    public void tiles__RequestsCorrectY() throws Exception {
        when(client.fetchTile(anyString(), anyInt(), anyInt(), anyInt()))
                .thenReturn(new ByteArrayInputStream("test-data".getBytes()));

        mvc.perform(get("/tiles/test-scene-id/789/123/456.png"));

        verify(client).fetchTile(anyString(), anyInt(), eq(456), anyInt());
    }

    @Test
    public void tiles__RequestsCorrectZ() throws Exception {
        when(client.fetchTile(anyString(), anyInt(), anyInt(), anyInt()))
                .thenReturn(new ByteArrayInputStream("test-data".getBytes()));

        mvc.perform(get("/tiles/test-scene-id/789/123/456.png"));

        verify(client).fetchTile(anyString(), anyInt(), anyInt(), eq(789));
    }

    @Test
    public void tiles__GracefullyHandlesProxyError() throws Exception {
        when(client.fetchTile(anyString(), anyInt(), anyInt(), anyInt()))
                .thenThrow(Client.Error.class);
        when(servletContext.getResourceAsStream(anyString()))
                .thenReturn(new ByteArrayInputStream("test-data".getBytes()));

        mvc.perform(get("/tiles/test-scene-id/123/456/789.png"))
                .andExpect(status().is(500));
    }

    @Test
    public void tiles__RendersPlaceholderOnError() throws Exception {
        when(client.fetchTile(anyString(), anyInt(), anyInt(), anyInt()))
                .thenThrow(Client.Error.class);
        when(servletContext.getResourceAsStream(eq("/tile-error.png")))
                .thenReturn(new ByteArrayInputStream("test-data".getBytes()));

        mvc.perform(get("/tiles/test-scene-id/123/456/789.png"))
                .andExpect(content().contentType(MediaType.IMAGE_PNG))
                .andExpect(content().bytes("test-data".getBytes()));
    }
}
