package pnote.projections.sandbox

import kotlinx.coroutines.channels.SendChannel

class RenderChannelBoxInitScope(private val renderChannel: SendChannel<RenderAction>) :
    BoxInitScope {
    override fun refreshScreen() {
        renderChannel.offer(RenderAction.Refresh)
    }

    override fun endProjection() {
        renderChannel.offer(RenderAction.Quit)
    }
}