package landsatviewer.planet;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Scene {
    private String id = "";

    public String getId() {
        return id;
    }
}
