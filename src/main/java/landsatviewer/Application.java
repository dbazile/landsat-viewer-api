package landsatviewer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.mashape.unirest.http.ObjectMapper;
import com.mashape.unirest.http.Unirest;
import landsatviewer.planet.Client;
import landsatviewer.planet.Scene;

import javax.ws.rs.*;
import javax.ws.rs.core.Response;

import java.io.IOException;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Path("/")
public class Application {
    private Client client;
    private static long startTimestamp = Instant.now().toEpochMilli();

    public Application() {
        client = new Client(System.getenv("PLANET_API_KEY"));
        initializeUnirest();
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

    @GET
    @Produces("application/json")
    public Map<String, Double> healthCheck() {
        HashMap<String, Double> status = new HashMap<>();
        status.put("uptime", (Instant.now().toEpochMilli() - startTimestamp) / 1000.0);
        return status;
    }

    @GET
    @Path("/scene/{id}")
    @Produces("application/json")
    public Scene getScene(@PathParam("id") String id) throws Client.Error {
        return client.getScene(id);
    }

    @GET
    @Path("/tile/{id}/{z}/{x}/{y}.png")
    @Produces("image/png")
    public Response tile(@PathParam("id") String sceneId,
                         @PathParam("x") int x,
                         @PathParam("y") int y,
                         @PathParam("z") int z) throws Client.Error {
        return Response.ok(client.fetchTile(sceneId, x, y, z)).build();
    }
}
