package com.marsa.smarttrackerhub.domain


/**
 * Created by Muhammed Shafi on 13/11/2025.
 * Moro Hub
 * muhammed.poyil@morohub.com
 */
enum class AccessCode(val code: String, val roleName: String, val level: Int) {
    GROCERY("GR1357", "grocery", 1),
    CAFE("CF2468", "cafe", 1),
    SUPERMARKET("SM7531", "supermarket", 1),
    OPS_UAE("OPS1357", "ops_uae", 2),
    OPS_KUWAIT("OPS2468", "ops_kuwait", 2),
    ADMIN("OPS4243", "admin", 3),
    GUEST("100", "guest", 0);

    companion object {
        // Returns GUEST for any unrecognized code
        fun fromCode(code: String): AccessCode {
            return entries.find { it.code == code } ?: GUEST
        }

        // Returns GUEST for any unrecognized role
        fun fromRole(role: String): AccessCode {
            return entries.find { it.roleName.equals(role, ignoreCase = true) } ?: GUEST
        }
    }
}