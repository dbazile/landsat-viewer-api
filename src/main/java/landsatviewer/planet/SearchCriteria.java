package landsatviewer.planet;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;

public class SearchCriteria {

    @JsonProperty("item_types")
    public String[] itemTypes = {"Landsat8L1G"};

    public AndFilter filter = new AndFilter();

    public SearchCriteria(double x, double y, int daysSince) {
        filter = new AndFilter();
        filter.config.add(new DateRangeFilter("acquired", daysSince));
        filter.config.add(new GeometryFilter("geometry", x, y));
    }

    static class AndFilter {
        public String type = "AndFilter";
        public List<Object> config = new ArrayList<>();
    }

    static class DateRangeFilter {
        public String type = "DateRangeFilter";

        @JsonProperty("field_name")
        public String fieldName;

        public Map<String, String> config = new HashMap<>();

        public DateRangeFilter(String fieldName, int daysSince) {
            this.fieldName = fieldName;
            this.config.put("gte", Duration.ofDays(daysSince).subtractFrom(Instant.now()).toString());
        }
    }

    static class GeometryFilter {
        private static final double BUFFER_SIZE = 1.0;

        public String type = "GeometryFilter";

        @JsonProperty("field_name")
        public String fieldName;

        public ConfigGeometry config;

        public GeometryFilter(String fieldName, double x, double y) {
            this.fieldName = fieldName;
            this.config = new ConfigGeometry(x, y);
        }

        static class ConfigGeometry {
            public String type = "Polygon";
            public List<List<List<Double>>> coordinates;

            public ConfigGeometry(double x, double y) {
                GeometryFactory factory = new GeometryFactory();
                Geometry polygon = factory.createPoint(new Coordinate(x, y)).buffer(BUFFER_SIZE);

                List<List<Double>> points = new ArrayList<>();
                for (Coordinate c : polygon.getCoordinates()) {
                    List<Double> point = new ArrayList<>(2);
                    point.add(c.x);
                    point.add(c.y);
                    points.add(point);
                }

                coordinates = new ArrayList<>();
                coordinates.add(points);
            }
        }
    }
}
