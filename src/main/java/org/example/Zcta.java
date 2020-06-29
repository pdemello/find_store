package org.example;

import com.opencsv.bean.CsvBindByName;

import java.io.Serializable;

public class Zcta implements Serializable {

    @CsvBindByName(column = "GEOID")
    String zip;

    @CsvBindByName(column = "INTPTLAT")
    String latitude;

    @CsvBindByName(column = "INTPTLONG")
    String longitude;
}
