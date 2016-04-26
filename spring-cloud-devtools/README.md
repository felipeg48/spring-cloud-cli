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
$ spring devtools
```

Currently starts configserver on port 8888 and then eureka on port 8761.

### Stopping

`Ctrl-C` in the same terminal `spring devtools` was run.

### TODO

Lots!