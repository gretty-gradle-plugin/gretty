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

        project.rootProject.tasks.named('testAllJavaToolchain').configure {
            dependsOn project.tasks.testAll
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

    public static AnyJavaVersion getToolchainJavaVersion(Project project) {
        //java 8 compatible, Optional.or() available from java 9
        String majorVersion = project.findProperty('toolchainJavaVersion') ?: JavaVersion.current().majorVersion
        
        return Optional.ofNullable(majorVersion)
                .map({ Integer.parseInt("$it") })
                .map({ AnyJavaVersion.of(it) })
                .get()
    }

    public static JavaVersion getGradleJavaVersion() {
        return JavaVersion.current()
    }

    private static def getGradleJvmSpec() {
        def gradleJvmVerson = Integer.valueOf(getGradleJavaVersion().getMajorVersion())
        return { languageVersion = JavaLanguageVersion.of(gradleJvmVerson) }
    }
}
