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
package org.jboss.pnc.bpm;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import org.jboss.pnc.common.json.moduleconfig.BpmModuleConfig;
import org.jboss.pnc.rest.restmodel.bpm.BpmNotificationRest;
import org.jboss.pnc.spi.exception.CoreException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import static java.util.Objects.requireNonNull;

/**
 * Parent type of all BPM tasks. Task is a representation of
 * BPM process execution, keeps track of the remote process instance
 * and handles notifications.
 * Class implements Comparable based on the taskId,
 * so they can be sorted at REST endpoint.
 * BPM tasks do not have to be thread safe.
 *
 * @author Jakub Senko
 */
@EqualsAndHashCode(of = "taskId")
@ToString
public abstract class BpmTask implements Comparable<BpmTask> {

    private static final Logger log = LoggerFactory.getLogger(BpmTask.class);

    protected static final ObjectMapper MAPPER = new ObjectMapper();

    /**
     * This is an internal identifier, not one provided by BPM server.
     */
    private Integer taskId;

    private Long processInstanceId;

    @Getter
    private final List<BpmNotificationRest> events = new ArrayList<>();

    private String processName;

    protected BpmModuleConfig config;

    private Map<BpmEventType, List<Consumer<?>>> listeners = new HashMap<>();

    /**
     * Users OAuth token used to authenticate requests on remote services
     */
    private String accessToken;

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public String getProcessName() {
        return processName;
    }

    /* package */ void setProcessName(String processName) {
        this.processName = processName;
    }

    /**
     * Get the name of the BPM process for this task.
     * Warning: The process MUST be asynchronous so it
     * does not block BPM manager.
     */
    protected abstract String getProcessId();

    /**
     * Provide parameters to the BPM process.
     * The JSON representation of this map will be available
     * in the BPM process as "processParameters" variable.
     * Some parameters, such as "taskId" are automatically added,
     * not into the "processParameters" variable, but 'one level higher'
     * and directly accessible
     * from process variables.
     *
     * @return a map of process parameters
     * @throws CoreException
     */
    protected abstract Map<String, Object> getProcessParameters() throws CoreException;

    /* package */ void setBpmConfig(BpmModuleConfig config) {
        this.config = config;
    }

    public Integer getTaskId() {
        return taskId;
    }

    /* package */ void setTaskId(int taskId) {
        this.taskId = taskId;
    }

    public Long getProcessInstanceId() {
        return processInstanceId;
    }

    /* package */ void setProcessInstanceId(Long processInstanceId) {
        this.processInstanceId = processInstanceId;
    }

    /**
     * Listen to notifications from BPM process for this task.
     *
     * @param eventType event to follow
     */
    public <T extends BpmNotificationRest> void addListener(BpmEventType eventType, Consumer<T> listener) {
        List<Consumer<?>> consumers = listeners.get(eventType);
        if (consumers == null) {
            consumers = new ArrayList<>();
        }
        consumers.add(listener);
        listeners.put(eventType, consumers);
    }


    /* package */ <T extends BpmNotificationRest> void notify(BpmEventType eventType, T data) {
        List<Consumer<?>> listeners = this.listeners.get(eventType);
        if(listeners == null) {
            listeners = new ArrayList<>();
            this.listeners.put(eventType, listeners);
        }
        log.debug("will notify bpm listeners for eventType: {}, matching listeners: {}, all listeners: {}", eventType, listeners);
        listeners.forEach(c -> {
            // Cast is OK because there is no unchecked method declaration to put wrong types
            ((Consumer<T>) c).accept(data);
        });
        events.add(data);
    }

    /**
     * Extend process parameters from the task with additional useful information,
     * such as pncBaseUrl and taskId, needed for notifications.
     * Before use, taskId MUST be assigned.
     *
     * @throws CoreException
     */
    protected Map<String, Object> getExtendedProcessParameters() throws CoreException {
        Map<String, Object> parameters = getProcessParameters();
        requireNonNull(parameters);
        Map<String, Object> actualParameters = new HashMap<>();
        try {
            actualParameters.put("processParameters", MAPPER.writeValueAsString(parameters));
        } catch (JsonProcessingException e) {
            throw new CoreException("Could not serialize process parameters '" +
                    parameters + "'.", e);
        }

        actualParameters.put("pncBaseUrl", config.getPncBaseUrl());
        actualParameters.put("repourBaseUrl", config.getRepourBaseUrl());
        actualParameters.put("taskId", taskId);
        actualParameters.put("usersAuthToken", accessToken);
        return actualParameters;
    }

    @Override
    public int compareTo(BpmTask other) {
        requireNonNull(other);
        if (taskId == null)
            return other.getTaskId() == null ? 0 : -1;
        if (other.getTaskId() == null)
            return 1;
        return taskId.compareTo(other.getTaskId());
    }
}
