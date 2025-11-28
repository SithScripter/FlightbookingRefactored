package com.demo.flightbooking.tests.api;

import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.testng.annotations.Test;

import static org.hamcrest.Matchers.equalTo;

public class SampleApiTest {

    @Test(description = "Perform a sample API test to demonstrate API testing capability.")
    public void sampleApiTest() {
        // Set the base URI for the API
        RestAssured.baseURI = "https://reqres.in/api";

        // Perform a GET request to the /users/2 endpoint
        Response response = RestAssured.given()
                .when()
                .get("/users/2")
                .then()
                .statusCode(200) // Assert that the status code is 200 OK
                .body("data.id", equalTo(2)) // Assert that the user ID in the response is 2
                .body("data.email", equalTo("janet.weaver@reqres.in")) // Assert the email
                .extract().response();

        // Print the response body to the console for verification
        System.out.println("Response Body: " + response.getBody().asString());
    }
}
