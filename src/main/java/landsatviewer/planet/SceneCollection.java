package landsatviewer.planet;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class SceneCollection {
    public List<Scene> features;
    public String type = "FeatureCollection";
}
