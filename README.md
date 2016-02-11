![build status](https://circleci.com/gh/palantir/docker-compose-rule.svg?style=shield&circle-token=ed5bbc06f483e3f7324d1b3440125827c8d355d7)

Docker Compose JUnit Rule
=========================

This is a small library for executing JUnit tests that interact with Docker Compose managed containers. It supports the following:

- Starting containers defined in a docker-compose.yml before tests and tearing them down afterwards
- Waiting for services to become available before running tests
- Recording log files from containers to aid debugging test failures

Why should I use this?
----------------------

The code here started out as the end to end tests for Magritte. We needed to run Magritte in a variety of different configurations and environments which were mutually incompatible, thus multiple Docker Compose files were needed and so a simplistic model of running `docker-compose up` in Gradle was insufficient.

If you're experiencing any of the following using Docker for your testing this library should hopefully help:

- Orchestrating multiple services and mapping the ports to outside the Docker machine so assertions can be made in tests
- Needing to know when services are up to prevent flickering tests caused by slow to start services or complicated service dependencies
- Lack of insight into what has happened in Docker containers during tests on CI servers due to loss of logs
- Tests failing due to needing open ports on the CI build host which conflict with the test configuration

Simple Use
----------

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
                                                    .waitingForService("db")
                                                    .waitingForHttpService("web", 8080, (port) -> "https://" + port.getIp() + ":" + port.getExternalPort());
                                                    .waitingForService("other", (port) -> customServiceCheck(port))

      @Test
      public void testThatDependsServicesHavingStarted() throws InterruptedException, IOException {
         ...
      }


  }

The simple `waitingForService(String container)` method will ensure all ports exposed in the Docker compose file will be open before the tests are run. Note that this is likely insufficient - the port will be opened by Docker before the service started by the container might actually be listening.

`waitingForHttpService(String container, int internalPort, Function<DockerPort, String> urlFunction)` will wait until a HTTP response is received when connecting to the service. `internalPort` should reference the port the Docker container exposes, not an external port that might be exposed in the Docker Compose file. The `urlFunction` will be provided with the actual port mapping at runtime and should return the URL that should be used to connect. For more information about port mappings see the section below.

`waitingForService(String container, Function<DockerPort, Boolean> check)` allows arbitrary checks to be executed. If the function provided returns false then the test setup will fail and JUnit will not execute any tests.

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

Accessing services in a container with TLS
------------------------------------------

When your containerised service is configured to use TLS, it's difficult to communicate with it, because cert validation will generally fail. There is an optional feature that replaces the internal Java `NameService` with a cert. To use it, set the system property `sun.net.spi.nameservice.provider.1` to `dns,docker-compose-rule`. When set, one can communicate with container `foo` at hostname `foo`.

An example of this (with Gradle) is:

    test {
        systemProperty "sun.net.spi.nameservice.provider.1", "dns,docker-compose-rule"
    } 
