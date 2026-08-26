# Third-party API - BE Home Assignment

## Overview

The purpose of the assignment is to assess object oriented analysis and modelling skills, Java coding skills, code structuring and API design.

Take your time on the task, but don't get too carried away. If you submit a solution that is in any way incomplete, the parts that you decided to focus on are relevant.

Keeping the objective in mind, you are free to use whatever tools, libraries, frameworks at your disposal. Artificial Intelligence (AI) usage is encouraged. Please include a README in any format how to run and use the program.

## Requirements

Your task is to write a Formula 1 betting backend application that will expose a REST HTTP API. The API should expose operations:

- To list F1 events.
- To place a bet.
- To simulate event outcome.

The details of these Use Cases can be found in the next page. There are no further defined requirements for the API, it is up to you to design and implement the necessary code in order for the API to support the mentioned operations above. This refers to the entire flow starting from the API, down to the persistence layer.

It is expected for you to spend around 90 minutes to complete the exercise.

## Delivery

- The solution needs to be 100% executable
- Provide a link to the GitHub repository where your solution is committed
    - Please make sure it is not private or restricted
- Provide a README file (documentation) how to run and use the solution

---

## Formula 1 Betting Service

We want to launch a new backend F1 betting service and it will be implemented from scratch. Here you have the relevant Use Cases to support:

### 1. View List of Formula 1 Events

a. The User queries the list of F1 events which can:
i. Be filtered by:
1. Session Type
2. Year
3. Country
b. The System gets the F1 events from the open-source API [here](https://openf1.org/) and:
i. Returns the relevant data to the User. The events are called Sessions inside that API.
ii. It also returns the Driver Market of each event:
1. The full name of the Driver.
2. The ID Number of the Driver.
3. The odds when placing a bet for this driver to win the F1 event.
a. For simplicity, value can only be **2**, **3** or **4**.
b. Always return a random integer between these 3 values.

### 2. Place a bet

a. The User places a Single Bet for the driver he/she thinks will win the specified F1 event.
i. For simplicity, let's say the User can place bets in any F1 event from the past so the API shared before can be used.
ii. The User specifies the amount to bet in EUR.
b. The System places the bet for the User and updates the User Balance.

### 3. Event outcome

a. The System receives a request for a F1 Event that has been finished.
i. We get the ID of the event and the ID of the driver that won.
b. The System saves the outcome.
c. The System checks which bets have won and which ones are lost and updates their status.
d. If a given bet was won, the System calculates the prize for the User.
e. The System adds the won money to each User Balance.

## Conditions

- For simplicity, the User is already registered. Consider each user has an associated User ID you pass as parameter.
- For simplicity, the User cannot deposit or withdraw money. They can only play with 100 EUR given as a gift during the registration stage.
- We will add new F1 event provider API's in future. Ensure your solution is not coupled with the API [here](https://openf1.org/).
