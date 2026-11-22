package com.example.domain

import com.example.model.BalanceMission
import java.util.Calendar
import java.util.Locale

data class IsoWeekSeed(
  val isoYear: Int,
  val isoWeek: Int
) {
  val seedKey: String
    get() = "${isoYear}_w${isoWeek.toString().padStart(2, '0')}"

  val numericSeed: Int
    get() = isoYear * 53 + isoWeek
}

/**
 * Proveedor determinista del "Reto de la semana".
 * - Apunta a la dimensión real más baja del hexágono del usuario.
 * - Propone una acción profunda de 45-60 minutos dividida en 3-4 pasos.
 * - Otorga +80 EXP que NO cuentan para el cap diario de 150 (solo para nivel y hexágono).
 * - Rota cada lunes de forma determinista según la semana ISO local (compatible con minSdk 24).
 */
object WeeklyChallengeProvider {

  const val WEEKLY_CHALLENGE_EXP = 80
  private const val ID_PREFIX = "weekly_"

  private data class WeeklyTemplate(
    val title: String,
    val summary: String,
    val description: String,
    val steps: List<String>,
    val benefit: String,
    val durationMinutes: Int
  )

  private val dimensionTitles = mapOf(
    0 to "Autoestima",
    1 to "Ejercicio",
    2 to "Descanso",
    3 to "Relaciones",
    4 to "Hobbies",
    5 to "Excelencia"
  )

