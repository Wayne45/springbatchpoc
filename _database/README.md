#### Commands

In the _database folder, run the below

##### Start flyway migration

```
mvn -P local flyway:migrate
```

The below flags can be added to let flyway knows about certain anomalies.

- -Dflyway.outOfOrder=true
- -Dflyway.ignoreMissingMigrations=true

##### Get flyway info

```
mvn -P local flyway:info
```

##### Repair flyway error (use it sparingly)

```
mvn -P local flyway:repair
```
