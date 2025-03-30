package ru.kavlab.dataimportaddon.app.service.odata;

import io.jmix.core.Metadata;
import org.apache.olingo.client.api.domain.ClientProperty;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.kavlab.dataimportaddon.app.data.ExternalMetadata;
import ru.kavlab.dataimportaddon.app.data.ExternalProperty;
import ru.kavlab.dataimportaddon.app.data.PropertyInfo;
import ru.kavlab.dataimportaddon.app.service.mapping.MappingServiceImpl;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Time;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertFalse;

@ExtendWith(MockitoExtension.class)
class ODataEntityMapperTest {

    private ODataEntityMapper dataEntityMapper;

    @Mock
    private Metadata metadata;
    @Mock
    private MappingServiceImpl mappingService;
    @Mock
    private ClientProperty clientProperty;
    @Mock
    ExternalProperty externalProperty;

    @BeforeEach
    void setUp() {
        List<ExternalMetadata> externalMetadataList = new ArrayList<>();
        dataEntityMapper = new ODataEntityMapper(metadata, mappingService, externalMetadataList);
    }

    @Test
    @DisplayName("Test conversion for Edm.Guid: valid conversion to String and invalid conversion for other types")
    void testEdmGuidConversion() {
        String rawValue = "123e4567-e89b-12d3-a456-426614174000";
        // Prepare a property with type Edm.Guid
        PropertyInfo propInfo = new PropertyInfo("guidProp", String.class, true, 0);
        ExternalProperty extProp = new ExternalProperty("guidProp", "Edm.Guid", true);

        // When target type is String, conversion should succeed.
        Optional<String> resultString = dataEntityMapper.convertValue(
                clientProperty, rawValue, propInfo, extProp, String.class);
        assertTrue(resultString.isPresent());
        assertEquals(rawValue, resultString.get());

        // When target type is not String (e.g. Integer), conversion should return empty.
        Optional<Integer> resultInteger = dataEntityMapper.convertValue(
                clientProperty, rawValue, propInfo, extProp, Integer.class);
        assertFalse(resultInteger.isPresent());
    }

    @Test
    @DisplayName("Test String conversion with and without truncation based on maxLength")
    void testStringConversion() {
        // Test without truncation: maxLength is greater than the raw value length.
        PropertyInfo propInfoNoTrunc = new PropertyInfo("stringProp", String.class, true, 50);
        ExternalProperty extProp = new ExternalProperty("stringProp", "Edm.String", true);
        Optional<String> resultNoTrunc = dataEntityMapper.convertValue(clientProperty, "Hello World", propInfoNoTrunc, extProp, String.class);
        assertTrue(resultNoTrunc.isPresent());
        assertEquals("Hello World", resultNoTrunc.get());

        // Test with truncation: maxLength is less than the raw value length.
        PropertyInfo propInfoTrunc = new PropertyInfo("stringProp", String.class, true, 5);
        Optional<String> resultTrunc = dataEntityMapper.convertValue(clientProperty, "Hello World", propInfoTrunc, extProp, String.class);
        assertTrue(resultTrunc.isPresent());
        assertEquals("Hello", resultTrunc.get());
    }

    @Test
    @DisplayName("Test Date conversion with valid and invalid date strings")
    void testDateConversion() throws ParseException {
        PropertyInfo propInfo = new PropertyInfo("dateProp", Date.class, true, 0);
        ExternalProperty extProp = new ExternalProperty("dateProp", "Edm.DateTime", true);
        String validDateStr = "2020-05-15T12:30:45";

        // Test valid date string
        Optional<Date> resultDate = dataEntityMapper.convertValue(clientProperty, validDateStr, propInfo, extProp, Date.class);
        assertTrue(resultDate.isPresent());
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
        Date expectedDate = sdf.parse(validDateStr);
        assertEquals(expectedDate, resultDate.get());

        // Test invalid date string
        Optional<Date> invalidDate = dataEntityMapper.convertValue(clientProperty, "invalid-date", propInfo, extProp, Date.class);
        assertFalse(invalidDate.isPresent());
    }

