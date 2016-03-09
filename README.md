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

For the most basic use simply add a `DockerComposition` object as a `@ClassRule` or `@Rule` in a JUnit test class.

    public class DockerCompositionTest {
  
        @ClassRule
        public DockerComposition composition = new DockerComposition("src/test/resources/docker-compose.yml");
  
        @Test
        public void testThatDependsOnDockerComposition() throws InterruptedException, IOException {
           ...
        }
  
    }

This will cause the containers defined in `src/test/resources/docker-compose.yml` to be started by Docker Compose before the test executes and then the containers will be killed once the test has finished executing.

The `docker-compose.yml` file is referenced using the path given, relative to the working directory of the test. It will not be copied elsewhere and so references to shared directories and other resources for your containers can be made using path relative to this file as normal. If you wish to manually run the Docker containers for debugging the tests simply run `docker-compose up` in the same directory as the `docker-compose.yml`.

### Running on a Mac

The above example will work out of the box on Linux machines with Docker installed. On Mac you will first need to install Docker using the instructions [here](https://docs.docker.com/v1.8/installation/mac/).

Once Docker is installed to run from the command line you will need to execute `docker-machine env <machine_name>` and follow the instructions to set the environment variables. Any tests can now be executed through Gradle in the usual way.

To run the tests from your IDE you will need to add the environment variables given from running `docker-machine env <machine_name>` to the run configuration for the test in your IDE. This is documented for [Eclipse](http://help.eclipse.org/mars/index.jsp?topic=%2Forg.eclipse.jdt.doc.user%2Ftasks%2Ftasks-java-local-configuration.htm) and [IntelliJ](https://www.jetbrains.com/idea/help/run-debug-configuration-application.html).

Waiting for a service to be available
-------------------------------------

To wait for services to be available before executing tests use the following methods on the DockerComposition object:

    public class DockerCompositionTest {

        @ClassRule
        public DockerComposition composition = new DockerComposition("src/test/resources/docker-compose.yml")
            .waitingForService("db", toHaveAllPortsOpen())
            .waitingForService("web", toRespondOverHttp(8080, (port) -> "https://" + port.getIp() + ":" + port.getExternalPort()))
            .waitingForService("other", (container) -> customServiceCheck(container), Duration.standardMinutes(2))

        @Test
        public void testThatDependsServicesHavingStarted() throws InterruptedException, IOException {
            ...
        }

The entrypoint method `waitingForService(String container, HealthCheck check[, Duration timeout])` will make sure the healthcheck passes for that container before the tests start. We provide 2 default healthChecks in the HealthChecks class:

1. `toHaveAllPortsOpen` - this waits till all ports can be connected to that are exposed on the container
2. `toRespondOverHttp` - which waits till the specified URL responds to a HTTP request.

Accessing services in containers from outside a container
---------------------------------------------------------

In tests it is likely services inside containers will need to be accessed in order to assert that they are behaving correctly. In addition, when tests run on Mac the Docker contains will be inside a Virtual Box machine and so must be accessed on an external IP address rather than the loopback interface.

It is recommended to only specify internal ports in the `docker-compose.yml` as described in the [https://docs.docker.com/compose/compose-file/#ports](reference). This makes tests independent of the environment on the host machine and of each other.

There are then two methods for accessing port information:

    DockerPort portOnContainerWithExternalMapping(String container, int portNumber)

    DockerPort portOnContainerWithInternalMapping(String container, int portNumber)

In both cases the port in the Docker compose file must be referenced. Using the latter method no external port needs to be declared, this will be allocated by Docker at runtime and the DockerPort object contains the dynamic port and IP assignment.

Collecting logs
---------------

To record the logs from your containers specify a location:

    public class DockerCompositionTest {
  
        @ClassRule
        public DockerComposition composition = new DockerComposition("src/test/resources/docker-compose.yml")
                                                      .saveLogsTo("build/dockerLogs/dockerCompositionTest");
  
        @Test
        public void testRecordsLogs() throws InterruptedException, IOException {
           ...
        }
  
    }

This will automatically record logs for all containers in real time to the specified directory. Collection will stop when the containers terminate.

Using a custom version of docker-compose
---------------

docker-compose-rule tries to use the docker-compose binary located at `/usr/local/bin/docker-compose`. This can be overriden by setting `DOCKER_COMPOSE_LOCATION` to be the path to a valid file.
