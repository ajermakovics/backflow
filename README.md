# Backflow

A load balancing reverse proxy server

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

```

# Running

`./bin/backflow`

# Build from source

`./gradlew distZip`

# License

Apache 2.0
