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

find_store requires either a zip or an address.  If a zip is provided, the app will lookup the zip in the [2019 US Census ZCTA (Zip Code Tabulation Areas) file](https://www2.census.gov/geo/docs/maps-data/data/gazetteer/2019_Gazetteer/2019_Gaz_zcta_national.zip) in order to find the corresponding area's centroid latitude/longitude coordinates. If instead an address is provided, the app will lookup the address using the [US Census Geocoding API](https://geocoding.geo.census.gov/geocoder/), which should provide the latitude/longitude for that address.

Once the zip or address has been mapped to coordinates, the app will attempt to find the nearest store to those coordinates, according to the list of stores in [store-locations.csv](src/store-locations.csv). If a store is found successfully, the app will output the store info as well as the distance from the provided zip or address.

Output defaults to human-readable format, and the unit of distance defaults to miles.  To output json, add `--output=json` to the command line.  To change the distance's units to kilometers, add `--units=km`

The app uses the following external libraries:
- [picocli](https://picocli.info/): command line parsing
- [openCSV](http://opencsv.sourceforge.net/): parsing files
- [Jackson](https://github.com/FasterXML/jackson): parsing to and from Json




## Caveats

## Future improvements / considerations
