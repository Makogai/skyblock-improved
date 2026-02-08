package me.sbi.modules.skyblock

import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.MutableComponent

/**
 * Parsed party data from Hypixel SkyBlock chat.
 * Updated when party list or transfer message is received.
 */
object SkyBlockParty {

    data class PartyMember(val rank: String, val name: String) {
        val displayText: String get() = if (rank.isNotEmpty()) "$rank $name" else name
    }

    /** Party leader */
    var partyLeader: PartyMember = PartyMember("", "")
        private set

    /** Other party members (excluding leader). Always deduplicated, never contains leader. */
    var partyMembers: List<PartyMember> = emptyList()
        private set(value) {
            field = value
                .filter { it.name.isNotEmpty() && !it.name.equals(partyLeader.name, ignoreCase = true) }
                .distinctBy { it.name.lowercase() }
        }

    /** All party members including leader. Always deduplicated by name (case-insensitive). */
    val allMembers: List<PartyMember>
        get() {
            val leader = if (partyLeader.name.isEmpty()) null else partyLeader
            val members = partyMembers.filter { it.name.isNotEmpty() }
            val combined = if (leader != null) listOf(leader) + members else members
            return combined.distinctBy { it.name.lowercase() }
        }

    /** Display-safe list - never shows duplicates. Use for all UI rendering. */
    val displayMembers: List<PartyMember>
        get() = allMembers.distinctBy { it.name.lowercase() }

    /** Legacy: plain leader name */
    val leaderName: String get() = partyLeader.name

    /** True if we have parsed party data */
    val hasParty: Boolean
        get() = partyLeader.name.isNotEmpty() || partyMembers.isNotEmpty()

    /** For debug output (Java mixin) */
    @JvmStatic
    fun getAllMembersForDebug(): String = "leader=${partyLeader.displayText}, members=${partyMembers.map { it.displayText }}"

    private val CLEAN = Regex("§[0-9a-fk-or]")
    private val RANK = Regex("\\[([^\\]]+)\\]")
    /** Party Leader: [MVP++] Name . or Party Leader: Name • */
    private val PARTY_LEADER_PATTERN = Regex("Party Leader:\\s*(?:\\[[^\\]]+\\]\\s*)?([^\\s•.]+)")
    /** Party Members: [Rank] Name or Party Moderators: Name */
    private val PARTY_MEMBER_PATTERN = Regex("Party (?:Members|Moderators):\\s*(?:\\[[^\\]]+\\]\\s*)?([^\\s•.]+)")
    /** Standalone line: [Rank] Name (e.g. from /p l list) */
    private val RANK_NAME_LINE = Regex("(?:\\[([^\\]]+)\\]\\s*)?([^\\s•.]+)")
    /** The party was transferred to [Rank] NewLeader by OldLeader */
    private val TRANSFER_PATTERN = Regex("The party was transferred to (.+?) by ", RegexOption.IGNORE_CASE)
    /** [MVP+] Sherere joined the party. */
    private val JOINED_PATTERN = Regex("(.+?)\\s+joined the party\\.?", RegexOption.IGNORE_CASE)
    /** [MVP+] Sherere has left the party. */
    private val LEFT_PATTERN = Regex("(.+?)\\s+has left the party\\.?", RegexOption.IGNORE_CASE)

    /** Hypixel rank to color. MVP++=gold, MVP+/MVP=aqua, VIP+/VIP=green */
    private val RANK_COLORS = mapOf(
        "MVP++" to ChatFormatting.GOLD,
        "MVP+" to ChatFormatting.AQUA,
        "MVP" to ChatFormatting.AQUA,
        "VIP+" to ChatFormatting.GREEN,
        "VIP" to ChatFormatting.GREEN,
        "YOUTUBER" to ChatFormatting.RED,
        "MOD" to ChatFormatting.DARK_GREEN,
        "ADMIN" to ChatFormatting.RED,
    )

