package landsatviewer.planet;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Scene {
    public String id;
    public Geometry geometry;
    public Properties properties;
    public String type;

    @JsonIgnoreProperties(ignoreUnknown = true)
    static class Properties {
        public String acquiredOn;
        public int resolution;
        public float cloudCover;
        public String wrsPath;
        public String wrsRow;

        @JsonCreator
        public Properties(@JsonProperty("acquired") String acquiredOn,
                          @JsonProperty("cloud_cover") float cloudCover,
                          @JsonProperty("pixel_resolution") int resolution,
                          @JsonProperty("wrs_path") int wrsPath,
                          @JsonProperty("wrs_row") int wrsRow) {
            this.acquiredOn = acquiredOn;
            this.cloudCover = cloudCover;
            this.resolution = resolution;
            this.wrsPath = String.format("%03d", wrsPath);
            this.wrsRow = String.format("%03d", wrsRow);
        }
    }

    static class Geometry {
        public String type;
        public double[][][] coordinates;
    }
}
