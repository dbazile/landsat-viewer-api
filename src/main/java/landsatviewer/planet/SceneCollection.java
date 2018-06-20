package landsatviewer.planet;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class SceneCollection {
    public List<Scene> features;
    public String type = "FeatureCollection";
}
