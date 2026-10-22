
# Smart Taxi Agent

A Java-based reinforcement learning taxi agent with a JavaFX interface.
Course project for **UCSXXX: Enterprise Web Application**.

## Project idea

A taxi learns, by trial and error (Q-learning), to pick up a passenger and
drop them at the destination on a grid city while avoiding walls. The
project demonstrates Core Java, collections, exception handling, file
handling, JDBC with a DAO layer, and JavaFX.

## Team

- Neha Bansal (GitHub: NehaBansal451)
- Teammate 2 (GitHub: TBD)
- Teammate 3 (GitHub: TBD)

## Planned features

- Grid city and OOP model
- Q-learning agent with epsilon-greedy exploration
- Custom exceptions and input validation
- Properties-file configuration and logging
- Save and load a trained agent (serialization)
- JDBC + DAO trip history, leaderboard and replay
- Live training dashboard
- Q-value heatmap with policy arrows
- City map editor
- Fare and earnings simulator

(Status: planning. This list will be updated as features are built.)


## Tech stack

- Java (JDK version to be confirmed)
- Maven
- JavaFX
- SQLite via JDBC
- JUnit

## Setup instructions

To be added once the project skeleton is created.

## Contributing

See CONTRIBUTING.md for our team workflow.

(Test change to practise the pull request workflow.)

## Setup and running

Requirements: JDK 17 or newer and Apache Maven 3.9 or newer. Internet is needed only for the first build.

    git clone https://github.com/NehaBansal451/Smart-taxi-agent.git
    cd Smart-taxi-agent
    mvn clean test
    mvn javafx:run

The app creates data/taxi.db on first run. Trained agents (*.ser), databases and logs are not committed.

## Using the app

- Simulation: watch the Random or Q-learning agent drive a trip. Tick Show heatmap to see learned values and arrows.
- Training Dashboard: choose an episode count, press Train, then Evaluate to compare Random with Q-learning. Save run stores the run in the database.
- History: browse saved runs, sort the leaderboard, edit notes, delete runs and replay a stored trip.

## Project layout

Packages under com.smarttaxi: model, map, env, agent, fare, service, db, storage, config, exception, util, ui.
