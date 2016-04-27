## Spring Cloud Devtools

### Building

`./mvnw clean install` from parent directory

### Installing

Install Spring CLI first: https://github.com/spring-cloud/spring-cloud-cli/blob/master/docs/src/main/asciidoc/install.adoc

```
$ spring install org.springframework.cloud:spring-cloud-cli:1.1.0.BUILD-SNAPSHOT
```

### Running

```
$ spring cloud
```

Currently starts configserver on port 8888 and then eureka on port 8761.

### Configuring

Spring Cloud Devtools use normal Spring Boot configuration mechanisms. The config name is `cloud`, so configuration can go in `cloud.yml` or `cloud.properties`.

For example, to run configserver and eureka, create a `cloud.yml` that looks like:
```
spring:
  cloud:
    devtools:
      to-deploy:
        - name: configserver
          coordinates: org.springframework.cloud.devtools:spring-cloud-devtools-configserver:1.1.0.BUILD-SNAPSHOT
          port: 8888
          waitUntilStarted: true
          order: -10
        - name: eureka
          coordinates: org.springframework.cloud.devtools:spring-cloud-devtools-eureka:1.1.0.BUILD-SNAPSHOT
          port: 8761
```

The `name` attribute is optional. If `waitUntilStarted` is true, Devtools will block until the application has reached the `deployed` state. Before commands are deployed, the list is sorted using Spring's `OrderComparator`. In the above case, `configserver` is deployed before any other app is deployed.

### Stopping

`Ctrl-C` in the same terminal `spring cloud` was run.

### TODO

Lots!