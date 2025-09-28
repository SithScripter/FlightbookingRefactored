package com.demo.flightbooking.model;

/**
 * A data model representing a passenger and their booking details.
 * This is a Java Record, which is a modern, concise way to create immutable
 * data-carrier classes, reducing boilerplate code for constructors, getters,
 * equals(), hashCode(), and toString().
 */
public record Passenger(
    String origin,
    String destination,
    String firstName,
    String lastName,
    String address,
    String city,
    String state,
    String zipCode,
    String cardType,
    String cardNumber,
    String month,
    String year,
    String cardName,
    int age,
    String gender
) {}