package com.demo.flightbooking.utils;

import com.demo.flightbooking.model.Passenger;
import org.testng.annotations.DataProvider;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/**
 * Provides test data to TestNG tests by reading from a CSV file.
 * This class demonstrates a data-driven approach using CSV as the data source.
 */
public class CsvDataProvider {

//    private static final String CSV_FILE = "testdata/passenger-data.csv";
	private static final String CSV_FILE = ConfigReader.getProperty("data.file.passengers.csv");

    /**
     * TestNG DataProvider method that reads passenger data from a CSV file.
     * It skips the header row and converts each subsequent row into a Passenger object.
     *
     * @return A 2D Object array where each inner array contains a single Passenger object.
     */
    @DataProvider(name = "passengerCsvData")
    public Object[][] provideCsvData() throws Exception {
        List<Passenger> passengerList = new ArrayList<>();

        // This part remains the same: reading the file and creating a list of Passenger records
        InputStream is = getClass().getClassLoader().getResourceAsStream(CSV_FILE);
        if (is == null) {
            throw new RuntimeException("CSV file not found on classpath: " + CSV_FILE);
        }

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
            String line;
            boolean skipHeader = true;

            while ((line = reader.readLine()) != null) {
                if (skipHeader) {
                    skipHeader = false;
                    continue;
                }

                String[] fields = line.split(",", -1);
                if (fields.length < 15) {
                    throw new IllegalArgumentException("CSV row has insufficient columns: " + line + " (expected at least 15, found " + fields.length + ")");
                }

                Passenger passenger = new Passenger(
                    fields[0].trim(),  // origin
                    fields[1].trim(),  // destination
                    fields[2].trim(),  // firstName
                    fields[3].trim(),  // lastName
                    fields[4].trim(),  // address
                    fields[5].trim(),  // city
                    fields[6].trim(),  // state
                    fields[7].trim(),  // zipCode
                    fields[8].trim(),  // cardType
                    fields[9].trim(),  // cardNumber
                    fields[10].trim(), // month
                    fields[11].trim(), // year
                    fields[12].trim(), // cardName
                    Integer.parseInt(fields[13].trim()), // age
                    fields[14].trim()  // gender
                );

                passengerList.add(passenger);
            }
        }

        // --- CHANGE: From a 'for' loop to a Java Stream ---
        // The old 'for' loop that converted the List into Object[][] has been replaced.
        // This new approach is more declarative and concise.

        return passengerList.stream()                // 1. Create a stream of Passenger objects.
            .map(passenger -> new Object[]{passenger})  // 2. For each passenger, transform it into a new Object array containing just that passenger.
            .toArray(Object[][]::new);                 // 3. Collect all the Object arrays into a final 2D Object array that TestNG can use.
    }
}