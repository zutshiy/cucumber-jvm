package io.cucucumber.jupiter.engine;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.platform.engine.ConfigurationParameters;
import org.junit.platform.engine.EngineDiscoveryRequest;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.TestTag;
import org.junit.platform.engine.UniqueId;

import java.io.File;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import static io.cucucumber.jupiter.engine.FeatureResolver.createFeatureResolver;
import static java.util.Arrays.asList;
import static java.util.Collections.emptySet;
import static java.util.Optional.of;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.platform.engine.TestDescriptor.Type.CONTAINER;
import static org.junit.platform.engine.TestDescriptor.Type.TEST;
import static org.junit.platform.engine.TestTag.create;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClasspathResource;
import static org.junit.platform.engine.support.descriptor.ClasspathResourceSource.from;
import static org.junit.platform.engine.support.descriptor.FilePosition.from;

class FeatureResolverTest {
    private final String featurePath = "io/cucumber/jupiter/engine/feature-with-outline.feature";

    private TestDescriptor testDescriptor;
    private UniqueId id;


    @BeforeEach
    void before() {
        CucumberTestEngine engine = new CucumberTestEngine();
        ConfigurationParameters configuration = new EmptyConfigurationParameters();
        EngineDiscoveryRequest discoveryRequest = new EmptyEngineDiscoveryRequest(configuration);
        id = UniqueId.forEngine(engine.getId());
        testDescriptor = engine.discover(discoveryRequest, id);
        FeatureResolver featureResolver = createFeatureResolver(testDescriptor);
        featureResolver.resolveClassPathResource(selectClasspathResource(featurePath));
    }

    @Test
    void feature() {
        TestDescriptor feature = getFeature();
        assertEquals("A feature with scenario outlines", feature.getDisplayName());
        assertEquals(emptySet(), feature.getTags());
        assertEquals(of(from(featurePath)), feature.getSource());
        assertEquals(CONTAINER, feature.getType());
        assertEquals(
            id.append("feature", featurePath),
            feature.getUniqueId()
        );
    }

    @Test
    void scenario() {
        TestDescriptor scenario = getScenario();
        assertEquals("A scenario", scenario.getDisplayName());
        assertEquals(
            asSet(create("@FeatureTag"), create("@ScenarioTag")),
            scenario.getTags()
        );
        assertEquals(of(from(featurePath, from(5, 3))), scenario.getSource());
        assertEquals(TEST, scenario.getType());
        assertEquals(
            id.append("feature", featurePath).append("scenario", "5"),
            scenario.getUniqueId()
        );

        PickleDescriptor pickleDescriptor = (PickleDescriptor) scenario;
        assertEquals("io.cucumber.jupiter.engine", pickleDescriptor.getPackage());
    }

    @Test
    void outline() {
        TestDescriptor outline = getOutline();
        assertEquals("A scenario outline", outline.getDisplayName());
        assertEquals(
            emptySet(),
            outline.getTags()
        );
        assertEquals(of(from(featurePath, from(11, 3))), outline.getSource());
        assertEquals(CONTAINER, outline.getType());
        assertEquals(id.append(
            "feature", featurePath).append("outline", "11"),
            outline.getUniqueId()
        );
    }

    @Test
    void example() {
        TestDescriptor example = getExample();
        assertEquals("Example #1", example.getDisplayName());
        assertEquals(
            asSet(create("@FeatureTag"), create("@Example1Tag"), create("@ScenarioOutlineTag")),
            example.getTags()
        );
        assertEquals(of(from(featurePath, from(19, 8))), example.getSource());
        assertEquals(TEST, example.getType());

        assertEquals(
            id.append("feature", featurePath).append("outline", "11").append("example", "19"),
            example.getUniqueId()
        );

        PickleDescriptor pickleDescriptor = (PickleDescriptor) example;
        assertEquals("io.cucumber.jupiter.engine", pickleDescriptor.getPackage());
    }

    private Set<TestTag> asSet(TestTag... tags) {
        return new HashSet<>(asList(tags));
    }

    private TestDescriptor getFeature() {
        Set<? extends TestDescriptor> features = testDescriptor.getChildren();
        return features.iterator().next();
    }

    private TestDescriptor getScenario() {
        return getFeature().getChildren().iterator().next();
    }

    private TestDescriptor getOutline() {
        Iterator<? extends TestDescriptor> iterator = getFeature().getChildren().iterator();
        iterator.next();
        return iterator.next();
    }

    private TestDescriptor getExample() {
        return getOutline().getChildren().iterator().next();
    }
}