/*
 * Copyright 2010-2018 JetBrains s.r.o.
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

package org.jetbrains.kotlin.gradle.plugin

import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.tasks.TaskProvider
import org.jetbrains.kotlin.types.typesApproximation.approximateCapturedTypesIfNecessary

class TaskProviderHolder<T : Task> : TaskHolder<T> {
    private val provider: TaskProvider<T>
    private val name: String
    private val configurator: TaskConfigurator<T>

    constructor(project: Project, name: String, type: Class<T>, initAction: (T) -> (Unit)) : super(null, null) {
        configurator = TaskConfigurator(initAction)
        provider = project.tasks.register(name, type, configurator)
        this.name = name
    }

    override fun getTaskOrProvider(): Any = provider

    override fun doGetTask(): T {
        return provider.get()
    }

    override fun configure(action: (T) -> (Unit)) {
        configurator.appendAction(action, provider)
    }

    override fun toString(): String {
        return "TaskProviderHolder instane: [className: ${javaClass.name}, holded task name: '${name}']"
    }
}

//TODO may be it is useless!!!
// make sure that configure works as follows
class TaskConfigurator<T : Task> : (T) -> (Unit) {
    private val actionsList = ArrayList<(T) -> (Unit)>()
    private var executed = false

    constructor(defaultAction: (T) -> (Unit)) {
        actionsList.add(defaultAction)
    }

    @Synchronized
    override fun invoke(p1: T) {
        executed = true
        for (action in actionsList) {
            action.invoke(p1)
        }
    }

    @Synchronized
    fun appendAction(action: (T) -> (Unit), provider: TaskProvider<T>) {
        if (executed) {
            provider.configure(action)
        } else {
            actionsList.add(action)
        }
    }
}