    @Test
    @DisplayName("Test Integer conversion with valid and invalid input")
    void testIntegerConversion() {
        PropertyInfo propInfo = new PropertyInfo("intProp", Integer.class, true, 0);
        ExternalProperty extProp = new ExternalProperty("intProp", "Edm.Int32", true);

        Optional<Integer> validInt = dataEntityMapper.convertValue(clientProperty, "123", propInfo, extProp, Integer.class);
        assertTrue(validInt.isPresent());
        assertEquals(123, validInt.get());

        Optional<Integer> invalidInt = dataEntityMapper.convertValue(clientProperty, "abc", propInfo, extProp, Integer.class);
        assertFalse(invalidInt.isPresent());
    }

    @Test
    @DisplayName("Test Long conversion with valid and invalid input")
    void testLongConversion() {
        PropertyInfo propInfo = new PropertyInfo("longProp", Long.class, true, 0);
        ExternalProperty extProp = new ExternalProperty("longProp", "Edm.Int64", true);

        Optional<Long> validLong = dataEntityMapper.convertValue(clientProperty, "1234567890", propInfo, extProp, Long.class);
        assertTrue(validLong.isPresent());
        assertEquals(1234567890L, validLong.get());

        Optional<Long> invalidLong = dataEntityMapper.convertValue(clientProperty, "abc", propInfo, extProp, Long.class);
        assertFalse(invalidLong.isPresent());
    }

    @Test
    @DisplayName("Test Short conversion with valid and invalid input")
    void testShortConversion() {
        PropertyInfo propInfo = new PropertyInfo("shortProp", Short.class, true, 0);
        ExternalProperty extProp = new ExternalProperty("shortProp", "Edm.Int16", true);

        Optional<Short> validShort = dataEntityMapper.convertValue(clientProperty, "123", propInfo, extProp, Short.class);
        assertTrue(validShort.isPresent());
        assertEquals((short) 123, validShort.get());

        Optional<Short> invalidShort = dataEntityMapper.convertValue(clientProperty, "abc", propInfo, extProp, Short.class);
        assertFalse(invalidShort.isPresent());
    }

    @Test
    @DisplayName("Test BigDecimal conversion with valid and invalid input")
    void testBigDecimalConversion() {
        PropertyInfo propInfo = new PropertyInfo("bdProp", BigDecimal.class, true, 0);
        ExternalProperty extProp = new ExternalProperty("bdProp", "Edm.Double", true);

        Optional<BigDecimal> validBD = dataEntityMapper.convertValue(clientProperty, "12345.67", propInfo, extProp, BigDecimal.class);
        assertTrue(validBD.isPresent());
        assertEquals(new BigDecimal("12345.67"), validBD.get());

        Optional<BigDecimal> invalidBD = dataEntityMapper.convertValue(clientProperty, "abc", propInfo, extProp, BigDecimal.class);
        assertFalse(invalidBD.isPresent());
    }

    @Test
    @DisplayName("Test BigInteger conversion with valid and invalid input")
    void testBigIntegerConversion() {
        PropertyInfo propInfo = new PropertyInfo("biProp", BigInteger.class, true, 0);
        ExternalProperty extProp = new ExternalProperty("biProp", "Edm.Int64", true);

        Optional<BigInteger> validBI = dataEntityMapper.convertValue(clientProperty, "123456789012345", propInfo, extProp, BigInteger.class);
        assertTrue(validBI.isPresent());
        assertEquals(new BigInteger("123456789012345"), validBI.get());

        Optional<BigInteger> invalidBI = dataEntityMapper.convertValue(clientProperty, "abc", propInfo, extProp, BigInteger.class);
        assertFalse(invalidBI.isPresent());
    }

    @Test
    @DisplayName("Test Boolean conversion with various inputs")
    void testBooleanConversion() {
        PropertyInfo propInfo = new PropertyInfo("boolProp", Boolean.class, true, 0);
        ExternalProperty extProp = new ExternalProperty("boolProp", "Edm.Boolean", true);

        // "true" should yield true
        Optional<Boolean> trueBool = dataEntityMapper.convertValue(clientProperty, "true", propInfo, extProp, Boolean.class);
        assertTrue(trueBool.isPresent());
        assertTrue(trueBool.get());

        // "false" should yield false
        Optional<Boolean> falseBool = dataEntityMapper.convertValue(clientProperty, "false", propInfo, extProp, Boolean.class);
        assertTrue(falseBool.isPresent());
        assertFalse(falseBool.get());

        // Any other value (e.g., "yes") should yield false by default
        Optional<Boolean> nonTrue = dataEntityMapper.convertValue(clientProperty, "yes", propInfo, extProp, Boolean.class);
        assertTrue(nonTrue.isPresent());
        assertFalse(nonTrue.get());
    }

