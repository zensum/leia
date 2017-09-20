# webhook-service

### What?

A service backened by Ktor, writing incoming HTTP requests to one or serveral Kafka topics.

### Why?

Sometimes a call to a webhook is only received once. Writing a HTTP request to a semi-permanent Kafka log allows for repeated processing.

### How?

Defining a route with the same syntax used for [Ktor routing](http://ktor.io/features/routing.html)

_Example_

```
title = "My routes config"

[[routes]]
        path = "/status/mail"
        topic = "mail"
        verify = false
        format = "proto"
        methods = [ "POST", "PUT", "HEAD", "GET" ]

[[routes]]
        path = "/api/{id}"
        topic = "api_calls"
        format = "raw_body"
        verify = true
```

Routing is saved in a file, and its location is set with the environment varaiable `ROUTES_FILE`. If this is not set it will default to `/etc/config/routes`.

#### Configuration

1. _path_ - is **mandatory**
2. _topic_ - is **mandatory**
3. _verify_ - is **optional**, default is _false_. Decides whether only request with a verified json web token is allowed. Requires [ktor-jwt](http://github.com/zensum/ktor-jwt).
4. _format_ - **optional**, default is _protobuf_.
5. _methods_ - **optional**, default is all seven HTTP verbs.
