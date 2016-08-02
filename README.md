[![build status](https://circleci.com/gh/palantir/docker-compose-rule.svg?style=shield&circle-token=ed5bbc06f483e3f7324d1b3440125827c8d355d7)](https://circleci.com/gh/palantir/docker-compose-rule) [ ![Download](https://api.bintray.com/packages/palantir/releases/docker-compose-rule/images/download.svg) ](https://bintray.com/palantir/releases/docker-compose-rule/_latestVersion)

Docker Compose JUnit Rule
=========================

This is a small library for executing JUnit tests that interact with Docker Compose managed containers. It supports the following:

- Starting containers defined in a docker-compose.yml before tests and tearing them down afterwards
- Waiting for services to become available before running tests
- Recording log files from containers to aid debugging test failures

Why should I use this?
----------------------

The code here started out as the end to end tests for one of our products. We needed to test this product in a variety of different configurations and environments which were mutually incompatible, thus multiple Docker Compose files were needed and so a simplistic model of running `docker-compose up` in Gradle was insufficient.

If you're experiencing any of the following using Docker for your testing this library should hopefully help:

- Orchestrating multiple services and mapping the ports to outside the Docker machine so assertions can be made in tests
- Needing to know when services are up to prevent flickering tests caused by slow to start services or complicated service dependencies
- Lack of insight into what has happened in Docker containers during tests on CI servers due to loss of logs
- Tests failing due to needing open ports on the CI build host which conflict with the test configuration

Simple Use
----------

Add a dependency to your project. For example, in gradle:

```groovy
repositories {
    maven {
        url 'https://dl.bintray.com/palantir/releases' // docker-compose-rule is published on bintray
    }
}
dependencies {
    compile 'com.palantir.docker.compose:docker-compose-rule:<latest-tag-from-bintray>'
}
```

For the most basic use simply add a `DockerComposeRule` object as a `@ClassRule` or `@Rule` in a JUnit test class.

```java
public class MyIntegrationTest {

    @ClassRule
    public static DockerComposeRule docker = DockerComposeRule.builder()
            .file("src/test/resources/docker-compose.yml")
            .build();

    @Test
    public void testThatUsesSomeDockerServices() throws InterruptedException, IOException {
       ...
    }

}
```

This will cause the containers defined in `src/test/resources/docker-compose.yml` to be started by Docker Compose before the test executes and then the containers will be killed and removed (along with associated volumes) once the test has finished executing.

The `docker-compose.yml` file is referenced using the path given, relative to the working directory of the test. It will not be copied elsewhere and so references to shared directories and other resources for your containers can be made using path relative to this file as normal. If you wish to manually run the Docker containers for debugging the tests simply run `docker-compose up` in the same directory as the `docker-compose.yml`.

### Running on a Mac

The above example will work out of the box on Linux machines with Docker installed. On Mac you will first need to install Docker using the instructions [here](https://docs.docker.com/v1.8/installation/mac/).

Once Docker is installed to run from the command line you will need to execute `docker-machine env <machine_name>` and follow the instructions to set the environment variables. Any tests can now be executed through Gradle in the usual way.

To run the tests from your IDE you will need to add the environment variables given from running `docker-machine env <machine_name>` to the run configuration for the test in your IDE. This is documented for [Eclipse](http://help.eclipse.org/mars/index.jsp?topic=%2Forg.eclipse.jdt.doc.user%2Ftasks%2Ftasks-java-local-configuration.htm) and [IntelliJ](https://www.jetbrains.com/idea/help/run-debug-configuration-application.html).

Waiting for a service to be available
-------------------------------------

To wait for services to be available before executing tests use the following methods on the `DockerComposeRule` object:

```java
public class MyEndToEndTest {

    @ClassRule
    public static DockerComposeRule docker = DockerComposeRule.builder()
        .file("src/test/resources/docker-compose.yml")
        .waitingForService("db", HealthChecks.toHaveAllPortsOpen())
        .waitingForService("web", HealthChecks.toRespondOverHttp(8080, (port) -> port.inFormat("https://$HOST:$EXTERNAL_PORT")))
        .waitingForService("other", (container) -> customServiceCheck(container), Duration.standardMinutes(2))
        .waitingForServices(ImmutableList.of("node1", "node2"), toBeHealthyAsACluster())
        .waitingForHostNetworkedPort(5432, toBeOpen())
        .build();

    @Test
    public void testThatDependsServicesHavingStarted() throws InterruptedException, IOException {
        ...
    }
}
```

The entrypoint method `waitingForService(String container, HealthCheck<Container> check[, Duration timeout])` will make sure the healthcheck passes for that container before the tests start.
The entrypoint method `waitingForServices(List<String> containers, HealthCheck<List<Container>> check[, Duration timeout])` will make sure the healthcheck passes for the cluster of containers before the tests start.
The entrypoint method `waitingForHostNetworkedPort(int portNumber, HealthCheck<DockerPort> check[, Duration timeout])` will make sure the healthcheck passes for a particular host networked port.

We provide 2 default healthChecks in the HealthChecks class:

1. `toHaveAllPortsOpen` - this waits till all ports can be connected to that are exposed on the container
2. `toRespondOverHttp` - which waits till the specified URL responds to a HTTP request.

Accessing services in containers from outside a container
---------------------------------------------------------

In tests it is likely services inside containers will need to be accessed in order to assert that they are behaving correctly. In addition, when tests run on Mac the Docker contains will be inside a Virtual Box machine and so must be accessed on an external IP address rather than the loopback interface.

It is recommended to only specify internal ports in the `docker-compose.yml` as described in the [https://docs.docker.com/compose/compose-file/#ports](reference). This makes tests independent of the environment on the host machine and of each other.  Docker will then randomly allocate an external port. For example:

```yaml
postgres:
  image: postgres:9.5
  ports:
    - 5432
```

Given a `DockerComposeRule` instance called `docker`, you could then access a service called
`postgres` as follows

```java
DockerPort postgres = docker.containers()
        .container("postgres")
        .port(5432);
```

You could then interpolate the host IP address and random external port as follows:

```java
String url = postgres.inFormat("jdbc:postgresql://$HOST:$EXTERNAL_PORT/mydb");
// e.g. "jdbc:postgresql://192.168.99.100:33045/mydb"
```

Run docker-compose exec
---------------------------------------------------------

We support docker-compose exec command which runs a new command in a running container.


```java
dockerCompose.exec(dockerComposeExecOption, containerName, dockerComposeExecArgument)
```

Just be aware that you need at least docker-compose 1.7 to run docker-compose exec


Collecting logs
---------------

To record the logs from your containers specify a location:

```java
public class DockerComposeRuleTest {

    @ClassRule
    public static DockerComposeRule docker = DockerComposeRule.builder()
            .file("src/test/resources/docker-compose.yml")
            .saveLogsTo("build/dockerLogs/dockerComposeRuleTest")
            .build();

    @Test
    public void testRecordsLogs() throws InterruptedException, IOException {
       ...
    }

}
```

This will automatically record logs for all containers in real time to the specified directory. Collection will stop when the containers terminate.

Skipping shutdown
-----------------

To skip shutdown of containers after tests are finished executing:

```java
public class DockerComposeRuleTest {
    @ClassRule
    public static DockerComposeRule docker = DockerComposeRule.builder()
            .file("src/test/resources/docker-compose.yml")
            .skipShutdown(true)
            .build();
}
```

This can shorten iteration time when services take a long time to start. Remember to never leave it on in CI!

Docker Machine
--------------

Docker is able to connect to daemons that either live on the machine where the client is running, or somewhere remote.
Using the `docker` client, you are able to control which daemon to connect to using the `DOCKER_HOST` environment
variable.

### Local Machine

The default out-of-the-box behaviour will configure `docker-compose` to connect to a Docker daemon that is running
*locally*. That is, if you're on Linux, it will use the Docker daemon that exposes its socket. In the case of Mac OS X -
which doesn't support Docker natively - we have to connect to a technically "remote" (but local) Docker daemon which is
running in a virtual machine via `docker-machine`.

If you're on Mac OS X, the `docker` cli expects the following environment variables:

 - `DOCKER_HOST`
 - If the Docker daemon is secured by TLS, `DOCKER_TLS_VERIFY` and `DOCKER_CERT_PATH` need to be set.

Similarly, if you're using a `LocalMachine`, you need to ensure the Run Configuration (in your IDE, command line etc.)
has those same variables set.

An example of creating a `DockerMachine` that connects to a local docker daemon:

```java
DockerMachine.localMachine()
             .build()
```

### Remote Machine

You may not always want to connect to a Docker daemon that is running on your local computer or a virtual machine
running on your local computer.

An example of this would be running containers in a clustered manner with Docker Swarm. Since Docker Swarm implements
the Docker API, setting the right environment variables would allow us to use Docker containers on the swarm.

An example of connecting to a remote Docker daemon that has also been secured by TLS:

```java
DockerMachine.remoteMachine()
             .host("tcp://remote-docker-host:2376")
             .withTLS("/path/to/cert")
             .build()
```

### Additional Environment Variables

It may also be useful to pass environment variables to the process that will call `docker-compose`.

You can do so in the following manner:

```java
DockerMachine.localMachine()
             .withEnvironmentVariable("SOME_VARIABLE", "SOME_VALUE")
             .build()
```

The variable `SOME_VARIABLE` will be available in the process that calls `docker-compose`, and can be used for Variable Interpolation inside the compose file.

### How to use a `DockerMachine`

When creating a `DockerComposeRule`, a custom `DockerMachine` may be specified. If no `DockerMachine` is specified, `DockerComposeRule` will connect to the local Docker
daemon, similarly to how the `docker` cli works.

```java
private final DockerMachine dockerMachine = DockerMachine.localMachine()
                                                         .withAdditionalEnvironmentVariable("SOME_VARIABLE", "SOME_VALUE")
                                                         .build();

@Rule
DockerComposeRule docker = DockerComposeRule.builder()
            .file("docker-compose.yaml")
            .machine(dockerMachine)
            .build();
```

Composing docker compose files
------------------------------

`docker-compose` (at least as of version 1.5.0) allows us to specify multiple docker-compose files. On the command line, you
can do this with this example command:

    docker-compose -f file1.yml -f file2.yml -f file3.yml

Semantics of how this works is explained here: [Docker compose reference](https://docs.docker.com/compose/reference/overview/)

To use this functionality inside docker-compose-rule, supply a `DockerComposeFiles` object to your `DockerComposeRule` builder:

```java
DockerComposeRule docker = DockerComposeRule.builder()
            .files(DockerComposeFiles.from("file1.yml", "file2.yml"))
            .build()
```

Using a custom version of docker-compose
---------------

docker-compose-rule tries to use the docker-compose binary located at `/usr/local/bin/docker-compose`. This can be overriden by setting `DOCKER_COMPOSE_LOCATION` to be the path to a valid file.
