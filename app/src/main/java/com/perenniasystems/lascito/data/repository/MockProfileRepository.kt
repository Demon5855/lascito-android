package com.perenniasystems.lascito.data.repository

import com.perenniasystems.lascito.data.model.Profile
import com.perenniasystems.lascito.data.model.ProfileStatus
import com.perenniasystems.lascito.data.model.ProfileType

/**
 * In-memory implementation of [ProfileRepository] used during Phase 1 development.
 *
 * Provides 4 realistic profiles (3 humans + 1 pet) so the Dashboard UI
 * can be built and tested without a live backend.
 *
 * Replace this class with FirestoreProfileRepository in Phase 2.
 */
class MockProfileRepository : ProfileRepository {

    override suspend fun getProfiles(userId: String): List<Profile> = MOCK_PROFILES

    override suspend fun getProfileById(profileId: String): Profile? =
        MOCK_PROFILES.find { it.profileId == profileId }

    override suspend fun save(profile: Profile): String = profile.profileId

    companion object {
        /**
         * Dates are Unix timestamps in milliseconds.
         *
         * Profile 1  – Abuela Elena (grandmother, human)
         * Profile 2  – Papá Roberto (father, human)
         * Profile 3  – Mochi (cat, pet)
         * Profile 4  – Tío Carlos (uncle, still living — passingDate is null)
         */
        private val MOCK_PROFILES = listOf(
            Profile(
                profileId      = "prof-001",
                creatorUserId  = "mock-user-123",
                fullName       = "Elena Ríos Marchetti",
                profileType    = ProfileType.HUMAN,
                relationship   = "Abuela",
                birthDate      = 599616000000L,   // 1989-01-01 — placeholder
                passingDate    = 1672531200000L,  // 2023-01-01
                avatarUrl      = "https://i.pravatar.cc/300?img=47",
                status         = ProfileStatus.ACTIVE
            ),
            Profile(
                profileId      = "prof-002",
                creatorUserId  = "mock-user-123",
                fullName       = "Roberto Vargas Solís",
                profileType    = ProfileType.HUMAN,
                relationship   = "Padre",
                birthDate      = 347155200000L,   // ~1981
                passingDate    = 1704153600000L,  // ~2024
                avatarUrl      = "https://i.pravatar.cc/300?img=12",
                status         = ProfileStatus.ACTIVE
            ),
            Profile(
                profileId      = "prof-003",
                creatorUserId  = "mock-user-123",
                fullName       = "Mochi",
                profileType    = ProfileType.PET,
                relationship   = "Compañero fiel",
                birthDate      = 1357084800000L,  // ~2013
                passingDate    = 1706745600000L,  // ~2024
                avatarUrl      = "https://placekitten.com/300/300",
                status         = ProfileStatus.ACTIVE
            ),
            Profile(
                profileId      = "prof-004",
                creatorUserId  = "mock-user-123",
                fullName       = "Carlos Mendoza Ríos",
                profileType    = ProfileType.HUMAN,
                relationship   = "Tío",
                birthDate      = 441849600000L,   // ~1984
                passingDate    = null,            // still alive
                avatarUrl      = "https://i.pravatar.cc/300?img=57",
                status         = ProfileStatus.READ_ONLY
            )
        )
    }
}
