package leia;

import org.junit.ClassRule;
import org.junit.experimental.categories.Category;
import org.testcontainers.containers.DockerComposeContainer;

import java.io.File;

interface IntegrationTest {
}

@Category(IntegrationTest.class)
public class IntegrationTestBaseRule {

    @ClassRule
    public static DockerComposeContainer environment =
        new DockerComposeContainer(new File("docker-compose-kafka-cluster.yaml"))
            .withExposedService("zookeeper", 32181)
            .withExposedService("kafka", 9092)
            .withExposedService("leia", 80)
            .withLocalCompose(true);
}
