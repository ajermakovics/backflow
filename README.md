# Backflow

A load balancing reverse proxy server with an API. Supports adding/removing backends on-the-fly without restarts. 

# Install

Download from [releases](https://github.com/ajermakovics/backflow/releases) and unzip

# Configuration

Edit proxy.yml

```yml
server:
  port: 8000  # proxy listen port
  backends: http://127.0.0.1:8090 http://127.0.0.1:8091  # List of hosts to forward requests to (in round-robin)
  maxRequestTime: 30000  # -1 to disable
  ioThread: 4
  backlog: 1000
  rewriteHostHeader: false
  reuseXForwarded: true
  connectionsPerThread: 20
  workerThreads: 16 # default: Runtime.getRuntime().availableProcessors()*8
  workerTaskMaxThreads : 16 # default: workerThreads
  sslPort: 8443
  keystore: /path/to/keystore.jks
  keystorePassword: secret

# Users for accesing the API
users:
  admin: secretPassword
```

All requests incoming on port 8000 will be forwarded to one of the backends.

# Running

`./bin/backflow`

# API

Backflow has a HTTP based API to add/remove backends.
Changes are applied dynamically without restarting the server.

## Add a new backend

`curl -uadmin:secretPassword http://localhost:8000/lb -XPOST -d 'http://new.backend.example.com'`

## Remove an existing

`curl -uadmin:secretPassword http://localhost:8000/lb -XDELETE -d 'http://some.backend.example.com'`

## List current backends

`curl -uadmin:secretPassword http://localhost:8000/lb`

# Build from source

`./gradlew distZip`

# License

Apache 2.0
