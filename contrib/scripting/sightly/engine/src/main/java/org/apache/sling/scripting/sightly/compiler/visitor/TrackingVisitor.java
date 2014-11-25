/*******************************************************************************
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
 ******************************************************************************/

package org.apache.sling.scripting.sightly.compiler.visitor;

import org.apache.sling.scripting.sightly.compiler.api.ris.Command;
import org.apache.sling.scripting.sightly.compiler.api.ris.command.BufferControl;
import org.apache.sling.scripting.sightly.compiler.api.ris.command.Loop;
import org.apache.sling.scripting.sightly.compiler.api.ris.command.VariableBinding;
import org.apache.sling.scripting.sightly.compiler.util.VariableTracker;
import org.apache.sling.scripting.sightly.compiler.api.ris.Command;
import org.apache.sling.scripting.sightly.compiler.api.ris.command.Loop;
import org.apache.sling.scripting.sightly.compiler.util.VariableTracker;

/**
 * Command visitor which tracks variables in commands
 */
public abstract class TrackingVisitor<T> extends UniformVisitor {

    protected final VariableTracker<T> tracker = new VariableTracker<T>();

    @Override
    public void visit(VariableBinding.Start variableBindingStart) {
        super.visit(variableBindingStart);
        tracker.pushVariable(variableBindingStart.getVariableName(), assignDefault(variableBindingStart));
    }

    @Override
    public void visit(VariableBinding.End variableBindingEnd) {
        super.visit(variableBindingEnd);
        tracker.popVariable();
    }

    @Override
    public void visit(Loop.Start loopStart) {
        super.visit(loopStart);
        tracker.pushVariable(loopStart.getIndexVariable(), assignDefault(loopStart));
        tracker.pushVariable(loopStart.getItemVariable(), assignDefault(loopStart));
    }

    @Override
    public void visit(Loop.End loopEnd) {
        super.visit(loopEnd);
        tracker.popVariable();
        tracker.popVariable();
    }

    @Override
    public void visit(BufferControl.Pop bufferPop) {
        super.visit(bufferPop);
        tracker.pushVariable(bufferPop.getVariableName(), assignDefault(bufferPop));
    }

    protected abstract T assignDefault(Command command);
}