    fun rankToColor(rank: String): ChatFormatting {
        val r = rank.uppercase().replace(" ", "")
        return RANK_COLORS.entries.firstOrNull { r.contains(it.key) }?.value ?: ChatFormatting.WHITE
    }

    @JvmStatic
    fun parseFromChat(message: String) {
        val text = CLEAN.replace(message, "").trim()

        // [Rank] Name joined the party. - add member (never duplicate)
        val joinedMatch = JOINED_PATTERN.find(text)
        if (joinedMatch != null) {
            val whoPart = joinedMatch.groupValues[1].trim()
            val rankMatch = RANK.find(whoPart)
            val rank = rankMatch?.groupValues?.get(1)?.let { "[$it]" } ?: ""
            val name = if (rankMatch != null) whoPart.replace(rankMatch.value, "").trim() else whoPart.trim()
            if (name.isNotEmpty() && !partyLeader.name.equals(name, ignoreCase = true) &&
                !partyMembers.any { it.name.equals(name, ignoreCase = true) }) {
                partyMembers = (partyMembers + PartyMember(rank, name)).distinctBy { it.name.lowercase() }
            }
            return
        }

        // [Rank] Name has left the party. - remove that member only
        val leftMatch = LEFT_PATTERN.find(text)
        if (leftMatch != null) {
            val whoPart = leftMatch.groupValues[1].trim()
            val rankMatch = RANK.find(whoPart)
            val name = if (rankMatch != null) whoPart.replace(rankMatch.value, "").trim() else whoPart.trim()
            if (name.isNotEmpty()) {
                partyMembers = partyMembers.filter { !it.name.equals(name, ignoreCase = true) }
            }
            return
        }

        // Party transfer: "The party was transferred to [Rank] NewLeader by OldLeader"
        val transferMatch = TRANSFER_PATTERN.find(message)
        if (transferMatch != null) {
            val newLeaderPart = transferMatch.groupValues[1].trim()
            val clean = CLEAN.replace(newLeaderPart, "")
            val rankMatch = RANK.find(clean)
            val rank = rankMatch?.groupValues?.get(1)?.let { "[$it]" } ?: ""
            val name = if (rankMatch != null) clean.replace(rankMatch.value, "").trim() else clean.trim()
            if (name.isNotEmpty()) {
                val oldLeader = partyLeader
                val newLeaderName = name
                var members = partyMembers.filter { !it.name.equals(newLeaderName, ignoreCase = true) }
                if (oldLeader.name.isNotEmpty() && !oldLeader.name.equals(newLeaderName, ignoreCase = true) && !members.any { it.name.equals(oldLeader.name, ignoreCase = true) }) {
                    members = listOf(PartyMember(oldLeader.rank, oldLeader.name)) + members
                }
                partyLeader = PartyMember(rank, newLeaderName)
                partyMembers = members
                return
            }
        }

        val lines = message.split("\n")
        val hasHeader = lines.any { "Party Members (" in it }
        val hasPartyLine = lines.any { "Party Leader:" in it || "Party Members:" in it || "Party Moderators:" in it }
        if (!hasHeader && !hasPartyLine) return

        // Parse into temp vars - when hasHeader treat as fresh /p l output; only apply if we got data (else keep join-added members)
        var parsedLeader = if (hasHeader) PartyMember("", "") else partyLeader
        val parsedMembers = if (hasHeader) mutableListOf<PartyMember>() else partyMembers.toMutableList()

        for (line in lines) {
            val clean = CLEAN.replace(line, "").trim()
            if (clean.isEmpty()) continue
            val leaderMatch = PARTY_LEADER_PATTERN.find(clean)
            if (leaderMatch != null) {
                val full = clean.replace("Party Leader:", "").trim()
                val r = RANK.find(full)
                val rank = r?.groupValues?.get(1)?.let { "[$it]" } ?: ""
                val name = if (r != null) full.replace(r.value, "").trim() else full.trim()
                if (name.isNotEmpty()) parsedLeader = PartyMember(rank, name)
                continue
            }
            val memberMatch = PARTY_MEMBER_PATTERN.find(clean)
            if (memberMatch != null) {
                val full = clean.replace(Regex("Party (?:Members|Moderators):"), "").trim()
                val r = RANK.find(full)
                val rank = r?.groupValues?.get(1)?.let { "[$it]" } ?: ""
                val name = if (r != null) full.replace(r.value, "").trim() else full.trim()
                if (name.isNotEmpty() && name != parsedLeader.name && !parsedMembers.any { it.name.equals(name, ignoreCase = true) }) {
                    parsedMembers.add(PartyMember(rank, name))
                }
                continue
            }
            // Fallback: line is just [Rank] Name (common in /p l when members on separate lines)
            // Skip lines we already processed as leader/member to avoid duplicates
            if ((hasHeader || hasPartyLine) && !clean.contains("Party Leader:") && !clean.contains("Party Members:") && !clean.contains("Party Moderators:")) {
                val rn = RANK_NAME_LINE.find(clean)
                if (rn != null) {
                    val rank = rn.groupValues.getOrNull(1)?.let { "[$it]" } ?: ""
                    val name = rn.groupValues.getOrNull(2)?.trim() ?: ""
                    if (name.isNotEmpty() && name.any { it.isLetter() } &&
                        !name.equals("Party", ignoreCase = true) && !name.equals("Leader", ignoreCase = true) &&
                        !name.equals("Members", ignoreCase = true) && !name.equals("Moderators", ignoreCase = true) &&
                        !name.matches(Regex("^\\(?\\d+\\)?$")) &&
                        !parsedLeader.name.equals(name, ignoreCase = true) && !parsedMembers.any { it.name.equals(name, ignoreCase = true) }) {
                        parsedMembers.add(PartyMember(rank, name))
                    }
                }
            }
        }

        val finalMembers = parsedMembers
            .filter { !it.name.equals(parsedLeader.name, ignoreCase = true) }
            .distinctBy { it.name.lowercase() }
        if (parsedLeader.name.isNotEmpty() || finalMembers.isNotEmpty()) {
            partyLeader = parsedLeader
            partyMembers = finalMembers
        }
    }

