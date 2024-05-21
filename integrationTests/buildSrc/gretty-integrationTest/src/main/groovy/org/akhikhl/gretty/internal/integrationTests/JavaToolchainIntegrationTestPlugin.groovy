package org.akhikhl.gretty.internal.integrationTests

import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.compile.GroovyCompile
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.api.tasks.testing.Test
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class JavaToolchainIntegrationTestPlugin extends BasePlugin {
    private static final Logger log = LoggerFactory.getLogger(IntegrationTestPlugin)

    protected void configureExtensions(Project project) {
        if (project.findProperty('toolchainJavaVersion')) {
            defineToolchainDSL(project, Integer.parseInt("${project.toolchainJavaVersion}"))
        }
    }

    private void defineToolchainDSL(Project project, int javaVersion) {
        project.java {
            toolchain {
                languageVersion = JavaLanguageVersion.of(javaVersion)
            }
        }
    }

    public static void forceSourceSetToUseGradleJvm(Project project, SourceSet sourceSet) {
        if (isPluginApplied(project)) {
            project.tasks.named(sourceSet.getCompileTaskName('java')).configure({ forceTaskToUseGradleJvm(it) })
            project.tasks.named(sourceSet.getCompileTaskName('groovy')).configure({ forceTaskToUseGradleJvm(it) })
        }
    }

    public static void forceTaskToUseGradleJvm(Task task) {
        task.project.with { proj ->
            if (isPluginApplied(proj)) {
                if (task instanceof JavaCompile) {
                    task.javaCompiler.value(proj.javaToolchains.compilerFor(gradleJvmSpec))
                }

                if (task instanceof GroovyCompile) {
                    task.javaLauncher.value(proj.javaToolchains.launcherFor(gradleJvmSpec))
                }

                if (task instanceof Test) {
                    task.javaLauncher.value(proj.javaToolchains.launcherFor(gradleJvmSpec))
                }
            }
        }
    }

    public static JavaVersion getToolchainJavaVersion(Project project) {
        return Optional.ofNullable(project.findProperty('toolchainJavaVersion'))
                .map({ Integer.parseInt("$it") })
                .map({ JavaVersion.toVersion(it) })
                .orElse(JavaVersion.current())
    }

    public static JavaVersion getGradleJavaVersion() {
        return JavaVersion.current()
    }

    private static def getGradleJvmSpec() {
        def gradleJvmVerson = Integer.valueOf(getGradleJavaVersion().getMajorVersion())
        return { languageVersion = JavaLanguageVersion.of(gradleJvmVerson) }
    }

    private static boolean isPluginApplied(Project project) {
        return project.plugins.hasPlugin(JavaToolchainIntegrationTestPlugin.class) ||
                project.plugins.hasPlugin(JavaToolchainIntegrationTestPlugin.class.name)
    }
}
