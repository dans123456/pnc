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
package org.jboss.pnc.datastore;

import org.jboss.pnc.model.Artifact;
import org.jboss.pnc.model.BuildConfigSetRecord;
import org.jboss.pnc.model.BuildConfiguration;
import org.jboss.pnc.model.BuildConfigurationAudited;
import org.jboss.pnc.model.BuildRecord;
import org.jboss.pnc.model.User;
import org.jboss.pnc.spi.BuildCoordinationStatus;
import org.jboss.pnc.spi.coordinator.BuildSetTask;
import org.jboss.pnc.spi.coordinator.BuildTask;
import org.jboss.pnc.spi.datastore.Datastore;
import org.jboss.pnc.spi.datastore.predicates.UserPredicates;
import org.jboss.pnc.spi.datastore.repositories.ArtifactRepository;
import org.jboss.pnc.spi.datastore.repositories.BuildConfigSetRecordRepository;
import org.jboss.pnc.spi.datastore.repositories.BuildConfigurationAuditedRepository;
import org.jboss.pnc.spi.datastore.repositories.BuildConfigurationRepository;
import org.jboss.pnc.spi.datastore.repositories.BuildRecordRepository;
import org.jboss.pnc.spi.datastore.repositories.SequenceHandlerRepository;
import org.jboss.pnc.spi.datastore.repositories.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;
import java.lang.invoke.MethodHandles;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.jboss.pnc.common.util.CollectionUtils.ofNullableCollection;
import static org.jboss.pnc.common.util.StreamCollectors.toFlatList;
import static org.jboss.pnc.spi.datastore.predicates.ArtifactPredicates.withIdentifierAndSha256;
import static org.jboss.pnc.spi.datastore.predicates.ArtifactPredicates.withOriginUrl;

@Stateless
public class DefaultDatastore implements Datastore {

    public static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private ArtifactRepository artifactRepository;

    private BuildRecordRepository buildRecordRepository;

    private BuildConfigurationRepository buildConfigurationRepository;

    private BuildConfigurationAuditedRepository buildConfigurationAuditedRepository;

    private BuildConfigSetRecordRepository buildConfigSetRecordRepository;

    private UserRepository userRepository;

    private SequenceHandlerRepository sequenceHandlerRepository;

    public DefaultDatastore() {
    }

    @Inject
    public DefaultDatastore(ArtifactRepository artifactRepository,
                            BuildRecordRepository buildRecordRepository,
                            BuildConfigurationRepository buildConfigurationRepository,
                            BuildConfigurationAuditedRepository buildConfigurationAuditedRepository,
                            BuildConfigSetRecordRepository buildConfigSetRecordRepository,
                            UserRepository userRepository,
                            SequenceHandlerRepository sequenceHandlerRepository) {
        this.artifactRepository = artifactRepository;
        this.buildRecordRepository = buildRecordRepository;
        this.buildConfigurationRepository = buildConfigurationRepository;
        this.buildConfigurationAuditedRepository = buildConfigurationAuditedRepository;
        this.buildConfigSetRecordRepository = buildConfigSetRecordRepository;
        this.userRepository = userRepository;
        this.sequenceHandlerRepository = sequenceHandlerRepository;
    }

    private static final String ARITFACT_ORIGIN_URL_IDENTIFIER_CONFLICT_MESSAGE = "Another artifact with the same originUrl but a different identifier already exists";
    private static final String ARITFACT_ORIGIN_URL_CHECKSUM_CONFLICT_MESSAGE = "Another artifact with the same originUrl but a different checksum already exists";

