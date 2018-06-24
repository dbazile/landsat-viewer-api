package landsatviewer.planet;

import java.time.Instant;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class DateRangeFilterTest {

    @Test
    public void canInstantiate() {
        new SearchCriteria.DateRangeFilter("test-field-name", 1234);
    }

    @Test
    public void setsCorrectFieldName() {
        SearchCriteria.DateRangeFilter filter = new SearchCriteria.DateRangeFilter("test-field-name", 1234);

        assertEquals(filter.fieldName, "test-field-name");
    }

    @Test
    public void setsCorrectType() {
        SearchCriteria.DateRangeFilter filter = new SearchCriteria.DateRangeFilter("test-field-name", 1234);

        assertEquals(filter.type, "DateRangeFilter");
    }

    @Test
    public void createsConfig() {
        SearchCriteria.DateRangeFilter filter = new SearchCriteria.DateRangeFilter("test-field-name", 1234);

        assertNotNull(filter.config);
    }

    @Test
    public void setsStartDate() {
        SearchCriteria.DateRangeFilter filter = new SearchCriteria.DateRangeFilter("test-field-name", 1234);

        assertTrue(Instant.parse(filter.config.get("gte")).isBefore(Instant.now()));
    }
}
