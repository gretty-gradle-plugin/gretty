package org.akhikhl.gretty.internal.integrationTests

import org.gradle.api.Action
import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.compile.GroovyCompile
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.api.tasks.testing.Test
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.util.function.Consumer

class JavaToolchainIntegrationTestPlugin implements Plugin<Project> {
    public static final String PLUGIN_ID = "org.gretty.internal.integrationTests.JavaToolchainIntegrationTestPlugin"
    private static final Logger log = LoggerFactory.getLogger(IntegrationTestPlugin)

    public static void applyPluginConditionally(Project project) {
        if (project.findProperty('toolchainJavaVersion')) {
            project.apply plugin: PLUGIN_ID
        }
    }

    public static void whenApplied(Project project, Consumer<JavaToolchainIntegrationTestPlugin> config) {
        project.plugins.withId(PLUGIN_ID, new Action<Plugin>() {
            @Override
            void execute(Plugin plugin) {
                config.accept((JavaToolchainIntegrationTestPlugin) plugin)
            }
        })
    }

    @Override
    public void apply(Project project) {
        int javaVersion = Integer.parseInt("${project.toolchainJavaVersion}")

        project.java {
            toolchain {
                languageVersion = JavaLanguageVersion.of(javaVersion)
            }
        }
    }

    public void forceSourceSetToUseGradleJvm(Project project, SourceSet sourceSet) {
        project.tasks.named(sourceSet.getCompileTaskName('java')).configure({ forceTaskToUseGradleJvm(it) })
        project.tasks.named(sourceSet.getCompileTaskName('groovy')).configure({ forceTaskToUseGradleJvm(it) })
    }

    public void forceTaskToUseGradleJvm(Task task) {
        task.project.with { proj ->
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

    public static JavaVersion getToolchainJavaVersion(Project project) {
        return Optional.ofNullable(project.findProperty('toolchainJavaVersion'))
                .map({ Integer.parseInt("$it") })
                .map({ JavaVersion.toVersion(it) })
                .orElse(JavaVersion.current())
    }

    public static JavaVersion getGradleJavaVersion() {
        return JavaVersion.current()
    }

    public static void enableOnlyJavaToolchainAwareProjects(Project project) {
        ([project] + project.subprojects)*.afterEvaluate({ p ->
            p.tasks.configureEach { t ->
                enableOnlyJavaToolchainAwareTasks(t)
            }
        })
    }

    private static void enableOnlyJavaToolchainAwareTasks(Task task) {
        task.project.with {
            def initialFlag = task.enabled
            if (task.project.findProperty('toolchainJavaVersion')) {
                task.enabled = false
            }

            whenApplied(task.project) {
                task.enabled = initialFlag
            }
        }
    }

    private static def getGradleJvmSpec() {
        def gradleJvmVerson = Integer.valueOf(getGradleJavaVersion().getMajorVersion())
        return { languageVersion = JavaLanguageVersion.of(gradleJvmVerson) }
    }
}
