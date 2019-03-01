# leia

## What?

A service backed by Ktor, writing incoming HTTP requests to one or serveral topics of one or more message brokers ([Kafka](https://kafka.apache.org/), [Redis](https://redis.io/topics/pubsub) or [Cloud Pub/Sub](https://cloud.google.com/pubsub/)).

## Why?

Sometimes a call to a webhook is only received once. Writing a HTTP request to a semi-permanent message broker log allows for repeated processing.

## How?

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
        validateJson = true
        jsonSchema = """
<put your JSON schema here>
"""
```

Routing is saved in files with `.toml` extension, and their location is set with the environment varaiable `CONFIG_DIRECTORY`. If this is not set it will default to `/etc/config/`.

### Configuration

Configuration consists of few tables:
 - [routes](#routes)
 - [sink providers](#sink-providers)
 - [auth providers](#auth-providers)

Configuration is loaded on startup and then refreshed every 1 minute.

It can be retrieved either from a file or from kubernetes custom resources.

#### Configuration in files

Configuration in a file is stored in [TOML](https://github.com/toml-lang/toml) file format. See example configuration [here](example-config/cfg.toml). You can have several configuration files stored in `/etc/config/` or in path defined in `CONFIG_DIRECTORY` environment variable.

#### Configuration in kubernetes

Configuration in kubernetes requires creating [custom resource defitnions](https://kubernetes.io/docs/tasks/access-kubernetes-api/custom-resources/custom-resource-definitions/) first:
```
kubectl create -f leiaroute_crd.yaml
kubectl create -f leiasinkprov_crd.yaml
```
Then add your objects:
```
kubectl create -f object1.yaml
...
```
Custom resource definitions and sample object definitions [YAML](https://en.wikipedia.org/wiki/YAML) files can be found [here](src/integrationTest).

### Routes

Route defines where given request should be sent to.

 - `path` - is **mandatory**, HTTP URL path to match
 - `topic` - is **mandatory**, topic where the message will be sent
 - `sink` - is optional, name of sink where message will be sent, if not provided uses default sink.
 - `verify` - is optional, default is `false`. Decides whether only request with a verified json web token is allowed. Requires [ktor-jwt](http://github.com/zensum/ktor-jwt).
 - `format` - is optional, default is `proto` a format using [protobuf](https://developers.google.com/protocol-buffers/) defined in [zensum/webhook-proto](https://github.com/zensum/webhook-proto). Also available is `raw_body` - writes HTTP body as is.
 - `methods` - is optional, default is all seven HTTP verbs.
 - `response` - is optional, HTTP response code which should be sent to the sender on success, default is `204`.
 - `validateJson` - is optional, wether to validate body of the request as JSON before sending, default is `false`. `204` code is returned on success, `400` code on error.
 - `jsonSchema` - is optional, validates request body against this JSON schema if `validateJson` is set to `true`
 - `cors` - is optional, list of hosts to check incoming request's [origin](https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Origin) against for [CORS](https://developer.mozilla.org/en-US/docs/Web/HTTP/CORS). By default all traffic is allowed. If contains `*` then all hosts are allowed. When non empty, `OPTIONS` method is allowed implicitly.
 - `authenticateUsing` - is optional, list of [auth providers](#auth-providers) to verify request against


### Sink providers

Leia needs at least one sink configured and marked as default.

- `name` - is **mandatory**, name to identify the sink in [routes](#routes) configuration `sink` field
- `type` - is optional, one of these types: `kafka`, `redis`, `gpubsub`, `null`, default is `kafka`
- `isDefault` - is optional, must be set to `true` for one sink, default is `false`
- `options` - is optional, additional options to configure sink provider, it is a list of key/value pairs

For testing purposes you can use `null` sink type which does not forward messages and only logs them.

#### Options

##### Kafka
To set [kafka](https://kafka.apache.org/) hostname and port use following option in configuration:
```
host = "<hostname>:<port>"
```

##### Redis
To set up hostname and port for [redis Pub/Sub](https://redis.io/topics/pubsub) use following options in configuration:
```
host = "<hostname>"
port = "<port>"
```

##### Cloud Pub/Sub
To use other project than the default for [Cloud Pub/Sub](https://cloud.google.com/pubsub/) one use following option in configuration:
```
projectId = "<your-project-id>"
```

Before writing to sinks in Cloud Pub/Sub you need to create them first:
```
gcloud pubsub topics list-subscriptions --project <your-project-id> <topic>
```
`GOOGLE_APPLICATION_CREDENTIALS` environment variable needs to point to your cloud service account key in json file. [Create service account key](https://console.cloud.google.com/apis/credentials/serviceaccountkey).

### Auth providers

This table is optional in configuration.

- `name` - is **mandatory**, name to identify the auth provider in [routes](#routes) configuration `authenticateUsing` field
- `type` - is optional, one of these types: `jwk`, `basic_auth`, `no_auth`, default is `no_auth`
- `options` - is optional, additional options to configure auth provider, it is a list of key/value pairs

#### Basic_auth
Option `basic_auth_users` is a map of users and passwords.

#### Jwk
Option `jwk_config`is **mandatory**, it is a map of key/values. The map must contain `jwk_url` and `jwk_issuer` keys.

### Environment variables

- `CONFIG_DIRECTORY` - location of toml files (default `/etc/config/`)
- `PORT` - port on which leia listens for requests (default `80`)
- `KUBERNETES_SERVICE_HOST` - hostname of kubernetes API (default `localhost`)
- `KUBERNETES_SERVICE_PORT` - port of kubernetes API (default `8080`)
- `KUBERNETES_ENABLE` - wether to read configuration from Kubernetes custom resources (default `true`). To disable set it to `false`.