    @Test
    @DisplayName("Test Character conversion with valid and invalid input")
    void testCharacterConversion() {
        PropertyInfo propInfo = new PropertyInfo("charProp", Character.class, true, 0);
        ExternalProperty extProp = new ExternalProperty("charProp", "Edm.String", true);

        Optional<Character> validChar = dataEntityMapper.convertValue(clientProperty, "A", propInfo, extProp, Character.class);
        assertTrue(validChar.isPresent());
        assertEquals('A', validChar.get());

        // Input with length != 1 should return empty
        Optional<Character> invalidChar = dataEntityMapper.convertValue(clientProperty, "AB", propInfo, extProp, Character.class);
        assertFalse(invalidChar.isPresent());
    }

    @Test
    @DisplayName("Test Double conversion with valid and invalid input")
    void testDoubleConversion() {
        PropertyInfo propInfo = new PropertyInfo("doubleProp", Double.class, true, 0);
        ExternalProperty extProp = new ExternalProperty("doubleProp", "Edm.Double", true);

        Optional<Double> validDouble = dataEntityMapper.convertValue(clientProperty, "12.34", propInfo, extProp, Double.class);
        assertTrue(validDouble.isPresent());
        assertEquals(12.34, validDouble.get());

        Optional<Double> invalidDouble = dataEntityMapper.convertValue(clientProperty, "abc", propInfo, extProp, Double.class);
        assertFalse(invalidDouble.isPresent());
    }

    @Test
    @DisplayName("Test Float conversion with valid and invalid input")
    void testFloatConversion() {
        PropertyInfo propInfo = new PropertyInfo("floatProp", Float.class, true, 0);
        ExternalProperty extProp = new ExternalProperty("floatProp", "Edm.Double", true);

        Optional<Float> validFloat = dataEntityMapper.convertValue(clientProperty, "12.34", propInfo, extProp, Float.class);
        assertTrue(validFloat.isPresent());
        assertEquals(12.34f, validFloat.get());

        Optional<Float> invalidFloat = dataEntityMapper.convertValue(clientProperty, "abc", propInfo, extProp, Float.class);
        assertFalse(invalidFloat.isPresent());
    }

    @Test
    @DisplayName("Test LocalDate conversion with valid and invalid input")
    void testLocalDateConversion() {
        PropertyInfo propInfo = new PropertyInfo("localDateProp", LocalDate.class, true, 0);
        ExternalProperty extProp = new ExternalProperty("localDateProp", "Edm.DateTime", true);

        // Valid ISO date-time string should extract the date part ("2020-05-15")
        Optional<LocalDate> validLocalDate = dataEntityMapper.convertValue(clientProperty, "2020-05-15T12:30:45", propInfo, extProp, LocalDate.class);
        assertTrue(validLocalDate.isPresent());
        assertEquals(LocalDate.of(2020, 5, 15), validLocalDate.get());

        // Valid date string without time
        Optional<LocalDate> validLocalDate2 = dataEntityMapper.convertValue(clientProperty, "2020-05-15", propInfo, extProp, LocalDate.class);
        assertTrue(validLocalDate2.isPresent());
        assertEquals(LocalDate.of(2020, 5, 15), validLocalDate2.get());

        // Invalid date string
        Optional<LocalDate> invalidLocalDate = dataEntityMapper.convertValue(clientProperty, "invalid", propInfo, extProp, LocalDate.class);
        assertFalse(invalidLocalDate.isPresent());
    }