  private val templatesByDimension: Map<Int, List<WeeklyTemplate>> = mapOf(
    // 0: Autoestima
    0 to listOf(
      WeeklyTemplate(
        title = "Carta honesta de logros y límites",
        summary = "Dedica 45 min a reconocer tus avances reales y soltar una autoexigencia.",
        description = "Una sesión tranquila para mirar con objetividad lo que ya superaste este año y definir un límite amable que proteja tu paz.",
        steps = listOf(
          "Busca un lugar cómodo y apaga notificaciones durante 45 minutos.",
          "Escribe 5 momentos recientes donde actuaste con valor o cuidado.",
          "Identifica 1 exigencia poco justa contigo y reescríbela como un acuerdo realista.",
          "Cierra eligiendo una acción pequeña de autocuidado para esta semana."
        ),
        benefit = "Consolida una confianza basada en hechos concretos y compasión, no en perfección.",
        durationMinutes = 45
      ),
      WeeklyTemplate(
        title = "Paseo consciente sin comparación",
        summary = "Sal 50 min sin redes sociales para reconectar con tu propio ritmo.",
        description = "Caminar a solas sin mirar métricas ni vidas ajenas devuelve claridad sobre lo que realmente valoras hoy.",
        steps = listOf(
          "Deja el móvil en modo avión o dentro del bolsillo durante el recorrido.",
          "Camina 20 minutos observando detalles de tu entorno con calma.",
          "Siéntate 15 minutos y anota 3 decisiones recientes que nacieron de tu propio criterio.",
          "Regresa despacio reconociendo cómo cambió tu diálogo interno."
        ),
        benefit = "Reduce el ruido de la comparación externa y fortalece tu seguridad personal.",
        durationMinutes = 50
      ),
      WeeklyTemplate(
        title = "Inventario de fortalezas cotidianas",
        summary = "Dedica 60 min a mapear habilidades que ya usas y merecen reconocimiento.",
        description = "Muchas capacidades pasan desapercibidas por parecer normales; darles nombre reafirma tu valía diaria.",
        steps = listOf(
          "Divide una hoja en tres columnas: Lo que resuelvo, Lo que cuido y Lo que aprendí.",
          "Completa al menos 4 ejemplos reales en cada columna sin minimizar ninguno.",
          "Subraya las dos fortalezas que te gustaría honrar más esta semana.",
          "Guarda la lista en un lugar visible para días de duda."
        ),
        benefit = "Entrena una mirada más equilibrada y justa hacia tus propias capacidades.",
        durationMinutes = 60
      )
    ),
    // 1: Ejercicio
    1 to listOf(
      WeeklyTemplate(
        title = "Caminata larga de exploración urbana o natural",
        summary = "Camina 50 minutos por una ruta nueva o parque a ritmo cómodo.",
        description = "Una sesión continua sin presión de marcas deportivas ayuda a despertar la vitalidad física y despejar la mente.",
        steps = listOf(
          "Prepara calzado cómodo y una botella de agua antes de salir.",
          "Dedica 5 minutos iniciales a movilidad suave de tobillos, caderas y hombros.",
          "Camina 40 minutos continuos manteniendo una respiración cómoda.",
          "Termina con 5 minutos de estiramientos lentos en casa."
        ),
        benefit = "Mejora el ánimo, activa la circulación y libera tensión acumulada.",
        durationMinutes = 50
      ),
      WeeklyTemplate(
        title = "Sesión completa de movilidad y fuerza amable",
        summary = "45 min de calentamiento, circuito suave y relajación corporal.",
        description = "Mover grandes grupos musculares de forma progresiva fortalece el cuerpo sin agotamiento extremo.",
        steps = listOf(
          "Prepara un espacio despejado y música tranquila durante 5 minutos.",
          "Realiza 10 minutos de movilidad articular de cuello, espalda y cadera.",
          "Completa 20 minutos de ejercicios suaves (sentadillas, planchas apoyadas y remos).",
          "Cierra con 10 minutos de estiramiento profundo y respiración nasal."
        ),
        benefit = "Aumenta la energía corporal y previene rigidez postural durante la semana.",
        durationMinutes = 45
      ),
      WeeklyTemplate(
        title = "Ruta activa al aire libre con música o silencio",
        summary = "60 minutos de movimiento continuo en bicicleta, trote suave o caminata.",
        description = "Reservar una hora íntegra para tu cuerpo envía una señal clara de cuidado físico sostenido.",
        steps = listOf(
          "Elige el horario del día con mejor luz o temperatura agradable.",
          "Empieza los primeros 10 minutos a ritmo muy suave para entrar en calor.",
          "Mantén 40 minutos de movimiento constante disfrutando el recorrido.",
          "Dedica 10 minutos finales a hidratarte y soltar piernas."
        ),
        benefit = "Refuerza la resistencia física y favorece un sueño más reparador por la noche.",
        durationMinutes = 60
      )
    ),
    // 2: Descanso
    2 to listOf(
      WeeklyTemplate(
        title = "Noche analógica de desconexión total",
        summary = "60 minutos antes de dormir sin pantallas ni pendientes laborales.",
        description = "Regalarle al cerebro una hora libre de estímulos luminosos antes de acostarte transforma la profundidad del descanso.",
        steps = listOf(
          "Activa la alarma de mañana y deja teléfono y ordenador fuera de la cama.",
          "Atenúa las luces de casa con una lámpara cálida.",
          "Dedica 30 minutos a leer en papel, escuchar música serena o tomar una infusión.",
          "Realiza 5 minutos de respiración lenta ya acostado."
        ),
        benefit = "Facilita la transición al sueño profundo y reduce el cansancio al despertar.",
        durationMinutes = 60
      ),
      WeeklyTemplate(
        title = "Siesta o pausa restaurativa sin culpa",
        summary = "45 min dedicados exclusivamente a descansar cuerpo y atención.",
        description = "Aprender a detenerse a mitad de semana sin sentir que pierdes el tiempo devuelve claridad sostenida.",
        steps = listOf(
          "Elige un bloque de 45 minutos y avisa si compartes espacio.",
          "Cierra cortinas o usa luz tenue y silencia alertas del móvil.",
          "Descansa recostado 25-30 minutos (durmiendo o solo con ojos cerrados).",
          "Despierta sin prisa estirando brazos y tomando agua."
        ),
        benefit = "Corta la fatiga acumulada y mejora la serenidad emocional.",
        durationMinutes = 45
      ),
      WeeklyTemplate(
        title = "Ritual semanal de orden y descarga mental",
        summary = "50 min para despejar tu dormitorio y vaciar pendientes de la mente.",
        description = "Un espacio de descanso despejado y sin asuntos abiertos en la cabeza invita a dormir tranquilo.",
        steps = listOf(
          "Ventila la habitación y cambia o acomoda la ropa de cama.",
          "Retira objetos de trabajo, cables sueltos o pantallas del área de dormir.",
          "Anota en una hoja todos los pendientes de la semana para sacarlos de tu mente.",
          "Cierra la sesión con 10 minutos de calma absoluta."
        ),
        benefit = "Asocia tu espacio nocturno con calma real y reduce despertares con preocupaciones.",
        durationMinutes = 50
      )
    ),
    // 3: Relaciones
    3 to listOf(
      WeeklyTemplate(
        title = "Encuentro sin prisas con alguien importante",
        summary = "Comparte 60 min de café, paseo o llamada profunda con atención plena.",
        description = "Dedicar una hora sin multitarea fortalece los vínculos reales mucho más que decenas de mensajes cortos.",
        steps = listOf(
          "Invita a un amigo, familiar o ser querido a un café, comida o llamada larga.",
          "Guarda el teléfono en silencio durante todo el encuentro.",
          "Escucha con curiosidad genuina haciendo preguntas abiertas sobre su semana.",
          "Al despedirse, agradece el rato compartido."
        ),
        benefit = "Nutre la cercanía emocional y recuerda que no tienes que caminar solo.",
        durationMinutes = 60
      ),
      WeeklyTemplate(
        title = "Tarde de cocina o paseo compartido",
        summary = "50 min haciendo una actividad cotidiana junto a alguien cercano.",
        description = "Compartir una actividad sencilla crea conversación natural sin necesidad de grandes planes.",
        steps = listOf(
          "Propón preparar algo rico juntos o dar una vuelta por el barrio.",
          "Acuerden hacer el plan sin pantallas ni urgencias de reloj.",
          "Comparte algo sincero sobre cómo te has sentido últimamente.",
          "Disfruten el cierre juntos con tranquilidad."
        ),
        benefit = "Refuerza la confianza mutua a través de experiencias simples y cálidas.",
        durationMinutes = 50
      ),
      WeeklyTemplate(
        title = "Reconectar con alguien que extrañas",
        summary = "45 min entre escribir una carta/audio largo y conversar con calma.",
        description = "Retomar un lazo valioso que se enfrió por la rutina aporta alegría y sentido de pertenencia.",
        steps = listOf(
          "Elige a esa persona que recuerdas con cariño pero hace meses no ves.",
          "Tómate 15 minutos para escribirle un mensaje sincero o proponerle una charla.",
          "Dedica 30 minutos a conversar por voz, videollamada o en persona si coincide.",
          "Acuerden tentativamente cuándo volver a coincidir."
        ),
        benefit = "Recupera redes de afecto reales y combate el aislamiento de la rutina.",
        durationMinutes = 45
      )
    ),
    // 4: Hobbies
    4 to listOf(
      WeeklyTemplate(
        title = "Bloque sagrado de creación sin productividad",
        summary = "60 min para pintar, tocar música, escribir, cocinar o armar algo por placer.",
        description = "Hacer algo únicamente porque te gusta —sin esperar dinero ni aplausos— devuelve ligereza a toda la semana.",
        steps = listOf(
          "Prepara tus materiales sobre la mesa durante 5 minutos.",
          "Apaga alertas y deja claro que esta hora es solo para disfrutar.",
          "Sumérgete 45 minutos en tu hobby sin juzgar si el resultado es perfecto.",
          "Dedica 10 minutos a guardar todo con cariño y contemplar lo creado."
        ),
        benefit = "Despierta la curiosidad y rompe el ciclo de solo trabajar y cumplir obligaciones.",
        durationMinutes = 60
      ),
      WeeklyTemplate(
        title = "Sesión inmersiva de lectura o arte",
        summary = "45 min continuos con un libro, película de autor, álbum o cuaderno.",
        description = "Darle continuidad de casi una hora a una obra permite entrar en estado de calma y disfrute real.",
        steps = listOf(
          "Prepara una bebida que disfrutes y busca tu rincón favorito.",
          "Deja el móvil en otra habitación para evitar interrupciones.",
          "Disfruta 40 minutos continuos de lectura, dibujo o escucha atenta.",
          "Anota en una línea qué idea o emoción te dejó."
        ),
        benefit = "Regenera la atención profunda y alimenta tu mundo interior.",
        durationMinutes = 45
      ),
      WeeklyTemplate(
        title = "Mini proyecto manual de fin de semana",
        summary = "50 min para reparar, plantar, fotografiar o experimentar algo nuevo.",
        description = "Usar las manos en un proyecto tangible genera una satisfacción serena muy distinta al trabajo digital.",
        steps = listOf(
          "Elige un proyecto pequeño que puedas empezar y cerrar en una sesión.",
          "Reúne herramientas o ingredientes antes de iniciar el cronómetro.",
          "Avanza 35 minutos con calma y atención al detalle.",
          "Ordena el espacio y disfruta el resultado tal como quedó."
        ),
        benefit = "Aporta sensación de juego, presencia y logro tangible.",
        durationMinutes = 50
      )
    ),
    // 5: Excelencia
    5 to listOf(
      WeeklyTemplate(
        title = "Bloque de enfoque profundo sin interrupciones",
        summary = "50 min dedicados a tu proyecto más importante con una sola pestaña abierta.",
        description = "Trabajar con serenidad y foco íntegro en lo esencial logra más que horas dispersas entre mensajes.",
        steps = listOf(
          "Define en una frase cuál es el único entregable que avanzarás en esta sesión.",
          "Cierra mensajería, redes y notificaciones por 50 minutos.",
          "Trabaja en bloques serenos manteniendo un ritmo sostenible.",
          "Dedica los últimos 5 minutos a anotar el siguiente paso claro para mañana."
        ),
        benefit = "Combina alto nivel de cuidado profesional con menos estrés mental.",
        durationMinutes = 50
      ),
      WeeklyTemplate(
        title = "Revisión semanal y limpieza de sistema",
        summary = "45 min para ordenar archivos, agenda y prioridades de la próxima semana.",
        description = "Dedicar menos de una hora a poner orden evita ir apagando incendios durante los siguientes días.",
        steps = listOf(
          "Revisa tus pendientes abiertos y archiva o descarta lo que ya no aporta.",
          "Ordena escritorio físico y carpeta de descargas o notas.",
          "Elige máximo 3 prioridades clave para la semana que inicia.",
          "Reserva espacio en tu agenda también para tu descanso."
        ),
        benefit = "Aporta claridad estratégica y sensación de control tranquilo.",
        durationMinutes = 45
      ),
      WeeklyTemplate(
        title = "Práctica deliberada de una habilidad clave",
        summary = "60 min para estudiar y aplicar una mejora real en tu oficio o estudios.",
        description = "Aprender de forma aplicada sobre algo que te importa eleva tu maestría personal con propósito.",
        steps = listOf(
          "Elige una competencia concreta que quieras pulir esta semana.",
          "Estudia un recurso de calidad durante 20 minutos tomando notas breves.",
          "Aplica lo aprendido en un ejercicio o caso real durante 30 minutos.",
          "Resume en 3 puntos qué mejorarás en la próxima práctica."
        ),
        benefit = "Impulsa crecimiento profesional auténtico y orgullo sano por tu trabajo.",
        durationMinutes = 60
      )
    )
  )

