package com.perenniasystems.lascito.data.repository

import com.perenniasystems.lascito.data.model.Memory
import com.perenniasystems.lascito.data.model.MediaType

/**
 * In-memory [MemoryRepository] for Phase 1 development.
 * Returns curated mock memories per profileId.
 * Replace with FirestoreMemoryRepository in Phase 2.
 */
class MockMemoryRepository : MemoryRepository {

    override suspend fun getMemoriesByProfile(profileId: String): List<Memory> =
        MOCK_MEMORIES[profileId] ?: emptyList()

    companion object {
        private val MOCK_MEMORIES: Map<String, List<Memory>> = mapOf(

            // ── Elena Ríos Marchetti (Abuela) ──────────────────────────────────
            "prof-001" to listOf(
                Memory(
                    memoryId        = "mem-001-a",
                    profileId       = "prof-001",
                    mediaType       = MediaType.IMAGE,
                    mediaUrl        = "https://images.unsplash.com/photo-1508214751196-bcfd4ca60f91?w=800",
                    storageClass    = "STANDARD",
                    contextText     = "Este fue nuestro último viaje juntos a la playa de Maldonado. " +
                            "Ella insistió en madrugar para ver el amanecer, y aunque todos nos quejamos, " +
                            "nadie olvidará esa luz dorada sobre el agua ni su risa cuando el frío nos " +
                            "hizo correr de vuelta a las mantas. Tenía esa magia de convertir cualquier " +
                            "momento ordinario en algo sagrado.",
                    contextAudioUrl = null,
                    capturedDate    = 1609459200000L  // 2021-01-01
                ),
                Memory(
                    memoryId        = "mem-001-b",
                    profileId       = "prof-001",
                    mediaType       = MediaType.IMAGE,
                    mediaUrl        = "https://images.unsplash.com/photo-1556911220-bda9f7f25ed2?w=800",
                    storageClass    = "STANDARD",
                    contextText     = "Las manos de la abuela Elena tejiendo en el sillón de la sala. " +
                            "Durante décadas ese sonido de agujas fue la banda sonora de nuestros inviernos. " +
                            "Cada bufanda que dio era una plegaria silenciosa de amor que llevamos " +
                            "puesta todavía.",
                    contextAudioUrl = null,
                    capturedDate    = 1577836800000L  // 2020-01-01
                ),
                // VIDEO — thumbnail de Unsplash, mediaUrl simularía un .mp4 en S3
                Memory(
                    memoryId        = "mem-001-v",
                    profileId       = "prof-001",
                    mediaType       = MediaType.VIDEO,
                    mediaUrl        = "https://images.unsplash.com/photo-1528360983277-13d401cdc186?w=800",
                    storageClass    = "STANDARD",
                    contextText     = "El cumpleaños número setenta y cinco. Todos los nietos cantamos " +
                            "'Feliz cumpleaños' desafinados y ella se rió tanto que se le cayeron las " +
                            "lágrimas. Ese videoclip de treinta segundos vale más que cualquier álbum.",
                    contextAudioUrl = null,
                    capturedDate    = 1596240000000L  // 2020-08-01
                ),
                // AUDIO — nota de voz grabada por la abuela (simulada)
                Memory(
                    memoryId        = "mem-001-au",
                    profileId       = "prof-001",
                    mediaType       = MediaType.AUDIO,
                    mediaUrl        = "https://mock-cdn.lascito.app/audio/mem-001-au.m4a",
                    storageClass    = "GLACIER_DEEP_ARCHIVE",
                    contextText     = "Una nota de voz que la abuela dejó grabada para los bisnietos que " +
                            "aún no habían nacido. Su propia voz, serena y cálida, contando la historia " +
                            "de cómo llegó a este país con dos maletas y mucha fe. Escucharla es " +
                            "sentirla presente.",
                    contextAudioUrl = null,
                    capturedDate    = 1545091200000L  // 2018-12-18
                ),
                Memory(
                    memoryId        = "mem-001-c",
                    profileId       = "prof-001",
                    mediaType       = MediaType.IMAGE,
                    mediaUrl        = "https://images.unsplash.com/photo-1504674900247-0877df9cc836?w=800",
                    storageClass    = "GLACIER_DEEP_ARCHIVE",
                    contextText     = "Su receta de tarta de manzana nunca fue anotada. Vivía en sus manos, " +
                            "en la presión exacta que aplicaba a la masa, en el olor que llenaba la casa " +
                            "cada domingo de otoño. Intentamos reproducirla mil veces y nunca fue igual. " +
                            "Ahora entendemos que parte de ella se fue con ella.",
                    contextAudioUrl = null,
                    capturedDate    = 1546300800000L  // 2019-01-01
                )
            ),

            // ── Roberto Vargas Solís (Padre) ───────────────────────────────────
            "prof-002" to listOf(
                Memory(
                    memoryId        = "mem-002-a",
                    profileId       = "prof-002",
                    mediaType       = MediaType.IMAGE,
                    mediaUrl        = "https://images.unsplash.com/photo-1519699047748-de8e457a634e?w=800",
                    storageClass    = "STANDARD",
                    contextText     = "Era el primero en llegar a mis partidos de fútbol y el último en irse. " +
                            "No importaba si llovía, si el equipo perdía, si yo había jugado mal — ahí estaba, " +
                            "siempre en el mismo rincón de la cancha, con los brazos cruzados y esa sonrisa " +
                            "tranquila que significaba que todo estaba bien.",
                    contextAudioUrl = null,
                    capturedDate    = 1514764800000L  // 2018-01-01
                ),
                Memory(
                    memoryId        = "mem-002-b",
                    profileId       = "prof-002",
                    mediaType       = MediaType.IMAGE,
                    mediaUrl        = "https://images.unsplash.com/photo-1493809842364-78817add7ffb?w=800",
                    storageClass    = "STANDARD",
                    contextText     = "La madrugada en que llegué con mi primer auto, un Volkswagen desvencijado " +
                            "que compré con mis ahorros de meses. Papá salió en pijama, dio una vuelta completa " +
                            "alrededor del auto, tocó el capó y dijo: 'Es tuyo. Cuídalo.' Fueron las palabras " +
                            "más orgullosas que me dio.",
                    contextAudioUrl = null,
                    capturedDate    = 1483228800000L  // 2017-01-01
                )
            ),

            // ── Mochi (Gato / Pet) ─────────────────────────────────────────────
            "prof-003" to listOf(
                Memory(
                    memoryId        = "mem-003-a",
                    profileId       = "prof-003",
                    mediaType       = MediaType.IMAGE,
                    mediaUrl        = "https://images.unsplash.com/photo-1514888286974-6c03e2ca1dba?w=800",
                    storageClass    = "STANDARD",
                    contextText     = "Mochi tenía la costumbre de dormir exactamente encima del libro " +
                            "que estabas leyendo. Era su forma de decir que merecía más atención que " +
                            "cualquier historia inventada. Y siempre tenía razón.",
                    contextAudioUrl = null,
                    capturedDate    = 1640995200000L  // 2022-01-01
                ),
                Memory(
                    memoryId        = "mem-003-b",
                    profileId       = "prof-003",
                    mediaType       = MediaType.IMAGE,
                    mediaUrl        = "https://images.unsplash.com/photo-1513360371669-4adf3dd7dff8?w=800",
                    storageClass    = "STANDARD",
                    contextText     = "El último invierno, cuando ya no saltaba pero todavía ronroneaba, " +
                            "pasaba las tardes envuelto en su manta azul cerca de la calefacción. " +
                            "Creo que en esos momentos quietos nos enseñó más sobre vivir el presente " +
                            "que cualquier libro de filosofía.",
                    contextAudioUrl = null,
                    capturedDate    = 1672531200000L  // 2023-01-01
                )
            ),

            // ── Carlos Mendoza Ríos (Tío, vivo) ────────────────────────────────
            "prof-004" to listOf(
                Memory(
                    memoryId        = "mem-004-a",
                    profileId       = "prof-004",
                    mediaType       = MediaType.IMAGE,
                    mediaUrl        = "https://images.unsplash.com/photo-1528605248644-14dd04022da1?w=800",
                    storageClass    = "STANDARD",
                    contextText     = "Tío Carlos y sus noches de dominó interminables. Llegaba sin avisar, " +
                            "ponía las fichas sobre la mesa y decía 'vamos'. Nadie le podía ganar, " +
                            "aunque sospecho que a veces perdía a propósito para que los niños " +
                            "no perdieran el interés en el juego.",
                    contextAudioUrl = null,
                    capturedDate    = 1704153600000L  // 2024-01-02
                )
            )
        )
    }
}
