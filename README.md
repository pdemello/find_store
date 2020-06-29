# find_store

find_store finds the nearest store to a given address or zip.

## Getting Started

### Prerequisites

- Maven (mvn)
- java sdk

### Installing

To build such that you can easily run from the command line:

```
mvn compile assembly:single
```

## Running the tests

```
mvn test
```

## Running the app

After building, you can run the convenience script (assuming it has executable permission) like the following examples:

```
./find_store --zip=94111
```
```
./find_store --address="1 Dr Carlton B Goodlett Pl, San Francisco, CA 94102" --output=json --units=km
```
or even just
```
./find_store
```

## How it works

find_store requires either a zip or an address.  If a zip is provided, the app will lookup the zip in the [2019 US Census ZCTA (Zip Code Tabulation Areas) file](https://www2.census.gov/geo/docs/maps-data/data/gazetteer/2019_Gazetteer/2019_Gaz_zcta_national.zip) in order to find the corresponding area's [centroid](https://en.wikipedia.org/wiki/Centroid) latitude/longitude coordinates. If instead an address is provided, the app will lookup the address using the [US Census Geocoding API](https://geocoding.geo.census.gov/geocoder/), which should provide the latitude/longitude for that address.

Once the zip or address has been mapped to coordinates, the app will attempt to find the nearest store to those coordinates, according to the list of stores in [store-locations.csv](src/store-locations.csv). If a store is found successfully, the app will output the store info as well as the distance from the provided zip or address.

To find the distance, the app attempts to use the [Spherical Law of Cosines](https://en.wikipedia.org/wiki/Spherical_law_of_cosines), where a sphere serves an approximation for the form of Earth (which in reality is an oblate spheroid).

Output defaults to human-readable format, and the unit of distance defaults to miles.  To output json, add `--output=json` to the command line.  To change the distance's units to kilometers, add `--units=km`

The app uses the following external libraries:
- [picocli](https://picocli.info/): command line parsing
- [openCSV](http://opencsv.sourceforge.net/): parsing files
- [Jackson](https://github.com/FasterXML/jackson): parsing to and from Json

## Caveats, future improvements, and considerations

- Consider property-based tests for verifying the distance calculation between two latitude/longitude coordinates.
- Consider different distant formulas such as [Haversine](https://en.wikipedia.org/wiki/Haversine_formula) or [Vincenty](https://en.wikipedia.org/wiki/Vincenty%27s_formulae), to optimize the accuracy and loss-of-precision in the distance calculation.
- Experiment with various geocoding services, not just the Census, as the Census can be updated as long as once per decade.
- The app requires zips to be 5 characters (requires leading zeroes for older, shorter zips).
- Tests need to be written for the CLI argument processing.
- Methods using Http could be tested more effectively my mocking the Http.  At the moment, the tests pass a real Http connection to the methods under test.
- The [Store class](src/main/java/org/example/Store.java) should be refactored to remove the `distance` and `units` fields.  These are not "store information", but having them in the class simplifies outputting Json.
- The lists of stores and Zip Code Tabulation Areas are simply files. The app would theoretically be faster and more scalable by preprocessing these files into a database, with appropriate indexing, prior to running the app.
- Consider geospatial database formats such as [Quadtrees](https://en.wikipedia.org/wiki/Quadtree)
- Searching the list of stores is at the moment an O(n) "full scan". All stores will be checked for their distance until a "minimum distance" is found, as the file is not (apparently) sorted in any way.
- Searching the list of Zip Code Tabulation Areas is at the moment O(n), but the file is sorted numerically by zip. The app takes advantage of this and ceases to scan the file once a zip code is found.
- If the app is changed to allow multiple searches in one invocation, consider using batch geolocation calls provided by the Census API, or other APIs, otherwise the app will be very slow due to the latency of each individual API call, even if the calls are done asynchronously.
- If the number of stores goes up by an order of magnitude, this would escalate the need to smartly keep the stores in a database.
- Some zip codes do not have a corresponding Zip Code Tabulation Area. Therefore in these cases, the zip lookup should fail with an error message.
