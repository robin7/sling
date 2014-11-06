/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.sling.replication.queue;

import javax.annotation.Nonnull;

import aQute.bnd.annotation.ConsumerType;
import org.apache.sling.replication.component.ReplicationComponent;

/**
 * A provider for {@link ReplicationQueue}s
 */
@ConsumerType
public interface ReplicationQueueProvider extends ReplicationComponent {

    /**
     * provide a named queue for the given agent
     *
     * @param queueName the name of the queue to retrieve
     * @return a {@link org.apache.sling.replication.queue.ReplicationQueue}
     * @throws ReplicationQueueException if any error happens
     */
    @Nonnull
    ReplicationQueue getQueue(@Nonnull String queueName) throws ReplicationQueueException;

    /**
     * enables queue driven processing for an agent
     *
     * @param queueProcessor the queue processor to be used
     */
    void enableQueueProcessing(@Nonnull ReplicationQueueProcessor queueProcessor) throws ReplicationQueueException;


    /**
     * disables queue driven processing for an agent
     *
     */
    void disableQueueProcessing() throws ReplicationQueueException;
}