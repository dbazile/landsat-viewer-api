package landsatviewer.planet;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class GeometryFilterTest {

    @Test
    public void canInstantiate() {
        new SearchCriteria.GeometryFilter("test-field-name", 5, 5);
    }

    @Test
    public void setsCorrectFieldName() {
        SearchCriteria.GeometryFilter filter = new SearchCriteria.GeometryFilter("test-field-name", 5, 5);

        assertEquals(filter.fieldName, "test-field-name");
    }

    @Test
    public void setsCorrectType() {
        SearchCriteria.GeometryFilter filter = new SearchCriteria.GeometryFilter("test-field-name", 5, 5);

        assertEquals(filter.type, "GeometryFilter");
    }


    @Test
    public void createsConfig() {
        SearchCriteria.GeometryFilter filter = new SearchCriteria.GeometryFilter("test-field-name", 5, 5);

        assertNotNull(filter.config);
    }

    @Test
    public void definesValidPolygon() {
        SearchCriteria.GeometryFilter filter = new SearchCriteria.GeometryFilter("test-field-name", 5, 5);

        assertEquals(filter.config.type, "Polygon");
        assertEquals(filter.config.coordinates.size(), 1);
        assertEquals(filter.config.coordinates.get(0).size(), 33);
        filter.config.coordinates.get(0).forEach(c -> assertEquals(c.size(), 2));
    }
}
