package com.gokul.personalvault.data.local

/** In-memory V2 key session. The key is never written to Firebase. */
object VaultKeySessionV2 {
    private var activeVaultId: String? = null
    private var activeUid: String? = null
    private var keyBytes: ByteArray? = null

    @Synchronized fun isUnlocked(vaultId: String, uid: String): Boolean =
        activeVaultId == vaultId && activeUid == uid && keyBytes?.size == 32

    @Synchronized fun install(vaultId: String, uid: String, key: ByteArray) {
        require(key.size == 32) { "Invalid vault key." }
        clear()
        activeVaultId = vaultId
        activeUid = uid
        keyBytes = key.copyOf()
    }

    /** Returns a defensive copy. Caller must clear the returned bytes when finished. */
    @Synchronized fun copyKey(vaultId: String, uid: String): ByteArray? {
        if (!isUnlocked(vaultId, uid)) return null
        return keyBytes?.copyOf()
    }

    @Synchronized fun clear() {
        keyBytes?.fill(0)
        keyBytes = null
        activeVaultId = null
        activeUid = null
    }
}
