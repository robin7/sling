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

package org.apache.sling.resourceaccesssecurity.it.impl.gates;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.resourceaccesssecurity.ResourceAccessGate;

@Component
@Service(value=ResourceAccessGate.class)
@Properties({
        @Property(name=ResourceAccessGate.PATH, label="Path", value="/test/(un|)secured-provider/read(-update|)/(prov|mixed)/.*", 
                description="The path is a regular expression for which resources the service should be called"),
        @Property(name=ResourceAccessGate.OPERATIONS, value="read,update"),
        @Property(name=ResourceAccessGate.CONTEXT, value=ResourceAccessGate.PROVIDER_CONTEXT)
})
public class ProviderGate1 extends AResourceAccessGate implements ResourceAccessGate {

    public static String GATE_ID = "providergate1";
    
    @Override
    protected String getGateId() {
        return GATE_ID;
    }

}