    @JvmStatic
    fun clear() {
        partyLeader = PartyMember("", "")
        partyMembers = emptyList()
    }

    /** Build styled Component for display (Hypixel colors). MVP+/VIP+ get red + like Hypixel. */
    fun toComponent(member: PartyMember, isLeader: Boolean): Component {
        val nameStyle = if (isLeader) ChatFormatting.GOLD
            else rankToColor(member.rank).let { if (it == ChatFormatting.WHITE) ChatFormatting.GRAY else it }
        val styles = if (isLeader) arrayOf(nameStyle, ChatFormatting.BOLD) else arrayOf(nameStyle)
        val nameComp = Component.literal(member.name).withStyle(*styles)
        return if (member.rank.isEmpty()) nameComp
        else buildRankComponent(member.rank).append(Component.literal(" ")).append(nameComp)
    }

    /** [MVP+] -> [MVP] in aqua + [+] in red; [VIP+] similar; others single color */
    private fun buildRankComponent(rank: String): MutableComponent {
        val inner = rank.trim('[', ']')
        val plusColor = ChatFormatting.RED
        val baseColor = rankToColor(rank)
        return when {
            inner.endsWith("+") && inner != "+" -> {
                val base = inner.dropLast(1)
                Component.literal("[$base").withStyle(baseColor)
                    .append(Component.literal("+").withStyle(plusColor))
                    .append(Component.literal("]").withStyle(baseColor))
            }
            inner.endsWith("++") -> {
                Component.literal("[$inner]").withStyle(baseColor)
            }
            else -> Component.literal("[$inner]").withStyle(baseColor)
        }
    }
}
