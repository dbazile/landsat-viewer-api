package landsatviewer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.mashape.unirest.http.ObjectMapper;
import com.mashape.unirest.http.Unirest;
import landsatviewer.planet.Client;
import landsatviewer.planet.Scene;
import landsatviewer.planet.SceneCollection;

import javax.ws.rs.*;
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.Response;

import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Path("/")
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

    @GET
    @Produces("application/json")
    public Map<String, Double> healthCheck() {
        HashMap<String, Double> status = new HashMap<>();
        status.put("uptime", (Instant.now().toEpochMilli() - START_TIMESTAMP) / 1000.0);
        return status;
    }

    @GET
    @Path("/scenes")
    @Produces("application/json")
    public Response search(@QueryParam("x") Double x,
                           @QueryParam("y") Double y,
                           @QueryParam("days_ago") @DefaultValue("14") int daysAgo) {
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

    @GET
    @Path("/scenes/{id}")
    @Produces("application/json")
    public Response getScene(@PathParam("id") String id) {
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

    @GET
    @Path("/tile/{id}/{z}/{x}/{y}.png")
    @Produces("image/png")
    public Response tile(@PathParam("id") String sceneId,
                         @PathParam("x") int x,
                         @PathParam("y") int y,
                         @PathParam("z") int z) {
        try {
            return createCached(client.fetchTile(sceneId, x, y, z), CACHE_LONG);
        }
        catch (Client.Error err) {
            return createError("Tile error: %s", err.getMessage());
        }
    }

    private Response createCached(Object entity, int maxAge) {
        CacheControl cacheControl = new CacheControl();
        cacheControl.setMaxAge(maxAge);
        return Response
                .ok(entity)
                .cacheControl(cacheControl)
                .build();
    }

    private Response createError(String message, Object... args) {
        Map<String, String> entity = new HashMap<>();
        entity.put("error", String.format(message, args));
        return Response
                .serverError()
                .entity(entity)
                .build();
    }

    private Response createError(int status, String message, Object... args) {
        Map<String, String> entity = new HashMap<>();
        entity.put("error", String.format(message, args));
        return Response
                .status(status)
                .entity(entity)
                .build();
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
}
