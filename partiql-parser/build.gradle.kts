/*
 * Copyright 2022 Amazon.com, Inc. or its affiliates.  All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at:
 *
 *      http://aws.amazon.com/apache2.0/
 *
 * or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific
 * language governing permissions and limitations under the License.
 */

plugins {
    id(Plugins.conventions)
    id(Plugins.publish)
    id(Plugins.kotlinLombok) version Versions.kotlinLombok
}

dependencies {
    api(project(":partiql-antlr", configuration = "shadow"))
    api(project(":partiql-ast"))
    api(project(":partiql-types"))
    implementation(Deps.ionElement)
    compileOnly(Deps.lombok)
    annotationProcessor(Deps.lombok)
}

// TODO: Remove
tasks.shadowJar {
    configurations = listOf(project.configurations.shadow.get())
}

// TODO: Remove
// Workaround for https://github.com/johnrengelman/shadow/issues/651
components.withType(AdhocComponentWithVariants::class.java).forEach { c ->
    c.withVariantsFromConfiguration(project.configurations.shadowRuntimeElements.get()) {
        skip()
    }
}

apiValidation {
    ignoredPackages.addAll(
        listOf(
            "org.partiql.parser.internal"
        )
    )
}

// TODO: Figure out
tasks.withType<Jar>().configureEach {
    // ensure "generateGrammarSource" is called before "sourcesJar".
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

// TODO: Figure out
tasks.processResources {
    from("src/main/antlr") {
        include("**/*.g4")
    }
}

publish {
    artifactId = "partiql-parser"
    name = "PartiQL Parser"
    description = "PartiQL's Parser"
    // `antlr` dependency configuration adds the ANTLR API configuration (and Maven `compile` dependency scope on
    // publish). It's a known issue w/ the ANTLR gradle plugin. Follow https://github.com/gradle/gradle/issues/820
    // for context. In the maven publishing step, any API or IMPLEMENTATION dependencies w/ "antlr4" non-runtime
    // dependency will be omitted from the created Maven POM.
    // excludedDependencies = setOf("antlr4")
}
