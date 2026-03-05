package com.perenniasystems.lascito.data.model

/**
 * Envoltorio inmutable para los dos objetos embebidos de Fideicomiso
 * leídos en una única consulta Firestore desde `users/{uid}`.
 *
 * Ambos campos son nullable para representar el estado "Sin configurar"
 * (documento existe pero los campos aún no han sido escritos).
 */
data class GuardianConfig(
    val guardian:      LegacyGuardian?,
    val deadManSwitch: DeadManSwitch?
)
