package org.example;

import com.opencsv.bean.CsvBindByName;

import java.io.Serializable;

public class Store implements Serializable {

    @CsvBindByName(column = "Store Name")
    String name;

    @CsvBindByName(column = "Store Location")
    String location;

    @CsvBindByName(column = "Address")
    String address;

    @CsvBindByName(column = "City")
    String city;

    @CsvBindByName(column = "State")
    String state;

    @CsvBindByName(column = "Zip Code")
    String zip;

    @CsvBindByName(column = "Latitude")
    String latitude;

    @CsvBindByName(column = "Longitude")
    String longitude;

    @CsvBindByName(column = "County")
    String county;

    double distance;
    String units;
}
