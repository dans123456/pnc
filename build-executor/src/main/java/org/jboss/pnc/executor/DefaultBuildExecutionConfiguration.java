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

package org.jboss.pnc.executor;

import org.jboss.pnc.model.SystemImageType;
import org.jboss.pnc.spi.executor.BuildExecutionConfiguration;

/**
 * @author <a href="mailto:matejonnet@gmail.com">Matej Lazar</a>
 */
public class DefaultBuildExecutionConfiguration implements BuildExecutionConfiguration {

    private final int id;
    private final String buildContentId;
    private final Integer userId;
    private final String buildScript;
    private final String name;
    private final String scmRepoURL;
    private final String scmRevision;
    private final String systemImageId;
    private final String systemImageRepositoryUrl;
    private final SystemImageType systemImageType;
    private final boolean podKeptAfterFailure;

    public DefaultBuildExecutionConfiguration(
            int id,
            String buildContentId,
            Integer userId,
            String buildScript,
            String name,
            String scmRepoURL,
            String scmRevision,
            String systemImageId,
            String systemImageRepositoryUrl,
            SystemImageType systemImageType,
            boolean podKeptAfterFailure) {

        this.id = id;
        this.buildContentId = buildContentId;
        this.userId = userId;
        this.buildScript = buildScript;
        this.name = name;
        this.scmRepoURL = scmRepoURL;
        this.scmRevision = scmRevision;
        this.systemImageId = systemImageId;
        this.systemImageRepositoryUrl = systemImageRepositoryUrl;
        this.systemImageType = systemImageType;
        this.podKeptAfterFailure = podKeptAfterFailure;
    }

    @Override
    public int getId() {
        return id;
    }

    @Override
    public String getBuildContentId() {
        return buildContentId;
    }

    @Override
    public Integer getUserId() {
        return userId;
    }

    @Override
    public String getBuildScript() {
        return buildScript;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getScmRepoURL() {
        return scmRepoURL;
    }

    @Override
    public String getScmRevision() {
        return scmRevision;
    }

    @Override
    public String getSystemImageId() {
        return systemImageId;
    }

    @Override
    public String getSystemImageRepositoryUrl() {
        return systemImageRepositoryUrl;
    }

    @Override
    public SystemImageType getSystemImageType() {
        return systemImageType;
    }

    @Override
    public boolean isPodKeptOnFailure() {
        return podKeptAfterFailure;
    }
}
