/*
 * Copyright 2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gradle.play.internal.routes;

import org.gradle.api.internal.tasks.SimpleWorkResult;
import org.gradle.api.tasks.WorkResult;
import org.gradle.language.base.internal.compile.Compiler;

import java.io.File;
import java.io.Serializable;

public class PlayRoutesCompiler implements Compiler<PlayRoutesCompileSpec>, Serializable {
    public WorkResult execute(PlayRoutesCompileSpec spec) {
        boolean didWork = false;

        try {

            Iterable<File> sources = spec.getSources();
            for (File sourceFile : sources) {
                System.out.println(sourceFile.getAbsoluteFile()); //TODO
            }
        } catch (Exception e) {
            throw new RuntimeException(e.getCause());
        }

        return new SimpleWorkResult(didWork);
    }
}
