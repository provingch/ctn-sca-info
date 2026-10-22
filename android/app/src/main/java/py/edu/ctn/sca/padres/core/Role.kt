package py.edu.ctn.sca.padres.core

enum class RoleAccent { BORDO, AZUL }

/** Rol de la cuenta según el `level` del JWT. ADMIN existe solo para rechazarlo: no tiene app. */
enum class Role(val level: Int, val label: String, val accent: RoleAccent) {
    PROFESOR(1, "Profesor", RoleAccent.BORDO),
    EVALUADOR(2, "Evaluador", RoleAccent.AZUL),
    ADMIN(3, "Administrador", RoleAccent.AZUL),
    PADRE(4, "Padre", RoleAccent.BORDO),
    COORDINACION(5, "Coordinación Pedagógica", RoleAccent.AZUL),
    ;

    companion object {
        fun fromLevel(level: Int?): Role? = entries.firstOrNull { it.level == level }

        /** Roles que pueden usar la app. */
        fun isSupported(level: Int?): Boolean = fromLevel(level).let { it != null && it != ADMIN }
    }
}
