# webhook-service

### What?

A service backened by Ktor, writing incoming HTTP requests to one or serveral Kafka topics.

### Why?

Sometimes a call to a webhook is only received once. Writing a HTTP request to a semi-permanent Kafka log allows for repeated processing.

### How?

Defining a route with the same syntax used for [Ktor routing](http://ktor.io/features/routing.html) with `/path/to/resource -> kafka_topic`

_Example_

```
/test -> test
/status/mail -> mail
/status/sms -> sms
/api/{id} -> api_calls
```

Routing is saved in a file, and its location is set with the environment varaiable `ROUTES_FILE`.
