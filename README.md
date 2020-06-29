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