    @Override
    public Map<Artifact, String> checkForConflictingArtifacts(Collection<Artifact> artifacts) {
        Map<Artifact, String> conflicts = new HashMap<>();
        for (Artifact artifact : artifacts) {
            // Check for matching URL with different identifier or checksum
            if (artifact.getOriginUrl() != null) {
                Artifact artifactFromDb = artifactRepository.queryByPredicates(withOriginUrl(artifact.getOriginUrl()));
                if (artifactFromDb.getIdentifier().equals(artifact.getIdentifier())) {
                    conflicts.put(artifact, ARITFACT_ORIGIN_URL_IDENTIFIER_CONFLICT_MESSAGE);
                }
                if (artifactFromDb.getSha256().equals(artifact.getSha256())) {
                    conflicts.put(artifact, ARITFACT_ORIGIN_URL_CHECKSUM_CONFLICT_MESSAGE);
                }
            }
        }
        return conflicts;
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public BuildRecord storeCompletedBuild(BuildRecord.Builder buildRecordBuilder) {
        BuildRecord buildRecord = buildRecordBuilder.build();
        refreshBuildConfiguration(buildRecord);
        buildRecord.setDependencies(saveArtifacts(buildRecord.getDependencies()));
        buildRecord.setBuiltArtifacts(saveArtifacts(buildRecord.getBuiltArtifacts()));
        buildRecord = buildRecordRepository.save(buildRecord);

        return buildRecord;
    }

    /**
     * Checks the given list against the existing database and creates a new list containing
     * artifacts which have been saved to or loaded from the database.
     *
     * @param artifacts of in-memory artifacts to either insert to the database or find the matching record in the db
     * @return Set of up to date JPA artifact entities
     */
    private Set<Artifact> saveArtifacts(Collection<Artifact> artifacts) {
        Set<Artifact> savedArtifacts = new HashSet<>();
        for (Artifact artifact : artifacts) {

            Artifact artifactFromDb = artifactRepository
                    .queryByPredicates(withIdentifierAndSha256(artifact.getIdentifier(), artifact.getSha256()));
            if (artifactFromDb == null) {
                artifactFromDb = artifactRepository.save(artifact);
            }
            savedArtifacts.add(artifactFromDb);
        }
        return savedArtifacts;
    }

    @Override
    public User retrieveUserByUsername(String username) {
        return userRepository.queryByPredicates(UserPredicates.withUserName(username));
    }

    private void refreshBuildConfiguration(BuildRecord buildRecord) {
        if (buildRecord.getLatestBuildConfiguration() != null) {
            BuildConfiguration configurationFromDB = buildConfigurationRepository.queryById(buildRecord
                    .getLatestBuildConfiguration().getId());
            buildRecord.setLatestBuildConfiguration(configurationFromDB);
        }
    }

    @Override
    public void createNewUser(User user) {
        userRepository.save(user);
    }

    @Override
    public int getNextBuildRecordId() {

        Long nextId = sequenceHandlerRepository.getNextID(BuildRecord.SEQUENCE_NAME);
        logger.debug("Build Record nextId: {}", nextId);

        return nextId.intValue();
    }

    /**
     * Save a build config set record to the db.  This requires a new transaction to ensure that
     * the record is immediately committed to the database so that it's available to use by the
     * foreign keys set in the individual build records.
     */
    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public BuildConfigSetRecord saveBuildConfigSetRecord(BuildConfigSetRecord buildConfigSetRecord) {
        return buildConfigSetRecordRepository.save(buildConfigSetRecord);
    }

    /**
     * Get the latest audited revision for the given build configuration ID
     *
     * @param buildConfigurationId
     * @return The latest revision of the given build configuration
     */
    @Override
    public BuildConfigurationAudited getLatestBuildConfigurationAudited(Integer buildConfigurationId) {
        List<BuildConfigurationAudited> buildConfigRevs = buildConfigurationAuditedRepository.findAllByIdOrderByRevDesc(buildConfigurationId);
        if (buildConfigRevs.isEmpty()) {
            // TODO should we throw an exception?  In theory, this should never happen.
            return null;
        }
        return buildConfigRevs.get(0);
    }

    @Override
    public BuildConfigSetRecord getBuildConfigSetRecordById(Integer buildConfigSetRecordId) {
        return buildConfigSetRecordRepository.queryById(buildConfigSetRecordId);
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public boolean requiresRebuild(BuildTask task) {
        BuildSetTask taskSet = task.getBuildSetTask();
        final BuildConfiguration buildConfiguration = task.getBuildConfiguration();
        BuildConfiguration refreshedConfig = buildConfigurationRepository.queryById(buildConfiguration.getId());

        if (refreshedConfig.getLatestSuccesfulBuildRecord() == null) {
            return true;
        }
        if (taskSet != null) {
            List<BuildConfiguration> nonRejectedBuildsInGroup = taskSet.getBuildTasks().stream()
                    .filter(t -> t.getStatus() != BuildCoordinationStatus.REJECTED_ALREADY_BUILT)
                    .map(BuildTask::getBuildConfiguration)
                    .collect(Collectors.toList());
            boolean hasInGroupDependency = refreshedConfig.dependsOnAny(nonRejectedBuildsInGroup);
            if (hasInGroupDependency) {
                return true;
            }
        }
        return hasARebuiltDependency(refreshedConfig);
    }

    private boolean hasARebuiltDependency(BuildConfiguration config) {
        BuildRecord record = config.getLatestSuccesfulBuildRecord();
        if (record == null) {
            return false;
        }

        Collection<BuildRecord> lastBuiltFrom = getRecordsUsedFor(record);

        return lastBuiltFrom.stream()
                .anyMatch(this::hasNewerVersion);
    }

    private boolean hasNewerVersion(BuildRecord record) {
        BuildConfiguration buildConfig = record.getLatestBuildConfiguration();
        BuildRecord newestRecord = buildConfig.getLatestSuccesfulBuildRecord();
        return !record.getId().equals(newestRecord.getId());
    }

    private Collection<BuildRecord> getRecordsUsedFor(BuildRecord record) {
        return ofNullableCollection(record.getDependencies())
                .stream()
                .map(Artifact::getBuildRecords)
                .collect(toFlatList());
    }

}
