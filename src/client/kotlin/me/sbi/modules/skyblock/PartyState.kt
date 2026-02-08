package me.sbi.modules.skyblock

/**
 * Party GUI state: pending invites, accept-all, incoming invites.
 * Updated via chat parsing.
 */
object PartyState {

    /** Names we invited (waiting for accept/decline/expire) - only add on "You have invited" success */
    val pendingOutgoing = mutableSetOf<String>()

    /** Incoming invites: who invited us */
    val incomingInvites = mutableListOf<Pair<String, Long>>()

    /** Accept all party invites automatically */
    var acceptAllInvites = false

    /** Last name we tried to invite - remove from pending when "cannot invite" (offline) */
    var lastInvitedName: String? = null

    fun addPendingInvite(name: String) {
        pendingOutgoing.add(name)
    }

    fun removePendingInvite(name: String) {
        pendingOutgoing.removeAll { it.equals(name, ignoreCase = true) }
    }

    fun addIncomingInvite(fromName: String) {
        val clean = fromName.trim()
        if (clean.isNotEmpty() && !incomingInvites.any { it.first.equals(clean, ignoreCase = true) }) {
            incomingInvites.add(clean to System.currentTimeMillis())
        }
    }

    fun removeIncomingInvite(name: String) {
        incomingInvites.removeAll { it.first.equals(name, ignoreCase = true) }
    }

    fun clearPending() {
        pendingOutgoing.clear()
        incomingInvites.clear()
    }
}
