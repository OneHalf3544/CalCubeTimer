package net.gnehzr.cct.dao.converters;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;
import java.time.Duration;
import java.util.Objects;

/**
 * <p>
 * <p>
 * Created: 08.02.2015 23:18
 * <p>
 *
 * @author OneHalf
 */
@Converter(autoApply = true)
public class DurationConverter implements AttributeConverter<Duration, String> {
    @Override
    public String convertToDatabaseColumn(Duration attribute) {
        return Objects.toString(attribute, null);
    }

    @Override
    public Duration convertToEntityAttribute(String dbData) {
        return Duration.parse(dbData);
    }
}
