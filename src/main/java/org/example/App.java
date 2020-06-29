package org.example;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.opencsv.bean.CsvToBeanBuilder;
import picocli.CommandLine;
import picocli.CommandLine.*;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Callable;

enum Units {
    mi, km
}

enum Output {
    text, json
}

class LatLong {
    String latitude;
    String longitude;
}

@Command(name = "find_store", description = "Finds the closest store to the given zip or address")
class App implements Callable<Integer> {

    @ArgGroup(exclusive = true, multiplicity = "1")
    private Origin origin;
    static class Origin {
        @Option(names = "--zip", description = "zip code for search", required = true) String zip;
        @Option(names = "--address", description = "address for search", required = true) String address;
    }

    @Option(names = {"--output"}, description = "text or json")
    private Output output = Output.text;

    @Option(names = {"--units"}, description = "mi or km")
    private Units units = Units.mi;

    public static void main(String... args) {
        System.exit(new CommandLine(new App()).execute(args));
    }
    @Override
    public Integer call() throws Exception {
        if (origin.zip == null && origin.address == null) {
            // Not supposed to happen; one of zip or address should be required by the CLI
            throw new RuntimeException("Error: One of either zip or address is required");
        }
        HttpClient httpClient = HttpClient.newBuilder().version(HttpClient.Version.HTTP_2).build();
        LatLong originLL = origin.zip == null ? convertAddressToLatLong(httpClient, origin.address) : convertZipToLatLong(origin.zip);

        Store closestStore = null;
        double shortestDistanceMiles = Double.MAX_VALUE;
        for (Store store : new CsvToBeanBuilder<Store>(new FileReader("src/store-locations.csv")).withType(Store.class).build()) {
            // Calculate distance
            double distanceMiles = findDistance(store, originLL);
            if (distanceMiles < shortestDistanceMiles) {
                closestStore = store;
                shortestDistanceMiles = distanceMiles;
            }
        }
        if (closestStore == null) {
            // Not supposed to happen; we should have read at least one store in the data
            throw new RuntimeException("Error: No stores found");
        }
        System.out.println(formatOutput(closestStore, shortestDistanceMiles, units, output));
        return 0;
    }

    public String formatOutput(Store closestStore, double shortestDistanceMiles, Units units, Output output) throws JsonProcessingException {
        closestStore.distance = convertDistance(shortestDistanceMiles, units);
        closestStore.units = units.name();
        return output == Output.json ? formatJsonOutput(closestStore) : formatTextOutput(closestStore);
    }

    public double convertDistance(double shortestDistanceMiles, Units units) {
        final double KM_PER_MILE = 1.609344;
        return units == Units.mi ? shortestDistanceMiles : shortestDistanceMiles * KM_PER_MILE;
    }

    public String formatJsonOutput(Store closestStore) throws JsonProcessingException {
        return new ObjectMapper().setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY).writeValueAsString(closestStore);
    }

    public String formatTextOutput(Store closestStore) {
        return String.format(
                "%s\n%s\n%s\n%s, %s %s\n%s\nLatitude: %s\nLongitude: %s\nDistance: %f %s",
                closestStore.name,
                closestStore.location,
                closestStore.address,
                closestStore.city,
                closestStore.state,
                closestStore.zip,
                closestStore.county,
                closestStore.latitude,
                closestStore.longitude,
                closestStore.distance,
                closestStore.units);
    }

    public LatLong convertZipToLatLong(String zip) throws FileNotFoundException {
        LatLong latLong = null;
        for (Zcta zcta : new CsvToBeanBuilder<Zcta>(
                new FileReader("src/2019_Gaz_zcta_national.txt"))
                .withType(Zcta.class)
                .withSeparator('\t')
                .withFilter(strings -> strings[0].equals(zip)).build()) {
            // After we filter in the first line, we break out of the loop to avoid reading the rest of the file.
            latLong = new LatLong();
            latLong.latitude = zcta.latitude.trim();
            latLong.longitude = zcta.longitude.trim();
            break;
        }
        if (latLong == null) {
            throw new RuntimeException("Error: zip not found");
        }
        return latLong;
    }

    public LatLong convertAddressToLatLong(HttpClient httpClient, String address) throws java.io.IOException, InterruptedException {
        String encodedAddress = null;
        try {
            encodedAddress = URLEncoder.encode(address, StandardCharsets.UTF_8.toString());
        } catch (UnsupportedEncodingException ex) {
            throw new RuntimeException("Error: " + ex.getCause());
        }
        HttpRequest request = HttpRequest.newBuilder()
                .GET()
                .uri(URI.create("https://geocoding.geo.census.gov/geocoder/locations/onelineaddress?address=" + encodedAddress + "&benchmark=Public_AR_Census2010&format=json"))
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new RuntimeException("Error: Received http " + response.statusCode());
        }
        final JsonNode jsonNode = new ObjectMapper().readTree(response.body());
        final JsonNode latNode = jsonNode.at("/result/addressMatches/0/coordinates/y");
        final JsonNode longNode = jsonNode.at("/result/addressMatches/0/coordinates/x");
        if (latNode.isMissingNode() || longNode.isMissingNode()) {
            throw new RuntimeException("Error: could not parse address location");
        }
        final LatLong latLong = new LatLong();
        latLong.latitude = latNode.asText();
        latLong.longitude = longNode.asText();
        return latLong;
    }

    public double findDistance(Store store, LatLong originLL) {
        return calculateDistanceMiles(
                Double.parseDouble(originLL.latitude),
                Double.parseDouble(originLL.longitude),
                Double.parseDouble(store.latitude),
                Double.parseDouble(store.longitude));
    }

    public double calculateDistanceMiles(double lat1Degrees, double long1Degrees, double lat2Degrees, double long2Degrees) {
        // Implementation of Law of Cosines.
        double longThetaRadians = convertDegreesToRadians(long2Degrees - long1Degrees);
        double lat1Radians = convertDegreesToRadians(lat1Degrees);
        double lat2Radians = convertDegreesToRadians(lat2Degrees);
        double distanceDegrees =
                convertRadiansToDegrees(
                    Math.acos(
                        Math.sin(lat2Radians) * Math.sin(lat1Radians)
                        + Math.cos(lat2Radians) * Math.cos(lat1Radians) * Math.cos(longThetaRadians)));
        final int MINUTES_PER_DEGREE = 60;
        final double MILES_PER_NAUTICAL_MILE = 1.150779; // Based on http://www.bipm.org/utils/common/pdf/si_brochure_8_en.pdf
        double distanceNauticalMiles = distanceDegrees * MINUTES_PER_DEGREE;
        return (distanceNauticalMiles * MILES_PER_NAUTICAL_MILE);
    }

    private double convertDegreesToRadians(double degrees) {
        return (degrees * Math.PI / 180.0);
    }

    private double convertRadiansToDegrees(double radians) {
        return (radians * 180.0 / Math.PI);
    }
}