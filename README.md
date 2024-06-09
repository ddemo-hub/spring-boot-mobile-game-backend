# Backend Engineering Case Study

## Prerequisites
- **Running with Docker**
    - Docker 24.x
    - Docker Compose 2.x
- **Running with Maven**
    - JDK 17+
    - Apache Maven 3.x
    - MySQL 8.x
    - Redis 7.x

## Configuration
- __src__
    - __main__
	    - __recources__
		    - __application.properties__ *-> App level configs (logger, scheduler, cache)* 
- __.env__ *-> Secret key and MySQL & Redis connection configs*
   



## How to Run
- **Using Docker**
    - Configure the ```.env``` and ```application.properties``` files    
    - Execute ```docker compose up```

- **Using Docker**
    - Configure the ```application-dev.properties``` and ```application.properties``` files 
    - Execute ```mvn clean install``` to build the application
    - Execute ```mvn spring-boot:run``` to run the application

## System Design
The system has three components
- **Redis:** caching and non-persistent storage.
- **MySQL:** persistent storage and part of the business logic.
- **Spring Boot Application:** The core business logic provider.

### MySQL
The MySQL database contains 3 tables and 9 triggers. 

The tables are:
1. **user:** Holds user information.
2. **tournament_group:** Records group IDs and their formation dates, so that the active tournament and the past tournaments can co-exist.
3. **user_in_tournament:** Matches users with groups and tournaments as well as holding group-specific data. Used for building *country and group leaderboards*, calculating *user ranks* and managing *rewards*.

The SQL triggers are used to shift some of the business logic from the Spring Boot application to the DBMS to ease management and optimize performance. The triggers that provide business logic are as follows:
1. **set_random_country:** Randomly assign a country to a user if not specified.
2. **update_tournament_score:** After users pass a level, increment their scores in their tournament groups if they are participating in an active tournament.
3. **update_coins_on_reward_claim:** When the is_reward_claimed field is set to true, update the user's coins by the reward amount.

The remaining triggers act as safety measures that block illegal actions (such as creating a new group after 20.00 UTC)

### Spring Boot Application
The Spring Boot Application can be broken down to three components that operate semi independently and on parallel.

1. The **Main Component** is the backbone of the application that responds to the requests sent by the users. The details regarding the endpoints can be studied in ```Backend Engineering Case Study.postman_collection.json```. The business logic is implemented through the following services:
    - **SecurityService**: Since user IDs are not secret and can be viewed in group leaderboards (it is assumed that this backend directly communicates with the end user), additional security measures are needed to make sure that players cannot make send requests using other players' IDs. Hence; when a new user is created, a JWT token that signs the user's data is sent back to the client and that token is excepted to be received in the HTTP's Authorization header in every request that requires authorization.
    - **RedisService:** Controls the operations performed on Redis and sets/resets the TTLs for the cached data based on the player's activity.
    - **UserService:** Handles calculations and operations related to user data.
    - **TournamentService:** Handles calculations and operations related to tournament data.
2. **Tournament Scheduler** is responsible for ending the old and starting the new tournament by distributing players their rewards and clearing the cache. Furthermore; in case of a system failure, this component loads the cache with the data queried from the persistent storage.
3. **Group Formation Scheduler** forms new tournament groups by periodically checking the the country waiting queues to see if there is at least one player from every country in the queue. Then, it updates the cache and the persistent storage to create the new group.

### Redis
Redis is used to cache data for the active tournament. When the Spring Boot application starts, the cache is loaded with the data regarding the active tournament (country & group leaderboards and user-group matchings) by querying the persistent storage. The TTL durations of each resource is resetted at every operation on that resource. Hence, only the information of the players who are currently playing the game are stored in memory.

The following data structures are utilized by Redis:

1. Sorted Sets:
    - **Country Leaderboard:** The country leaderboard of the active tournament is *cached* in the form of a sorted set where the country's name is the key and its score is the value. The country leaderboard lives in Redis throughout the session since its size is neglectable and does not scale. 
    - **Country Waiting Queues:** When a users make requests to enter the active tournament, they are placed into the queues of their corresponding countries. The key is the user's ID and the value is the timestamp of their entry, hence the sorted set acts as a FIFO queue. The data stored in the country waiting queues are *not saved in the persistent storage* and only exist in the memory.
2. Hashes: 
    - **Group Leaderboards:** Group leaderboards for the active tournament  are *cached* in Redis in the forms of hashes where the hash key is the group ID, and the fields are the group-specific leaderboard data.
3. Key-Value pairs: The key-value pairs of **(user ID, group ID)** are *cached* in Redis for improved query performance on the active tournament. 