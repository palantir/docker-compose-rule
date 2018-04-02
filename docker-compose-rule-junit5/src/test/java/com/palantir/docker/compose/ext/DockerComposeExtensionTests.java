package com.palantir.docker.compose.ext;


import static org.assertj.core.api.Assertions.assertThat;

import com.palantir.docker.compose.DockerComposition;
import com.palantir.docker.compose.DockerCompositionConfiguration;
import com.palantir.docker.compose.DockerCompositionExecution;
import com.palantir.docker.compose.test.TestExecution;
import com.palantir.docker.compose.test.TestExecutionsReport;
import com.palantir.docker.compose.test.TestExecutor;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class DockerComposeExtensionTests {

    @Test
    void testConstructorInjection() {
        TestExecutionsReport executionsReport = TestExecutor.execute(ConstructorInjectionTest.class);
        assertThat(executionsReport.executions()).isNotEmpty();

        TestExecution testExecution = executionsReport.firstExecution("test()");
        testExecution.
    }

//    @Test
//    void testMethodInjection() {
//        TestExecutionsReport report = TestExecutor.execute(MethodInjectionTest.class);
//        assertThat(report.executions()).isEmpty();
//    }


    @DockerComposition(files = {"src/test/resources/docker-compose.yaml"}, project = "example")
    static class ConstructorInjectionTest {
        private DockerCompositionExecution dockerComposeExecution;

        ConstructorInjectionTest(DockerCompositionExecution dockerComposeExecution) {
            this.dockerComposeExecution = dockerComposeExecution;
        }

        @Test
        void test() {
            assertThat(dockerComposeExecution.projectName().asString()).isEqualTo("example");
        }

        @ParameterizedTest
        @ValueSource(strings = { "notExample", "foobar" })
        void testParameterized(String input) {
            assertThat(dockerComposition.projectName().asString()).isNotEqualTo(input);
        }

        @Nested
        class NestedConstructorInjectionTest {
            private DockerCompositionConfiguration innerDockerComposition;
            NestedConstructorInjectionTest(DockerCompositionConfiguration dockerComposition) {
                this.innerDockerComposition = dockerComposition;
            }
            @Test
            void test_shouldFail() {
                assertThat(dockerComposition.projectName().asString())
                        .isEqualTo(innerDockerComposition.projectName().asString());
            }
        }
    }

    @DockerComposition(files = {"src/test/resources/docker-compose.yaml"}, project = "example")
    static class MethodInjectionTest {

        @Test
        void test_shouldFail(DockerCompositionConfiguration dockerComposition) {
            assertThat(dockerComposition.projectName().asString()).isEqualTo("example");
        }

    }

}