  /**
   * Calcula la semana ISO local (Lunes como inicio de semana) compatible con minSdk 24.
   */
  fun currentIsoWeekSeed(dayKey: DayKey = DayKey.DEFAULT): IsoWeekSeed {
    val cal = Calendar.getInstance(Locale.getDefault()).apply {
      timeInMillis = dayKey.currentMillis()
      firstDayOfWeek = Calendar.MONDAY
      minimalDaysInFirstWeek = 4
    }
    val week = cal.get(Calendar.WEEK_OF_YEAR)
    var isoYear = cal.get(Calendar.YEAR)
    val month = cal.get(Calendar.MONTH)
    if (month == Calendar.DECEMBER && week == 1) {
      isoYear += 1
    } else if (month == Calendar.JANUARY && week >= 52) {
      isoYear -= 1
    }
    return IsoWeekSeed(isoYear = isoYear, isoWeek = week)
  }

  /**
   * Devuelve el reto semanal determinista para la dimensión real más baja y la semana ISO actual.
   * Rota automáticamente cada lunes cuando cambia el seed ISO.
   */
  fun getChallengeForWeek(
    realScores: Map<Int, Int>,
    perceivedScores: Map<Int, Int>,
    dayKey: DayKey = DayKey.DEFAULT
  ): BalanceMission {
    val lowestDim = HexagonCalculator.findLowestRealDimension(realScores, perceivedScores)
    val seed = currentIsoWeekSeed(dayKey)
    return buildChallenge(dimIndex = lowestDim, seed = seed)
  }

