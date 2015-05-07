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

package com.mulesoft.build.muleagent

import org.junit.Before
import org.junit.Test

import static org.junit.Assert.*

/**
 * Test the Mule Agent DSL
 *
 * Created by juancavallotti on 5/6/15.
 */
class TestAgentEnvironmentsDSL {

    MuleAgentPluginExtension extension

    @Before
    void setupTests() {
        extension = new MuleAgentPluginExtension()
    }


    @Test
    void testLocalEnvironmentDefinition() {

        extension.environments {
            local
        }

        assertTrue('Should contain local environment', extension.environments.containsKey('local'))

    }

    @Test
    void testLocalClusterDefinition() {

        extension.environments {
            local cluster {
                local
                local
            }
        }

        assertTrue('Should contain local environment', extension.environments.containsKey('local'))
        assertTrue('Environments Should be an array list of 2', extension.environments['local'].size() == 2)
    }


    @Test
    void testBasicDSLDefinition() {
        extension.environments {
            local baseUrl: 'http://localhost:8081/mule'
        }

        assertTrue('Should contain local environment', extension.environments.containsKey('local'))
    }

    @Test
    void testClusterDSLDefinition() {
        extension.environments {
            development cluster {
                node1 baseUrl: 'http://10.11.10.11:9999/mule'
                node2 baseUrl: 'http://10.11.10.12:9999/mule'
            }
        }

        assertTrue('Should only contain one environment', extension.environments.size() == 1)
        assertTrue('Should contain development cluster', extension.environments.containsKey('development'))
        assertTrue('Environments Should be an array list of 2', extension.environments['development'].size() == 2)
    }

    @Test
    void testMultipleClusterDSLDefinition() {
        extension.environments {
            development cluster {
                node1 baseUrl: 'http://10.11.10.11:9999/mule'
                node2 baseUrl: 'http://10.11.10.12:9999/mule'
            }

            production cluster {
                node1 baseUrl: 'http://12.11.10.11:9999/mule'
                node2 baseUrl: 'http://12.11.10.12:9999/mule'
            }
        }

        assertTrue('Should only contain two environments', extension.environments.size() == 2)
        assertTrue('Should contain development cluster', extension.environments.containsKey('development'))
        assertTrue('Should contain production cluster', extension.environments.containsKey('production'))
    }

}