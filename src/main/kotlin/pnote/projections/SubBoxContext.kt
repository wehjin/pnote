package pnote.projections

import pnote.projections.sandbox.SubProjection

class SubBoxContext {
    private var subProjection: SubProjection? = null

    fun isProjecting(name: String): Boolean = (subProjection?.name ?: "") == name

    fun clear(name: String? = null) {
        if (name == null || subProjection?.name == name) {
            subProjection = null
        }
    }

    fun subProject(name: String, toSubProjection: () -> SubProjection) {
        if (!isProjecting(name)) {
            subProjection = toSubProjection()
        }
    }
}

