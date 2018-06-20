package landsatviewer;

import javax.servlet.ServletContext;
import java.io.InputStream;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import landsatviewer.planet.Client;

@RestController
class PlanetController {
    private static final Logger logger = LoggerFactory.getLogger(PlanetController.class);

    private static final int CACHE_LONG = 86400;
    private static final int CACHE_SHORT = 300;
    private static final Instant START_TIMESTAMP = Instant.now();

    private final Client client;
    private final ServletContext context;

    @Autowired
    PlanetController(Client client, ServletContext context) {
        this.client = client;
        this.context = context;
    }

    @GetMapping("/")
    Map<String, Double> healthCheck() {
        return Map.of("uptime", Duration.between(START_TIMESTAMP, Instant.now()).toMillis() / 1000D);
    }

    @GetMapping("/scenes")
    ResponseEntity search(@RequestParam(required = false) Double x,
                          @RequestParam(required = false) Double y,
                          @RequestParam(name = "days_ago", defaultValue = "14") int daysAgo) {
        if (x == null || y == null) {
            return createError(400, "Malformed input: missing 'x' and/or 'y' value");
        }

        try {
            return createCached(client.search(x, y, daysAgo), CACHE_SHORT);
        }
        catch (Client.Error err) {
            return createError("Search error: %s", err.getMessage());
        }
    }

    @GetMapping("/scenes/{id}")
    ResponseEntity getScene(@PathVariable String id) {
        try {
            return createCached(client.getScene(id), CACHE_LONG);
        }
        catch (Client.NotFound err) {
            return createError(404, "Scene '%s' not found", id);
        }
        catch (Client.Error err) {
            return createError("Scene fetch error: %s", err.getMessage());
        }
    }

    @GetMapping("/tiles/{sceneId}/{z}/{x}/{y}.png")
    ResponseEntity<InputStreamResource> tiles(@PathVariable String sceneId,
                                              @PathVariable int x,
                                              @PathVariable int y,
                                              @PathVariable int z) {
        InputStream stream;
        int status = 200;

        try {
            stream = client.fetchTile(sceneId, x, y, z);
        }
        catch (Client.Error err) {
            logger.error("Could not proxy tile request (scene={}, x={}, y={}, z={})", sceneId, x, y, z);
            status = 500;
            stream = context.getResourceAsStream("/tile-error.png");
        }
        return ResponseEntity
                .status(status)
                .contentType(MediaType.IMAGE_PNG)
                .cacheControl(CacheControl.maxAge(CACHE_LONG, TimeUnit.SECONDS))
                .body(new InputStreamResource(stream));
    }

    private ResponseEntity createCached(Object entity, int maxAge) {
        return ResponseEntity
                .ok()
                .cacheControl(CacheControl.maxAge(maxAge, TimeUnit.SECONDS))
                .body(entity);
    }

    private ResponseEntity<Map<String, String>> createError(String message, Object... args) {
        return createError(500, message, args);
    }

    private ResponseEntity<Map<String, String>> createError(int status, String message, Object... args) {
        return ResponseEntity
                .status(status)
                .body(Map.of("error", String.format(message, args)));
    }
}
