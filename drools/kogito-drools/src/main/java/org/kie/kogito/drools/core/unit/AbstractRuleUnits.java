/*
 * Copyright 2019 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.kie.kogito.drools.core.unit;

import java.util.HashMap;
import java.util.Map;

import org.kie.kogito.rules.RuleUnit;
import org.kie.kogito.rules.RuleUnitInstance;
import org.kie.kogito.rules.RuleUnits;

public abstract class AbstractRuleUnits implements RuleUnits {

    private Map<String, RuleUnitInstance<?>> unitRegistry = new HashMap<>();

    @Override
    public <T extends org.drools.ruleunits.api.RuleUnitData> RuleUnit<T> create(Class<T> clazz) {
        return (RuleUnit<T>) create(clazz.getCanonicalName());
    }

    protected abstract RuleUnit<?> create(String fqcn);

    @Override
    public void register(String name, RuleUnitInstance<?> unitInstance) {
        if (name == null) {
            throw new IllegalArgumentException("Cannot register a RuleUnitInstance with a null name");
        }
        unitRegistry.put(name, unitInstance);
    }

    @Override
    public RuleUnitInstance<?> getRegisteredInstance(String name) {
        return unitRegistry.get(name);
    }
}
