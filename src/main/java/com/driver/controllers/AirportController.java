package com.driver.controllers;


import com.driver.model.Airport;
import com.driver.model.City;
import com.driver.model.Flight;
import com.driver.model.Passenger;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
public class AirportController {
    private Map<String, Airport> airportMap = new HashMap<>();
    private Map<Integer, Flight> flightMap = new HashMap<>();
    private Map<Integer, Passenger> passengerMap = new HashMap<>();
    private Map<Integer, Set<Integer>> flightBookings = new HashMap<>();
    @PostMapping("/add_airport")
    public String addAirport(@RequestBody Airport airport){

        //Simply add airport details to your database
        //Return a String message "SUCCESS"
        airportMap.put(airport.getAirportName(), airport);
        return "SUCCESS";
    }

    @GetMapping("/get-largest-aiport")
    public String getLargestAirportName(){

        //Largest airport is in terms of terminals. 3 terminal airport is larger than 2 terminal airport
        //Incase of a tie return the Lexicographically smallest airportName
        return airportMap.values().stream()
                .max(Comparator.comparingInt(Airport::getNoOfTerminals)
                        .thenComparing(Airport::getAirportName))
                .map(Airport::getAirportName)
                .orElse(null);


        //return null;
    }

    @GetMapping("/get-shortest-time-travel-between-cities")
    public double getShortestDurationOfPossibleBetweenTwoCities(@RequestParam("fromCity") City fromCity, @RequestParam("toCity")City toCity){

        //Find the duration by finding the shortest flight that connects these 2 cities directly
        //If there is no direct flight between 2 cities return -1.
        return flightMap.values().stream()
                .filter(flight -> flight.getFromCity().equals(fromCity) && flight.getToCity().equals(toCity))
                .mapToDouble(Flight::getDuration)
                .min()
                .orElse(-1);

    }

    @GetMapping("/get-number-of-people-on-airport-on/{date}")
    public int getNumberOfPeopleOn(@PathVariable("date") Date date,@RequestParam("airportName")String airportName){

        //Calculate the total number of people who have flights on that day on a particular airport
        //This includes both the people who have come for a flight and who have landed on an airport after their flight
        int count = 0;
        for (Flight flight : flightMap.values()) {
            if (flight.getFlightDate().equals(date)) {
                if (airportMap.containsKey(airportName)) {
                    if (flight.getFromCity().equals(airportMap.get(airportName).getCity()) ||
                            flight.getToCity().equals(airportMap.get(airportName).getCity())) {
                        count += flightBookings.getOrDefault(flight.getFlightId(), Collections.emptySet()).size();
                    }
                }
            }
        }
        return count;

    }

    @GetMapping("/calculate-fare")
    public int calculateFlightFare(@RequestParam("flightId")Integer flightId){

        //Calculation of flight prices is a function of number of people who have booked the flight already.
        //Price for any flight will be : 3000 + noOfPeopleWhoHaveAlreadyBooked*50
        //Suppose if 2 people have booked the flight already : the price of flight for the third person will be 3000 + 2*50 = 3100
        //This will not include the current person who is trying to book, he might also be just checking price
        Set<Integer> bookedPassengers = flightBookings.getOrDefault(flightId, new HashSet<>());
        return 3000 + bookedPassengers.size() * 50;


    }


    @PostMapping("/book-a-ticket")
    public String bookATicket(@RequestParam("flightId")Integer flightId,@RequestParam("passengerId")Integer passengerId) {

        //If the numberOfPassengers who have booked the flight is greater than : maxCapacity, in that case :
        //return a String "FAILURE"
        //Also if the passenger has already booked a flight then also return "FAILURE".
        //else if you are able to book a ticket then return "SUCCESS"

        Flight flight = flightMap.get(flightId);
        if (flight == null || !passengerMap.containsKey(passengerId)) {
            return "FAILURE";
        }

        Set<Integer> bookedPassengers = flightBookings.getOrDefault(flightId, new HashSet<>());
        if (bookedPassengers.size() >= flight.getMaxCapacity() || bookedPassengers.contains(passengerId)) {
            return "FAILURE";
        }

        bookedPassengers.add(passengerId);
        flightBookings.put(flightId, bookedPassengers);
        return "SUCCESS";
    }

    @PutMapping("/cancel-a-ticket")
    public String cancelATicket(@RequestParam("flightId")Integer flightId,@RequestParam("passengerId")Integer passengerId){

        //If the passenger has not booked a ticket for that flight or the flightId is invalid or in any other failure case
        // then return a "FAILURE" message
        // Otherwise return a "SUCCESS" message
        // and also cancel the ticket that passenger had booked earlier on the given flightId
        Set<Integer> bookedPassengers = flightBookings.get(flightId);
        if (bookedPassengers == null || !bookedPassengers.contains(passengerId)) {
            return "FAILURE";
        }

        bookedPassengers.remove(passengerId);
        return "SUCCESS";
       //return null;
    }


    @GetMapping("/get-count-of-bookings-done-by-a-passenger/{passengerId}")
    public int countOfBookingsDoneByPassengerAllCombined(@PathVariable("passengerId")Integer passengerId){

        //Tell the count of flight bookings done by a passenger: This will tell the total count of flight bookings done by a passenger :
        return (int) flightBookings.values().stream()
                .filter(bookings -> bookings.contains(passengerId))
                .count();
        //return 0;
    }

    @PostMapping("/add-flight")
    public String addFlight(@RequestBody Flight flight){

        //Return a "SUCCESS" message string after adding a flight.
        flightMap.put(flight.getFlightId(), flight);
        return "SUCCESS";
    }


    @GetMapping("/get-aiportName-from-flight-takeoff/{flightId}")
    public String getAirportNameFromFlightId(@PathVariable("flightId")Integer flightId){

        //We need to get the starting airportName from where the flight will be taking off (Hint think of City variable if that can be of some use)
        //return null incase the flightId is invalid or you are not able to find the airportName

        Flight flight = flightMap.get(flightId);
        if (flight == null) {
            return null;
        }

        for (Airport airport : airportMap.values()) {
            if (airport.getCity().equals(flight.getFromCity())) {
                return airport.getAirportName();
            }
        }
        return null;
    }


    @GetMapping("/calculate-revenue-collected/{flightId}")
    public int calculateRevenueOfAFlight(@PathVariable("flightId")Integer flightId){

        //Calculate the total revenue that a flight could have
        //That is of all the passengers that have booked a flight till now and then calculate the revenue
        //Revenue will also decrease if some passenger cancels the flight


        Set<Integer> bookedPassengers = flightBookings.getOrDefault(flightId, Collections.emptySet());
        return bookedPassengers.size() * 3000 + bookedPassengers.stream().mapToInt(p -> (bookedPassengers.size() - 1) * 50).sum();

    }


    @PostMapping("/add-passenger")
    public String addPassenger(@RequestBody Passenger passenger){

        //Add a passenger to the database
        //And return a "SUCCESS" message if the passenger has been added successfully.

            passengerMap.put(passenger.getPassengerId(), passenger);
            return "SUCCESS";
      // return null;
    }


}