    @Test
    @DisplayName("Test LocalDateTime conversion with valid and invalid input")
    void testLocalDateTimeConversion() {
        PropertyInfo propInfo = new PropertyInfo("localDateTimeProp", LocalDateTime.class, true, 0);
        ExternalProperty extProp = new ExternalProperty("localDateTimeProp", "Edm.DateTime", true);

        Optional<LocalDateTime> validLDT = dataEntityMapper.convertValue(clientProperty, "2020-05-15T12:30:45", propInfo, extProp, LocalDateTime.class);
        assertTrue(validLDT.isPresent());
        assertEquals(LocalDateTime.of(2020, 5, 15, 12, 30, 45), validLDT.get());

        Optional<LocalDateTime> invalidLDT = dataEntityMapper.convertValue(clientProperty, "invalid", propInfo, extProp, LocalDateTime.class);
        assertFalse(invalidLDT.isPresent());
    }

    @Test
    @DisplayName("Test LocalTime conversion with valid and invalid input")
    void testLocalTimeConversion() {
        PropertyInfo propInfo = new PropertyInfo("localTimeProp", LocalTime.class, true, 0);
        ExternalProperty extProp = new ExternalProperty("localTimeProp", "Edm.Time", true);

        Optional<LocalTime> validLT = dataEntityMapper.convertValue(clientProperty, "12:30:45", propInfo, extProp, LocalTime.class);
        assertTrue(validLT.isPresent());
        assertEquals(LocalTime.of(12, 30, 45), validLT.get());

        Optional<LocalTime> invalidLT = dataEntityMapper.convertValue(clientProperty, "invalid", propInfo, extProp, LocalTime.class);
        assertFalse(invalidLT.isPresent());
    }

    @Test
    @DisplayName("Test OffsetDateTime conversion with valid and invalid input")
    void testOffsetDateTimeConversion() {
        PropertyInfo propInfo = new PropertyInfo("offsetDateTimeProp", OffsetDateTime.class, true, 0);
        ExternalProperty extProp = new ExternalProperty("offsetDateTimeProp", "Edm.DateTime", true);

        String validODTStr = "2020-05-15T12:30:45+01:00";
        Optional<OffsetDateTime> validODT = dataEntityMapper.convertValue(clientProperty, validODTStr, propInfo, extProp, OffsetDateTime.class);
        assertTrue(validODT.isPresent());
        assertEquals(OffsetDateTime.parse(validODTStr), validODT.get());

        Optional<OffsetDateTime> invalidODT = dataEntityMapper.convertValue(clientProperty, "invalid", propInfo, extProp, OffsetDateTime.class);
        assertFalse(invalidODT.isPresent());
    }

    @Test
    @DisplayName("Test OffsetTime conversion with valid and invalid input")
    void testOffsetTimeConversion() {
        PropertyInfo propInfo = new PropertyInfo("offsetTimeProp", OffsetTime.class, true, 0);
        ExternalProperty extProp = new ExternalProperty("offsetTimeProp", "Edm.Time", true);

        String validOTStr = "12:30:45+01:00";
        Optional<OffsetTime> validOT = dataEntityMapper.convertValue(clientProperty, validOTStr, propInfo, extProp, OffsetTime.class);
        assertTrue(validOT.isPresent());
        assertEquals(OffsetTime.parse(validOTStr), validOT.get());

        Optional<OffsetTime> invalidOT = dataEntityMapper.convertValue(clientProperty, "invalid", propInfo, extProp, OffsetTime.class);
        assertFalse(invalidOT.isPresent());
    }

    @Test
    @DisplayName("Test SQL Time conversion with valid and invalid input")
    void testSqlTimeConversion() {
        PropertyInfo propInfo = new PropertyInfo("sqlTimeProp", Time.class, true, 0);
        ExternalProperty extProp = new ExternalProperty("sqlTimeProp", "Edm.Time", true);

        String validTimeStr = "12:30:45";
        Optional<Time> validTime = dataEntityMapper.convertValue(clientProperty, validTimeStr, propInfo, extProp, Time.class);
        assertTrue(validTime.isPresent());
        assertEquals(Time.valueOf(LocalTime.of(12, 30, 45)), validTime.get());

        Optional<Time> invalidTime = dataEntityMapper.convertValue(clientProperty, "invalid", propInfo, extProp, Time.class);
        assertFalse(invalidTime.isPresent());
    }
}