package com.example.widget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import androidx.core.content.ContextCompat
import com.example.R
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

object EquilibrioWidgetHexagonRenderer {

  private val defaultDimensionColors = intArrayOf(
    0xFF4F46E5.toInt(), // Autoestima
    0xFF0D9488.toInt(), // Ejercicio
    0xFF2563EB.toInt(), // Descanso
    0xFFDB2777.toInt(), // Relaciones
    0xFFD97706.toInt(), // Hobbies
    0xFF7C3AED.toInt()  // Excelencia
  )

  fun renderForContext(
    context: Context,
    realScores: Map<Int, Float>,
    sizePx: Int
  ): Bitmap {
    val gridColor = ContextCompat.getColor(context, R.color.equilibrio_widget_hex_grid)
    val fillColor = ContextCompat.getColor(context, R.color.equilibrio_widget_hex_fill)
    val strokeColor = ContextCompat.getColor(context, R.color.equilibrio_widget_hex_stroke)
    val vertexRingColor = ContextCompat.getColor(context, R.color.equilibrio_widget_bg)
    return renderBitmap(
      sizePx = sizePx,
      realScores = realScores,
      gridColor = gridColor,
      fillColor = fillColor,
      strokeColor = strokeColor,
      vertexRingColor = vertexRingColor
    )
  }

  fun renderBitmap(
    sizePx: Int,
    realScores: Map<Int, Float>,
    gridColor: Int,
    fillColor: Int,
    strokeColor: Int,
    vertexRingColor: Int,
    dimensionColors: IntArray = defaultDimensionColors
  ): Bitmap {
    val safeSize = sizePx.coerceAtLeast(64)
    val bitmap = Bitmap.createBitmap(safeSize, safeSize, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val centerX = safeSize / 2f
    val centerY = safeSize / 2f
    val maxRadius = safeSize * 0.41f

    val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      style = Paint.Style.STROKE
      color = gridColor
      strokeWidth = (safeSize * 0.018f).coerceAtLeast(1.5f)
      strokeJoin = Paint.Join.ROUND
    }

    val innerGridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      style = Paint.Style.STROKE
      color = gridColor
      alpha = 150
      strokeWidth = (safeSize * 0.012f).coerceAtLeast(1f)
      strokeJoin = Paint.Join.ROUND
    }

    // Outer & 50% concentric hexagons + spokes
    drawHexagonRing(canvas, centerX, centerY, maxRadius, gridPaint)
    drawHexagonRing(canvas, centerX, centerY, maxRadius * 0.5f, innerGridPaint)
    for (i in 0..5) {
      val angle = -PI / 2.0 + i * (PI / 3.0)
      val endX = (centerX + maxRadius * cos(angle)).toFloat()
      val endY = (centerY + maxRadius * sin(angle)).toFloat()
      canvas.drawLine(centerX, centerY, endX, endY, innerGridPaint)
    }

    // Real scores polygon (normalized 0..10 with subtle 0.06f minimum floor matching main chart)
    val polygonPath = Path()
    val pointXs = FloatArray(6)
    val pointYs = FloatArray(6)
    for (i in 0..5) {
      val score = (realScores[i] ?: 0f).coerceIn(0f, 10f)
      val ratio = (score / 10f).coerceIn(0.06f, 1f)
      val r = maxRadius * ratio
      val angle = -PI / 2.0 + i * (PI / 3.0)
      val px = (centerX + r * cos(angle)).toFloat()
      val py = (centerY + r * sin(angle)).toFloat()
      pointXs[i] = px
      pointYs[i] = py
      if (i == 0) {
        polygonPath.moveTo(px, py)
      } else {
        polygonPath.lineTo(px, py)
      }
    }
    polygonPath.close()

    val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      style = Paint.Style.FILL
      color = fillColor
    }
    canvas.drawPath(polygonPath, fillPaint)

    val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      style = Paint.Style.STROKE
      color = strokeColor
      strokeWidth = (safeSize * 0.028f).coerceAtLeast(2.2f)
      strokeJoin = Paint.Join.ROUND
      strokeCap = Paint.Cap.ROUND
    }
    canvas.drawPath(polygonPath, strokePaint)

    val hasRealData = realScores.values.any { it > 0.05f }
    val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      style = Paint.Style.FILL
      color = vertexRingColor
    }
    val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      style = Paint.Style.FILL
    }
    val dotRadius = if (hasRealData) (safeSize * 0.038f).coerceAtLeast(3f) else (safeSize * 0.025f).coerceAtLeast(2f)
    val haloRadius = dotRadius * 1.45f

    for (i in 0..5) {
      val color = dimensionColors.getOrElse(i) { strokeColor }
      dotPaint.color = color
      dotPaint.alpha = if (hasRealData) 255 else 135
      canvas.drawCircle(pointXs[i], pointYs[i], haloRadius, ringPaint)
      canvas.drawCircle(pointXs[i], pointYs[i], dotRadius, dotPaint)
    }

    return bitmap
  }

  private fun drawHexagonRing(
    canvas: Canvas,
    centerX: Float,
    centerY: Float,
    radius: Float,
    paint: Paint
  ) {
    val path = Path()
    for (i in 0..5) {
      val angle = -PI / 2.0 + i * (PI / 3.0)
      val x = (centerX + radius * cos(angle)).toFloat()
      val y = (centerY + radius * sin(angle)).toFloat()
      if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    path.close()
    canvas.drawPath(path, paint)
  }
}
