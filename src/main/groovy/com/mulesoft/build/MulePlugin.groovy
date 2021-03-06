/*
 * Copyright 2015 juancavallotti.
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
package com.mulesoft.build

import com.mulesoft.build.dependency.MuleDependencyPlugin
import com.mulesoft.build.deploy.MuleDeployPlugin
import com.mulesoft.build.run.MuleRunPlugin
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.artifacts.Configuration
import org.gradle.api.file.FileCollection
import org.gradle.api.internal.artifacts.publish.ArchivePublishArtifact
import org.gradle.api.internal.plugins.DefaultArtifactPublicationSet
import org.gradle.api.plugins.BasePlugin
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.bundling.AbstractArchiveTask
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * Created by juancavallotti on 02/01/14.
 */
class MulePlugin implements Plugin<Project> {

    Logger logger = LoggerFactory.getLogger(MulePlugin.class)

    void apply(Project project) {

        //apply the java plugin.
        project.apply(plugin: JavaPlugin)

        //add the mule extension, only if not present
        if (project.extensions.findByType(MulePluginExtension) == null) {
            project.extensions.create('mule', MulePluginExtension)
        }

        //add the mule plugin convention.
        project.convention.create('muleConvention', MulePluginConvention)

        //apply plugins that also read the config

        //add the mule-esb dependencies
        project.apply(plugin: MuleDependencyPlugin)

        //add the tasks related to deployment
        project.apply(plugin: MuleDeployPlugin)

        //add the tasks related to execution
        project.apply(plugin: MuleRunPlugin)


        //add providedCompile and providedRuntime for dependency management.
        //this is needed because we'll be generating a container - based archive.
        project.configurations {

            providedCompile {
                description = 'Compile time dependencies that should not be part of the final zip file.'
                visible = false
            }

            providedRuntime {
                description = 'Runtime dependencies that should not be part of the final zip file.'
                visible = false
                extendsFrom providedCompile
            }

            providedTestCompile {
                description = 'Compile time test dependencies that are already provided by tooling I.E. MuleStudio.'
                visible = false
            }

            providedTestRuntime {
                description = 'Runtime time test dependencies that are already provided by tooling I.E. MuleStudio.'
                visible = false
                extendsFrom providedTestCompile

            }

            compile {
                extendsFrom providedCompile
            }

            runtime {
                extendsFrom providedRuntime
            }

            testCompile {
                extendsFrom providedTestCompile
            }

            testRuntime {
                extendsFrom providedTestRuntime
            }

        }

        Task ziptask = addZipDistributionTask(project)

        ArchivePublishArtifact zipArtifact = new ArchivePublishArtifact(ziptask as AbstractArchiveTask)
        //make it believe it is a war
        zipArtifact.setType("war")

        project.extensions.getByType(DefaultArtifactPublicationSet.class).addCandidate(zipArtifact)


        //add the sample project task.
        Task initProjectTask = project.tasks.create('initMuleProject', InitProjectTask)
        initProjectTask.description = 'Create the necessary directory structures suitable for a mule project. ' +
                'Includes sample files.'
        initProjectTask.group = MulePluginConstants.MULE_GROUP

        //configure test resources.s
        addTestResources(project)
    }

    private Task addZipDistributionTask(Project project) {
        //the packaging logic.
        Task ziptask = project.tasks.create('mulezip', MuleZip.class)

        ziptask.dependsOn project.check

        ziptask.classpath {

            FileCollection runtimeClasspath = project.convention.getPlugin(JavaPluginConvention.class)
                    .sourceSets.getByName(SourceSet.MAIN_SOURCE_SET_NAME).runtimeClasspath

            Configuration providedRuntime = project.configurations.getByName(
                    'providedRuntime');

            runtimeClasspath -= providedRuntime;

        }

        ziptask.description = 'Generate a deployable zip archive for this Mule APP'
        ziptask.group = BasePlugin.BUILD_GROUP

        return ziptask
    }


    private void addTestResources(Project project) {

        //include any zip that might be on plugins as external classpath.
        Task unpackPluginJars = project.tasks.create('unpackPluginJars', UnpackPluginJarsTask.class)

        unpackPluginJars.doLast {
            logger.debug('Adding unpacked plugin jars as part of the test classpath...')
            project.test.classpath = project.test.classpath + unpackPluginJars.pluginJars
        }

        project.tasks.test.dependsOn unpackPluginJars

        project.afterEvaluate { proj ->
            proj.sourceSets {
                test {
                    resources {
                        MulePluginConvention convention = project.convention.getByName('muleConvention')
                        srcDirs += convention.appResourcesDirectory()
                    }
                }
            }
        }
    }
}