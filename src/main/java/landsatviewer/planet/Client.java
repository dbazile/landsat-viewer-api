package landsatviewer.planet;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;

import java.io.InputStream;
import java.util.Scanner;

public class Client {
    public static final String TILE_URL = "https://tiles.planet.com/data/v1/Landsat8L1G/{sceneId}/{z}/{x}/{y}.png";
    private final String apiKey;

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
                    .queryString("api_key", apiKey)
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

    public static class Error extends Exception {
        public Error(String message) {
            super(message);
        }

        public Error(Throwable e) {
            super(e);
        }
    }
}
