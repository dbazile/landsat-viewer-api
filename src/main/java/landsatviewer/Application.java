package landsatviewer;

import landsatviewer.planet.Client;

import javax.ws.rs.*;
import javax.ws.rs.core.Response;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Path("/")
public class Application {
    private Client client;
    private static long startTimestamp = Instant.now().toEpochMilli();

    public Application() {
        client = new Client(System.getenv("PLANET_API_KEY"));
    }

    @GET
    @Produces("application/json")
    public Map<String, Double> healthCheck() {
        HashMap<String, Double> status = new HashMap<>();
        status.put("uptime", (Instant.now().toEpochMilli() - startTimestamp) / 1000.0);
        return status;
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
