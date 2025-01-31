/*
 * Copyright 2022 Red Hat, Inc. and/or its affiliates.
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
package org.kie.kogito.addon.source.files.deployment;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.kie.kogito.addon.source.files.SourceFile;
import org.kie.kogito.addon.source.files.SourceFilesRecorder;

final class FakeSourceFilesRecorder extends SourceFilesRecorder {

    private final Map<String, Collection<SourceFile>> files = new HashMap<>();

    @Override
    public void addSourceFile(String id, SourceFile sourceFile) {
        files.computeIfAbsent(id, k -> new ArrayList<>()).add(sourceFile);
    }

    boolean containsRecordFor(String processId, SourceFile sourceFile) {
        return files.getOrDefault(processId, List.of()).contains(sourceFile);
    }
}
