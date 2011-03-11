/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sling.testing.samples.integrationtests.serverside;

import org.apache.sling.junit.remote.testrunner.SlingRemoteTestParameters;
import org.apache.sling.junit.remote.testrunner.SlingRemoteTestRunner;
import org.junit.runner.RunWith;

/** Run server-side tests from the sampletests bundle, without
 *  checking the number of tests executed (by not implementing
 *  {@link SlingTestsCountChecker} */
@RunWith(SlingRemoteTestRunner.class)
public class DontCareForTestsCountTest extends ServerSideTestsBase 
implements SlingRemoteTestParameters {

    public static final String TEST_SELECTOR = "org.apache.sling.testing.samples.sampletests";
    
    public String getJunitServletUrl() {
        return getServerBaseUrl() + JUNIT_SERVLET_PATH;
    }

    public String getTestClassesSelector() {
        return TEST_SELECTOR;
    }

    public String getTestMethodSelector() {
        return null;
    }
}