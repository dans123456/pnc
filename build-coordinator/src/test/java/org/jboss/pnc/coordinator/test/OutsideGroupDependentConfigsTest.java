/**
 * JBoss, Home of Professional Open Source.
 * Copyright 2014 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.pnc.coordinator.test;

import lombok.RequiredArgsConstructor;
import org.jboss.pnc.common.json.ConfigurationParseException;
import org.jboss.pnc.model.Artifact;
import org.jboss.pnc.model.BuildConfiguration;
import org.jboss.pnc.model.BuildConfigurationSet;
import org.jboss.pnc.model.BuildRecord;
import org.jboss.pnc.spi.datastore.DatastoreException;
import org.jboss.pnc.spi.exception.CoreException;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Group consists of configA,config B and configC. <br/>
 * configC is independent, configB depends on configA. <br/>
 *
 *
 * config1 is an "outside" dependency of configA
 *
 * <p>
 * Author: Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com
 * Date: 9/14/16
 * Time: 12:09 PM
 * </p>
 */
public class OutsideGroupDependentConfigsTest extends AbstractDependentBuildTest {

    private BuildConfiguration config1;

    private BuildConfiguration configA;
    private BuildConfiguration configB;

    private BuildConfigurationSet configSet;

    @Before
    public void setUp() throws DatastoreException, ConfigurationParseException {
        config1 = config("1");

        configA = config("A", config1);
        configB = config("B", configA);
        BuildConfiguration configC = config("C");

        configSet = configSet(configA, configB, configC);

        markAsAlreadyBuilt(config1, configA, configB, configC);
        make(configA).dependOn(config1);
    }

    @Test
    public void shouldNotRebuildIfDependencyIsNotRebuilt() throws CoreException, TimeoutException, InterruptedException {
        build(configSet, false);
        waitForEmptyBuildQueue();
        List<BuildConfiguration> configsWithTasks = getBuiltConfigs();
        assertThat(configsWithTasks).isEmpty();
    }

    @Test
    public void shouldRebuildOnlyDependent() throws CoreException, TimeoutException, InterruptedException {
        createNewVersion(config1);
        build(configSet, false);
        waitForEmptyBuildQueue();
        List<BuildConfiguration> configsWithTasks = getBuiltConfigs();
        assertThat(configsWithTasks).hasSameElementsAs(Arrays.asList(configA, configB));
    }

    private void createNewVersion(BuildConfiguration config1) {
        config1.getBuildRecords().add(buildRecord(config1));
    }


    private DependencyHandler make(BuildConfiguration config) {
        return new DependencyHandler(config);
    }

    @RequiredArgsConstructor
    private static class DependencyHandler {
        final BuildConfiguration config;
        BuildRecord record;

        public void dependOn(BuildConfiguration... dependencies) {
            record = config.getLatestSuccesfulBuildRecord();
            record.setLatestBuildConfiguration(config);

            Set<Artifact> artifacts = Stream.of(dependencies)
                    .map(this::mockArtifactBuiltWith)
                    .collect(Collectors.toSet());
            config.getLatestSuccesfulBuildRecord().setDependencies(artifacts);
        }

        private Artifact mockArtifactBuiltWith(BuildConfiguration config) {
            BuildRecord record = config.getLatestSuccesfulBuildRecord();
            Set<BuildRecord> records = new HashSet<>();
            records.add(record);
            return  Artifact.Builder.newBuilder()
                    .buildRecords(records)
                    .build();
        }
    }
}