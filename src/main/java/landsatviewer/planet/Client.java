package landsatviewer.planet;

import java.io.IOException;
import java.io.InputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Component
public class Client {
    private static final Logger logger = LoggerFactory.getLogger(Client.class);

    private static final String TILE_URL = "https://tiles.planet.com/data/v1/Landsat8L1G/{sceneId}/{z}/{x}/{y}.png";
    private static final String SCENE_URL = "https://api.planet.com/data/v1/item-types/Landsat8L1G/items/{sceneId}";
    private static final String SEARCH_URL = "https://api.planet.com/data/v1/quick-search";

    private final RestTemplate restTemplate;

    @Autowired
    public Client(@Value("${PLANET_API_KEY}") String apiKey) {
        this.restTemplate = new RestTemplateBuilder()
                .basicAuthorization(apiKey, "")
                .build();
    }

    public Client(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public InputStream fetchTile(String sceneId, int x, int y, int z) throws Error {
        logger.debug("Request tile (sceneId={}, x={}, y={}, z={})", sceneId, x, y, z);

        final ResponseEntity<ByteArrayResource> response;
        try {
            response = restTemplate.getForEntity(TILE_URL, ByteArrayResource.class, sceneId, z, x, y);
        }
        catch (RestClientException e) {
            logger.error("Could not communicate with Planet API: {}", e.getMessage(), e);
            throw new Error(e);
        }

        final int status = response.getStatusCodeValue();
        if (status != 200) {
            logger.error("Tile request failed (sceneId={}, x={}, y={}, z={})", sceneId, x, y, z);
            throw new Error("Planet returned HTTP %s", status);
        }

        final InputStream stream;
        try {
            stream = response.getBody().getInputStream();
        }
        catch (IOException e) {
            logger.error("Tile request failed: {} (sceneId={}, x={}, y={}, z={})", e.getMessage(), sceneId, x, y, z, e);
            throw new Error("could not get stream from response");
        }

        return stream;
    }

    public Scene getScene(String sceneId) throws Error {
        logger.debug("Request metadata for scene '{}'", sceneId);

        final ResponseEntity<Scene> response;
        try {
            response = restTemplate.getForEntity(SCENE_URL, Scene.class, sceneId);
        }
        catch (RestClientException e) {
            logger.error("Could not communicate with Planet API: {}", e.getMessage(), e);
            throw new Error(e);
        }

        final int status = response.getStatusCodeValue();
        if (status != 200) {
            if (status == 404) {
                throw new NotFound();
            }
            logger.error("Scene metadata request failed for '{}': Planet returned HTTP {}", sceneId, status);
            throw new Error("Planet returned HTTP %s", status);
        }

        return response.getBody();
    }

    public SceneCollection search(double x, double y, int daysSince) throws Error {
        final HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        final HttpEntity<SearchCriteria> entity = new HttpEntity<>(new SearchCriteria(x, y, daysSince), headers);

        final ResponseEntity<SceneCollection> response;
        try {
            response = restTemplate.postForEntity(SEARCH_URL, entity, SceneCollection.class);
        }
        catch (RestClientException e) {
            logger.error("Could not communicate with Planet API: {}", e.getMessage(), e);
            throw new Error(e);
        }

        final int status = response.getStatusCodeValue();
        if (status != 200) {
            logger.error("Search failed: Planet returned HTTP {}", status);
            throw new Error("Planet returned HTTP %s", status);
        }

        return response.getBody();
    }

    public static class Error extends Exception {
        Error(String message) {
            super(message);
        }

        Error(String message, Object... args) {
            super(String.format(message, args));
        }

        Error(Throwable e) {
            super(e);
        }
    }

    public static class NotFound extends Error {
        NotFound() {
            super("scene not found");
        }

    }
}
