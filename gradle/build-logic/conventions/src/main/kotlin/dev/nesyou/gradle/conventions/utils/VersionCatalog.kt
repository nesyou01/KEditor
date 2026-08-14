package dev.nesyou.gradle.conventions.utils

import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.getByType

internal val Project.libs: VersionCatalog
    get() = extensions.getByType<VersionCatalogsExtension>().named("libs")

internal fun VersionCatalog.getVersion(name: String): String =
    findVersion(name).get().requiredVersion


internal fun VersionCatalog.getVersionAsInt(name: String): Int =
    getVersion(name).toInt()

