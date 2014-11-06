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
package org.apache.sling.replication.trigger.impl;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;

import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIUtils;
import org.apache.http.concurrent.FutureCallback;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.impl.nio.client.HttpAsyncClients;
import org.apache.http.nio.ContentDecoder;
import org.apache.http.nio.IOControl;
import org.apache.http.nio.protocol.BasicAsyncRequestProducer;
import org.apache.http.nio.protocol.BasicAsyncResponseConsumer;
import org.apache.sling.commons.scheduler.ScheduleOptions;
import org.apache.sling.commons.scheduler.Scheduler;
import org.apache.sling.replication.communication.ReplicationActionType;
import org.apache.sling.replication.transport.impl.ReplicationEndpoint;
import org.apache.sling.replication.communication.ReplicationRequest;
import org.apache.sling.replication.transport.authentication.TransportAuthenticationContext;
import org.apache.sling.replication.transport.authentication.TransportAuthenticationProvider;
import org.apache.sling.replication.trigger.ReplicationRequestHandler;
import org.apache.sling.replication.trigger.ReplicationTrigger;
import org.apache.sling.replication.trigger.ReplicationTriggerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link org.apache.sling.replication.trigger.ReplicationTrigger} to trigger replication upon reception of server sent events
 * on a certain URL
 */
public class RemoteEventReplicationTrigger implements ReplicationTrigger {
    private final static String SCHEDULE_NAME = "remoteEventTrigger";

    private final Logger log = LoggerFactory.getLogger(getClass());

    private final ReplicationEndpoint endpoint;
    private final TransportAuthenticationProvider<CredentialsProvider, CredentialsProvider> authenticationProvider;

    private Scheduler scheduler;

    private Map<String, Future<HttpResponse>> requests = new ConcurrentHashMap<String, Future<HttpResponse>>();

    public RemoteEventReplicationTrigger(String endpoint, TransportAuthenticationProvider<CredentialsProvider, CredentialsProvider> authenticationProvider, Scheduler scheduler) {
        if (endpoint == null) {
            throw new IllegalArgumentException("Endpoint is required");
        }

        if (authenticationProvider == null) {
            throw new IllegalArgumentException("Authentication provider is required");
        }

        if (!authenticationProvider.canAuthenticate(CredentialsProvider.class)) {
            throw new IllegalArgumentException("Authentication provider cannot authenticate CredentialsProvider");
        }

        this.authenticationProvider = authenticationProvider;
        this.endpoint = new ReplicationEndpoint(endpoint);
        this.scheduler = scheduler;
    }

    public void register(@Nonnull ReplicationRequestHandler requestHandler) throws ReplicationTriggerException {
        try {
            log.info("applying remote event replication trigger");

            ScheduleOptions options = scheduler.NOW();
            options.name(getJobName(requestHandler));
            scheduler.schedule(new EventBasedReplication(requestHandler), options);
        } catch (Exception e) {
            throw new ReplicationTriggerException("unable to register handler " + requestHandler, e);
        }
    }

    public void unregister(@Nonnull ReplicationRequestHandler requestHandler) throws ReplicationTriggerException {
        Future<HttpResponse> httpResponseFuture = requests.remove(requestHandler.toString());
        if (httpResponseFuture != null) {
            httpResponseFuture.cancel(true);
        }
        scheduler.unschedule(getJobName(requestHandler));
    }

    String getJobName(ReplicationRequestHandler requestHandler) {
        return SCHEDULE_NAME + requestHandler.toString();
    }

    private class SSEResponseConsumer extends BasicAsyncResponseConsumer {

        private final ReplicationRequestHandler handler;

        private SSEResponseConsumer(ReplicationRequestHandler handler) {
            this.handler = handler;
        }

        @Override
        protected void onContentReceived(ContentDecoder decoder, IOControl ioctrl) throws IOException {
            log.debug("complete {}", decoder.isCompleted());
            ByteBuffer buffer = ByteBuffer.allocate(1024);
            decoder.read(buffer);
            log.debug("content {} received {},{}", new Object[]{buffer, decoder, ioctrl});

            // TODO : currently it always triggers poll request on /, should this be configurable?
            ReplicationRequest replicationRequest = new ReplicationRequest(System.currentTimeMillis(), ReplicationActionType.POLL, "/");
            handler.handle(replicationRequest);
            log.info("replication request to agent {} sent ({} {})", new Object[]{
                    handler,
                    replicationRequest.getAction(),
                    replicationRequest.getPaths()});

            super.onContentReceived(decoder, ioctrl);
        }

        @Override
        protected void onResponseReceived(HttpResponse response) throws IOException {
            log.info("response received {}", response);
            super.onResponseReceived(response);
        }
    }

    private class EventBasedReplication implements Runnable {
        private final ReplicationRequestHandler requestHandler;

        public EventBasedReplication(ReplicationRequestHandler requestHandler) {
            this.requestHandler = requestHandler;
        }

        public void run() {
            try {
                log.debug("getting events from {}", endpoint.getUri().toString());

                log.debug("preparing request");

                // TODO : http client should be cached and reused

                CredentialsProvider credentialsProvider = new BasicCredentialsProvider();

                TransportAuthenticationContext context = new TransportAuthenticationContext();
                context.addAttribute("endpoint", endpoint);
                credentialsProvider = authenticationProvider.authenticate(credentialsProvider, context);

                final CloseableHttpAsyncClient httpClient = HttpAsyncClients.custom()
                        .setDefaultCredentialsProvider(credentialsProvider)
                        .build();

                HttpGet get = new HttpGet(endpoint.getUri());
                HttpHost target = URIUtils.extractHost(get.getURI());
                BasicAsyncRequestProducer basicAsyncRequestProducer = new BasicAsyncRequestProducer(target, get);
                httpClient.start();
                try {
                    log.debug("sending request");
                    Future<HttpResponse> futureResponse = httpClient.execute(
                            basicAsyncRequestProducer,
                            new SSEResponseConsumer(requestHandler), new FutureCallback<HttpResponse>() {
                                public void completed(HttpResponse httpResponse) {
                                    log.debug("response received {}", httpResponse);
                                }

                                public void failed(Exception e) {
                                    log.warn("failed request {}", e.toString());
                                }

                                public void cancelled() {
                                    log.warn("request cancelled");
                                }
                            });
                    requests.put(requestHandler.toString(), futureResponse);
                    futureResponse.get();

                } catch (Exception e) {
                    log.warn("cannot communicate with {}", endpoint, e);
                }
                httpClient.close();
                log.debug("request finished");
            } catch (Exception e) {
                log.error("cannot run event based replication {}", e);
            }
        }
    }
}