  fun buildChallenge(dimIndex: Int, seed: IsoWeekSeed): BalanceMission {
    val safeDim = dimIndex.coerceIn(0, 5)
    val templates = templatesByDimension[safeDim] ?: templatesByDimension.getValue(0)
    val templateIndex = (seed.numericSeed and 0x7FFFFFFF) % templates.size
    val template = templates[templateIndex]
    val dimTitle = dimensionTitles[safeDim] ?: "Equilibrio"

    return BalanceMission(
      id = "${ID_PREFIX}${seed.seedKey}_d${safeDim}_t${templateIndex}",
      dimensionIndex = safeDim,
      dimensionTitle = dimTitle,
      title = template.title,
      summary = template.summary,
      description = template.description,
      steps = template.steps,
      benefit = template.benefit,
      durationLabel = "${template.durationMinutes} min",
      durationMinutes = template.durationMinutes,
      isPriorityFocus = true,
      isCompleted = false,
      isSavedForLater = false,
      isCustom = false,
      frequency = "Reto semanal",
      isHabit = false,
      expReward = WEEKLY_CHALLENGE_EXP,
      baseExpReward = WEEKLY_CHALLENGE_EXP,
      isWeaknessFocus = true,
      hasMultiplier = false,
      difficulty = "Reto semanal",
      countsTowardDailyCap = false,
      isWeeklyChallenge = true
    )
  }

