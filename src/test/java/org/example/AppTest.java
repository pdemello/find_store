package org.example;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.http.HttpClient;

import static org.junit.Assert.*;

public class AppTest 
{
    public Store store;
    public App app;

    @Before
    public void setUp() throws Exception {
        app = new App();
        store = new Store();
        store.name = "Store Name";
        store.location = "Store Location";
        store.address = "1600 Pennsylvania Ave NW, Washington, DC 2050";
        store.city = "Washington";
        store.state = "DC";
        store.zip = "02050";
        store.county = "Washington County";
        store.latitude = "38.897957";
        store.longitude = "-77.036560";
        store.distance = 1.2345;
        store.units = Units.mi.name();
    }

    @Ignore
    @Test
    public void call() {
        // TODO: test CLI behavior
    }

    @Test
    public void formatOutput_shouldFormatJsonIfJson() {
        try {
            String output = app.formatOutput(store, 2.3456, Units.mi, Output.json);
            assertEquals(app.formatJsonOutput(store), output);
        } catch (JsonProcessingException e) {
            fail();
        }
    }

    @Test
    public void formatOutput_shouldFormatTextIfText() {
        try {
            String output = app.formatOutput(store, 2.3456, Units.mi, Output.text);
            assertEquals(app.formatTextOutput(store), output);
        } catch (JsonProcessingException e) {
            fail();
        }
    }

    @Test
    public void convertDistance_shouldPreserveMilesIfMiles() {
        assertEquals(867.5309, app.convertDistance(867.5309, Units.mi), 0.0);
    }

    @Test
    public void convertDistance_shouldReturnKilometersIfKilometers() {
        assertEquals(1396.1556487296, app.convertDistance(867.5309, Units.km), 0.0);
    }

    @Test
    public void formatJsonOutput_shouldFormatCorrectJson() {
        try {
            String output = app.formatJsonOutput(store);
            assertEquals(
                    "{\"name\":\"Store Name\",\"location\":\"Store Location\",\"address\":\"1600 Pennsylvania Ave NW, Washington, DC 2050\",\"city\":\"Washington\",\"state\":\"DC\",\"zip\":\"02050\",\"latitude\":\"38.897957\",\"longitude\":\"-77.036560\",\"county\":\"Washington County\",\"distance\":1.2345,\"units\":\"mi\"}",
                    output);
        } catch (Exception e) {
            fail();
        }
    }

    @Test
    public void formatTextOutput_shouldFormatCorrectText() {
        String output = app.formatTextOutput(store);
        assertEquals(
                "Store Name\n" +
                "Store Location\n" +
                "1600 Pennsylvania Ave NW, Washington, DC 2050\n" +
                "Washington, DC 02050\n" +
                "Washington County\n" +
                "Latitude: 38.897957\n" +
                "Longitude: -77.036560\n" +
                "Distance: 1.234500 mi",
                output);
    }

    @Test
    public void convertZipToLatLong_latitude_whenFound() {
        try {
            LatLong actual;
            actual = app.convertZipToLatLong("94111");
            assertEquals("37.79937", actual.latitude);
            assertEquals("-122.398409", actual.longitude);
        } catch (Exception e) {
            fail();
        }
    }

    @Test
    public void convertZipToLatLong_whenNotFound() {
        try {
            app.convertZipToLatLong("12345");
            fail();
        } catch (RuntimeException e) {
            assertEquals(e.getMessage(), "Error: zip not found");
        } catch (Exception e) {
            fail();
        }
    }

    @Test
    public void convertZipToLatLong_whenInvalid() {
        try {
            app.convertZipToLatLong("Hello!");
        } catch (RuntimeException e) {
            assertEquals(e.getMessage(), "Error: zip not found");
        } catch (Exception e) {
            fail();
        }
    }

    @Test
    public void convertAddressToLatLong_valid() {
        // TODO: consider passing in a mocked HttpClient
        HttpClient httpClient = HttpClient.newBuilder().version(HttpClient.Version.HTTP_2).build();
        try {
            final LatLong actual = app.convertAddressToLatLong(httpClient, "1600 Pennsylvania Ave, Washington, DC");
            assertEquals("38.898735", actual.latitude);
            assertEquals("-77.038025", actual.longitude);
        } catch (IOException | InterruptedException e) {
            fail();
        }
    }

    @Test
    public void convertAddressToLatLong_valid_incomplete() {
        HttpClient httpClient = HttpClient.newBuilder().version(HttpClient.Version.HTTP_2).build();
        try {
            final LatLong actual = app.convertAddressToLatLong(httpClient, "1600 Pennsylvania Ave, Washington, DC");
            assertEquals("38.898735", actual.latitude);
            assertEquals("-77.038025", actual.longitude);
        } catch (IOException | InterruptedException e) {
            fail();
        }
    }

    @Test
    public void convertAddressToLatLong_valid_full() {
        HttpClient httpClient = HttpClient.newBuilder().version(HttpClient.Version.HTTP_2).build();
        try {
            final LatLong actual = app.convertAddressToLatLong(httpClient, "1600 Pennsylvania Ave NW, Washington, DC 20500");
            assertEquals("38.898735", actual.latitude);
            assertEquals("-77.038025", actual.longitude);
        } catch (IOException | InterruptedException e) {
            fail();
        }
    }

    @Test
    public void convertAddressToLatLong_invalid_wrongZip() {
        HttpClient httpClient = HttpClient.newBuilder().version(HttpClient.Version.HTTP_2).build();
        try {
            app.convertAddressToLatLong(httpClient, "1600 Pennsylvania Ave, Washington, DC 12345");
            fail();
        } catch (IOException | InterruptedException e) {
            fail();
        } catch (RuntimeException e) {
            assertEquals("Error: could not parse address location", e.getMessage());
        }
    }

    @Test
    public void findDistance_shouldConvertStrings() {
        LatLong latLong = new LatLong();
        latLong.latitude = "35.6789";
        latLong.longitude = "-123.4567";
        final double actual = app.findDistance(store, latLong);
        final double expected = app.calculateDistanceMiles(35.6789, -123.4567, 38.897957, -77.036560);
        assertEquals(expected, actual, 0.0);
    }

    @Test
    public void calculateDistanceMiles_longDistance() {
        final double actual = app.calculateDistanceMiles(35.6789, -123.4567, 38.897957, -77.036560);
        assertEquals(2532.2404742902672, actual, 0.0);
    }

    @Test
    public void calculateDistanceMiles_shortDistance() {
        final double actual = app.calculateDistanceMiles(35.6789, -123.4567, 35.68, -123.457);
        assertEquals(0.07779282770314093, actual, 0.0);
    }

    @Test
    public void calculateDistanceMiles_zeroDistance() {
        final double actual = app.calculateDistanceMiles(35.6789, -123.4567, 35.6789, -123.4567);
        assertEquals(0.0, actual, 0.0);
    }
}
