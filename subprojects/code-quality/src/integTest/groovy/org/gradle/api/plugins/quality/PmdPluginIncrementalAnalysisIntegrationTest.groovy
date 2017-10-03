/*
 * Copyright 2017 the original author or authors.
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
package org.gradle.api.plugins.quality

import org.gradle.util.Matchers
import org.gradle.util.TestPrecondition
import org.junit.Assume
import spock.lang.Unroll

import static org.hamcrest.Matchers.containsString
import static org.hamcrest.Matchers.not

class PmdPluginIncrementalAnalysisIntegrationTest extends AbstractPmdPluginVersionIntegrationTest {
    def setup() {
        buildFile << """
            apply plugin: "java"
            apply plugin: "pmd"

            ${mavenCentralRepository()}

            pmd {
                toolVersion = '$version'
            }
            
            ${!TestPrecondition.FIX_TO_WORK_ON_JAVA9.fulfilled ? "sourceCompatibility = 1.6" : ""}
        """.stripIndent()
    }

    def "incremental analysis cache file is not generated by default"() {
        goodCode()

        expect:
        succeeds("check")
        !file("build/tmp/pmdMain/incremental.cache").exists()
    }

    def "incremental analysis can be enabled"() {
        given:
        Assume.assumeTrue(fileLockingIssuesSolvedWithIncrementalAnalysis())
        Assume.assumeTrue(supportIncrementalAnalysis())
        enableIncrementalAnalysis()
        goodCode()

        when:
        args('--info')
        succeeds("pmdMain")
        file("build/tmp/pmdMain/incremental.cache").exists()

        then:
        output.contains('Analysis cache invalidated, rulesets changed')

        when:
        args('--rerun-tasks', '--info')
        succeeds("pmdMain")

        then:
        !output.contains('Analysis cache invalidated, rulesets changed')
    }

    def 'incremental analysis is transparent'() {
        given:
        Assume.assumeTrue(fileLockingIssuesSolvedWithIncrementalAnalysis())
        Assume.assumeTrue(supportIncrementalAnalysis())
        enableIncrementalAnalysis()
        goodCode()
        badCode()

        when:
        fails('pmdMain')

        then:
        file("build/reports/pmd/main.xml").assertContents(Matchers.containsText('BadClass'))

        when:
        file('src/main/java/org/gradle/BadClass.java').delete()
        succeeds('pmdMain')

        then:
        file("build/reports/pmd/main.xml").assertContents(not(containsString('BadClass')))
    }

    @Unroll
    def 'incremental analysis invalidated when #reason'() {
        given:
        Assume.assumeTrue(fileLockingIssuesSolvedWithIncrementalAnalysis())
        Assume.assumeTrue(supportIncrementalAnalysis())
        enableIncrementalAnalysis()
        goodCode()
        customRuleSet()

        when:
        succeeds('pmdMain')
        buildFile << "\npmd{${code}}"
        succeeds('pmdMain', '--info')

        then:
        outputContains("Analysis cache invalidated, ${reason}")


        where:
        reason                | code
        'PMD version changed' | 'toolVersion="5.8.0"'
        'rulesets changed'    | 'ruleSetFiles = files("customRuleSet.xml")'
    }

    def "incremental analysis is available in 5.6.0+"() {
        given:
        Assume.assumeTrue(fileLockingIssuesSolvedWithIncrementalAnalysis())
        Assume.assumeTrue(supportIncrementalAnalysis())
        enableIncrementalAnalysis()
        goodCode()

        expect:
        succeeds('pmdMain')
    }

    def "incremental analysis fails when enabled with < 5.6.0"() {
        given:
        Assume.assumeFalse(supportIncrementalAnalysis())
        enableIncrementalAnalysis()
        goodCode()

        when:
        fails('pmdMain')

        then:
        failure.error.contains('Incremental analysis only supports PMD 5.6.0+')
    }

    private goodCode() {
        file("src/main/java/org/gradle/GoodClass.java") <<
            "package org.gradle; class GoodClass { public boolean isFoo(Object arg) { return true; } }"
    }

    private badCode() {
        // PMD Lvl 2 Warning BooleanInstantiation
        // PMD Lvl 3 Warning OverrideBothEqualsAndHashcode
        file("src/main/java/org/gradle/BadClass.java") <<
            "package org.gradle; class BadClass { public boolean equals(Object arg) { return java.lang.Boolean.valueOf(true); } }"
    }

    private customRuleSet() {
        file("customRuleSet.xml") << """
            <ruleset name="custom"
                xmlns="http://pmd.sf.net/ruleset/1.0.0"
                xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                xsi:schemaLocation="http://pmd.sf.net/ruleset/1.0.0 http://pmd.sf.net/ruleset_xml_schema.xsd"
                xsi:noNamespaceSchemaLocation="http://pmd.sf.net/ruleset_xml_schema.xsd">

                <description>Custom rule set</description>

                <rule ref="rulesets/java/braces.xml"/>
            </ruleset>
        """
    }

    void enableIncrementalAnalysis() {
        buildFile << 'pmd { incrementalAnalysis = true }'
    }
}