  /**
   * Reconstruye un reto semanal guardado a partir de su ID determinista ("weekly_YYYY_wWW_dD_tT")
   * para poder mostrar su título y editar reflexiones en el diario histórico o en Hoy/Plan.
   */
  fun findById(missionId: String): BalanceMission? {
    if (!missionId.startsWith(ID_PREFIX)) return null
    val parts = missionId.removePrefix(ID_PREFIX).split("_")
    // Formato esperado: [YYYY, wWW, dD, tT]
    if (parts.size < 4) return null
    val year = parts[0].toIntOrNull() ?: return null
    val week = parts[1].removePrefix("w").toIntOrNull() ?: return null
    val dim = parts[2].removePrefix("d").toIntOrNull()?.coerceIn(0, 5) ?: return null
    val templateIdx = parts[3].removePrefix("t").toIntOrNull()
    val templates = templatesByDimension[dim] ?: return null
    val safeTemplateIdx = (templateIdx ?: ((year * 53 + week) and 0x7FFFFFFF)).coerceAtLeast(0) % templates.size
    val template = templates[safeTemplateIdx]
    val dimTitle = dimensionTitles[dim] ?: "Equilibrio"

    return BalanceMission(
      id = missionId,
      dimensionIndex = dim,
      dimensionTitle = dimTitle,
      title = template.title,
      summary = template.summary,
      description = template.description,
      steps = template.steps,
      benefit = template.benefit,
      durationLabel = "${template.durationMinutes} min",
      durationMinutes = template.durationMinutes,
      isPriorityFocus = true,
      frequency = "Reto semanal",
      expReward = WEEKLY_CHALLENGE_EXP,
      baseExpReward = WEEKLY_CHALLENGE_EXP,
      isWeaknessFocus = true,
      hasMultiplier = false,
      difficulty = "Reto semanal",
      countsTowardDailyCap = false,
      isWeeklyChallenge = true
    )
  }
}
