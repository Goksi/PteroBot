package tech.goksi.pterobot.entities

import com.mattmalec.pterodactyl4j.application.entities.ApplicationEgg
import com.mattmalec.pterodactyl4j.application.entities.ApplicationUser
import com.mattmalec.pterodactyl4j.application.entities.Node
import com.mattmalec.pterodactyl4j.entities.Allocation

@Suppress("MemberVisibilityCanBePrivate")
class ServerCreate {
    companion object {
        const val NOT_SET = "N/A"
    }

    private var _owner: ApplicationUser? = null
    private var _egg: ApplicationEgg? = null
    private var _node: Node? = null
    private var _primaryAllocation: Allocation? = null
    val owner
        get() = _owner?.userName ?: NOT_SET
    val egg
        get() = _egg?.name ?: NOT_SET
    val node
        get() = _node?.name ?: NOT_SET
    val primaryAllocation
        get() = _primaryAllocation?.fullAddress ?: NOT_SET
    var serverName: String = NOT_SET
    var serverDescription: String = NOT_SET
    var memory: Long = -1
    var disk: Long = -1

    fun setOwner(user: ApplicationUser) {
        this._owner = user
    }

    fun setNode(node: Node) {
        this._node = node
    }

    fun setAllocation(allocation: Allocation) {
        _primaryAllocation = allocation
    }

    fun removeAllocation() {
        _primaryAllocation = null
    }

    fun getNode() = _node

    fun canCreate(): Boolean {
        return isSet(owner) && isSet(egg) && isSet(node) && isSet(primaryAllocation) && isSet(serverName) &&
                memory != -1L && disk != -1L
    }

    private fun isSet(text: String) = text != NOT_SET
}
