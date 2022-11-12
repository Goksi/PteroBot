package tech.goksi.pterobot.entities

import tech.goksi.pterobot.util.Checks

class SemVer(version: String) : Comparable<SemVer> {

    private val versionList: List<String> = version.split(".")

    init {
        Checks.arguments(versionList.isNotEmpty(), "Invalid semantic version format")
    }

    override fun compareTo(other: SemVer): Int {
        var result = 0

        for (i in 0..2) {
            val v1 = versionList[i].toInt()
            val v2 = other.versionList[i].toInt()
            val compare = v1.compareTo(v2)
            if (compare != 0) {
                result = compare
                break
            }
        }
        return result
    }
}