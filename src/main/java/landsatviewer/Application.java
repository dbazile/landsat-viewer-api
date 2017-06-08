package landsatviewer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.mashape.unirest.http.ObjectMapper;
import com.mashape.unirest.http.Unirest;
import landsatviewer.planet.Client;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Controller
@EnableAutoConfiguration
public class Application {
    private static final int CACHE_LONG = 86400;
    private static final int CACHE_SHORT = 300;
    private static final long START_TIMESTAMP = Instant.now().toEpochMilli();

    private Client client;

    public Application() {
        this(new Client(System.getenv("PLANET_API_KEY")));
        initializeUnirest();
    }

    public Application(Client client) {
        this.client = client;
    }

    @RequestMapping("/")
    @ResponseBody
    public Map<String, Double> healthCheck() {
        HashMap<String, Double> status = new HashMap<>();
        status.put("uptime", (Instant.now().toEpochMilli() - START_TIMESTAMP) / 1000.0);
        return status;
    }

    @RequestMapping(value = "/scenes")
    public ResponseEntity search(@RequestParam(required = false) Double x,
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

    @RequestMapping("/scenes/{id}")
    public ResponseEntity getScene(@PathVariable String id) {
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

    @RequestMapping("/tile/{sceneId}/{z}/{x}/{y}.png")
    public ResponseEntity tile(@PathVariable String sceneId,
                               @PathVariable int x,
                               @PathVariable int y,
                               @PathVariable int z) {
        try {
            InputStream stream = client.fetchTile(sceneId, x, y, z);
            return createCached(new InputStreamResource(stream), CACHE_LONG);
        }
        catch (Client.Error err) {
            return createError("Tile error: %s", err.getMessage());
        }
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
        Map<String, String> entity = new HashMap<>();
        entity.put("error", String.format(message, args));
        return ResponseEntity
                .status(status)
                .body(entity);
    }

    private void initializeUnirest() {
        Unirest.setObjectMapper(new ObjectMapper() {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();

            public <T> T readValue(String s, Class<T> cls) {
                try {
                    return mapper.readValue(s, cls);
                }
                catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }

            public String writeValue(Object o) {
                try {
                    return mapper.writeValueAsString(o);
                }
                catch (JsonProcessingException e) {
                    throw new RuntimeException(e);
                }
            }
        });
    }

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
