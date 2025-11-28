package com.demo.flightbooking.tests.api;

import com.aventstack.extentreports.ExtentTest;
import com.aventstack.extentreports.Status;
import com.demo.flightbooking.tests.base.ApiBaseTest;
import com.demo.flightbooking.tests.base.BaseTest;
import com.demo.flightbooking.utils.ExtentManager;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.testng.annotations.Test;

import static org.hamcrest.Matchers.equalTo;

public class SampleApiTest extends ApiBaseTest {

    @Test(description = "Perform a sample API test to demonstrate API testing capability.")
    public void sampleApiTest() {
        ExtentTest test = ExtentManager.getTest();
        if (test != null) {
            test.log(Status.INFO, "Starting API validation against ReqRes");
        }

        RestAssured.baseURI = "https://reqres.in/api";

        Response response = RestAssured.given()
                .when()
                .get("/users/2")
                .then()
                .statusCode(200)
                .body("data.id", equalTo(2))
                .body("data.email", equalTo("janet.weaver@reqres.in"))
                .extract().response();

        String responseBody = response.getBody().asString();
        System.out.println("Response Body: " + responseBody);

        if (test != null) {
            test.log(Status.PASS, "Verified ReqRes user API response for user 2");
            test.log(Status.INFO, "Payload: " + responseBody);
        }
    }
}
