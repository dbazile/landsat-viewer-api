package landsatviewer.planet;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.ObjectMapper;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import com.mashape.unirest.http.options.Option;
import com.mashape.unirest.http.options.Options;

import java.io.IOException;
import java.io.InputStream;

public class Client {
    private static final String TILE_URL = "https://tiles.planet.com/data/v1/Landsat8L1G/{sceneId}/{z}/{x}/{y}.png";
    private static final String SCENE_URL = "https://api.planet.com/data/v1/item-types/Landsat8L1G/items/{sceneId}";
    private static final String SEARCH_URL = "https://api.planet.com/data/v1/quick-search";

    private final String apiKey;

    static {
        initializeUnirest();
    }

    public Client(String apiKey) {
        this.apiKey = apiKey;
    }

    public InputStream fetchTile(String sceneId, int x, int y, int z) throws Error {
        HttpResponse<InputStream> response;

        try {
            response = Unirest.get(TILE_URL)
                    .routeParam("x", String.valueOf(x))
                    .routeParam("y", String.valueOf(y))
                    .routeParam("z", String.valueOf(z))
                    .routeParam("sceneId", sceneId)
                    .basicAuth(apiKey, "")
                    .asBinary();
        }
        catch (UnirestException e) {
            throw new Error(e);
        }

        int status = response.getStatus();
        if (status != 200) {
            throw new Error(String.format("Planet returned HTTP %s", status));
        }

        return response.getBody();
    }

    public Scene getScene(String sceneId) throws Error {
        HttpResponse<Scene> response;
        try {
            response = Unirest.get(SCENE_URL)
                    .routeParam("sceneId", sceneId)
                    .basicAuth(apiKey, "")
                    .asObject(Scene.class);
        }
        catch (UnirestException e) {
            throw new Error(e);
        }

        int status = response.getStatus();
        if (status != 200) {
            if (status == 404) {
                throw new NotFound();
            }
            throw new Error(String.format("Planet returned HTTP %s", status));
        }

        return response.getBody();
    }

    public SceneCollection search(double x, double y, int daysSince) throws Error {
        HttpResponse<SceneCollection> response;

        try {
            response = Unirest.post(SEARCH_URL)
                    .basicAuth(apiKey, "")
                    .header("Content-Type", "application/json")
                    .body(new SearchCriteria(x, y, daysSince))
                    .asObject(SceneCollection.class);
        }
        catch (UnirestException e) {
            throw new Error(e);
        }

        int status = response.getStatus();
        if (status != 200) {
            throw new Error(String.format("Planet returned HTTP %s", status));
        }

        return response.getBody();
    }

    public static class NotFound extends Error {
        public NotFound() {
            super("scene not found");
        }
    }

    public static class Error extends Exception {
        public Error(String message) {
            super(message);
        }

        public Error(Throwable e) {
            super(e);
        }
    }

    private static void initializeUnirest() {
        // HACK -- invoking 'setOption' directly avoids clobbering custom clients
        Options.setOption(Option.OBJECT_MAPPER, new ObjectMapper() {
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
