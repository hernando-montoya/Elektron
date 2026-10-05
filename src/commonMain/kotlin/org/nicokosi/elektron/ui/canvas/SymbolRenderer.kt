package org.nicokosi.elektron.ui.canvas

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import org.nicokosi.elektron.domain.model.SymbolElement
import org.nicokosi.elektron.domain.model.SymbolType

// Colores de componentes eléctricos
private val SymbolStrokeColor = Color(0xFFECEFF1)
private val SymbolFillColor = Color(0xFF1E2333)
private val TerminalColor = Color(0xFF29B6F6) // Cyan para bornas
private val TerminalBorderColor = Color(0xFF0288D1)
private val AccentColor = Color(0xFFFFB74D)

object SymbolRenderer {

    fun drawSymbol(
        drawScope: DrawScope,
        symbol: SymbolElement,
        isSelected: Boolean = false,
        isPreview: Boolean = false,
        isWhiteBackground: Boolean = false
    ) {
        val baseStroke = if (isWhiteBackground) Color(0xFF0F172A) else SymbolStrokeColor
        val fillColor = if (isWhiteBackground) Color(0xFFFFFFFF) else SymbolFillColor
        val strokeColor = when {
            isSelected -> AccentColor
            isPreview -> baseStroke.copy(alpha = 0.6f)
            else -> baseStroke
        }
        val strokeWidth = if (isSelected) 2.5f else 1.8f

        drawScope.withTransform({
            translate(symbol.position.x, symbol.position.y)
            rotate(degrees = symbol.rotationDegrees.toFloat(), pivot = Offset.Zero)
        }) {
            val halfW = symbol.width / 2f
            val halfH = symbol.height / 2f

            when (symbol.type) {
                // ==========================================
                // SCHÉMA DÉVELOPPÉ (IMAGEN 1)
                // ==========================================
                SymbolType.BUS_LINE_PH -> drawBusLine(Color(0xFFE53935), "PH", halfW, halfH)
                SymbolType.BUS_LINE_N -> drawBusLine(Color(0xFF1E88E5), "N", halfW, halfH)
                SymbolType.BUS_LINE_PE -> drawBusLine(Color(0xFF43A047), "PE", halfW, halfH)
                SymbolType.POLE_DISJONCTEUR_PH -> drawPoleDisjoncteurPh(strokeColor, strokeWidth, halfW, halfH)
                SymbolType.POLE_DISJONCTEUR_N -> drawPoleDisjoncteurN(strokeColor, strokeWidth, halfW, halfH)
                SymbolType.CONTACT_INTERRUPTEUR -> drawContactInterrupteur(strokeColor, strokeWidth, halfW, halfH)
                SymbolType.LAMPE_AVEC_TERRE -> drawLampeAvecTerre(strokeColor, fillColor, strokeWidth, halfW, halfH)

                // ==========================================
                // SCHÉMA MULTIFILAIRE (IMAGEN 2)
                // ==========================================
                SymbolType.DISJONCTEUR_MULTI -> drawDisjoncteurMulti(strokeColor, fillColor, strokeWidth, halfW, halfH)
                SymbolType.DIFFERENTIEL_MULTI -> drawDifferentielMulti(strokeColor, fillColor, strokeWidth, halfW, halfH)
                SymbolType.INTERRUPTEUR_MURAL -> drawInterrupteurMural(strokeColor, strokeWidth, halfW, halfH)
                SymbolType.LAMPE_DCL_MULTI -> drawLampeDclMulti(strokeColor, fillColor, strokeWidth, halfW, halfH)
                SymbolType.BORNE_WAGO -> drawBorneWago(strokeColor, if (isWhiteBackground) Color(0xFFE2E8F0) else Color(0xFF263238), strokeWidth, halfW, halfH)
                SymbolType.BARRETTE_TERRE -> drawBarretteTerre(strokeColor, strokeWidth, halfW, halfH)

                // Símbolos estándar
                SymbolType.CIRCUIT_BREAKER -> drawCircuitBreaker(strokeColor, fillColor, strokeWidth, halfW, halfH)
                SymbolType.DIFFERENTIAL -> drawDifferential(strokeColor, fillColor, strokeWidth, halfW, halfH)
                SymbolType.POWER_OUTLET -> drawPowerOutlet(strokeColor, fillColor, strokeWidth, halfW, halfH)
                SymbolType.LIGHT_POINT -> drawLightPoint(strokeColor, fillColor, strokeWidth, halfW, halfH)
                SymbolType.SWITCH_SIMPLE -> drawSwitchSimple(strokeColor, fillColor, strokeWidth, halfW, halfH)
            }
        }

        // Renderizado de las bornas de conexión en coordenadas absolutas
        val termColor = if (isWhiteBackground) Color(0xFF0288D1) else TerminalColor
        val termBorder = if (isWhiteBackground) Color(0xFF01579B) else TerminalBorderColor
        for ((_, termPos) in symbol.getAbsoluteTerminals()) {
            drawScope.drawCircle(
                color = termColor,
                radius = 3.5f,
                center = termPos
            )
            drawScope.drawCircle(
                color = termBorder,
                radius = 3.5f,
                center = termPos,
                style = Stroke(width = 1.0f)
            )
        }
    }

    // ========================================================
    // RENDERERS: SCHÉMA DÉVELOPPÉ
    // ========================================================

    private fun DrawScope.drawBusLine(color: Color, name: String, halfW: Float, halfH: Float) {
        // Barra colectora vertical robusta
        drawLine(
            color = color,
            start = Offset(0f, -halfH),
            end = Offset(0f, halfH),
            strokeWidth = 4.0f
        )
        // Punto central de toma
        drawCircle(color = color, radius = 4f, center = Offset.Zero)
    }

    private fun DrawScope.drawPoleDisjoncteurPh(color: Color, strokeWidth: Float, halfW: Float, halfH: Float) {
        // Terminal de entrada izquierda
        drawLine(color, Offset(-halfW, 0f), Offset(-12f, 0f), strokeWidth = strokeWidth)

        // Contacto móvil con cruz de poder de corte
        drawLine(color, Offset(-12f, 0f), Offset(0f, -8f), strokeWidth = strokeWidth)
        // Cruz en el contacto
        drawLine(color, Offset(-8f, -7f), Offset(-4f, -1f), strokeWidth = 1.2f)
        drawLine(color, Offset(-4f, -7f), Offset(-8f, -1f), strokeWidth = 1.2f)

        // Disparador térmico y magnético (cresta en arco)
        val path = Path().apply {
            moveTo(0f, 0f)
            lineTo(6f, 0f)
            cubicTo(6f, -7f, 12f, -7f, 12f, 0f)
            lineTo(halfW, 0f)
        }
        drawPath(path, color = color, style = Stroke(width = strokeWidth))
    }

    private fun DrawScope.drawPoleDisjoncteurN(color: Color, strokeWidth: Float, halfW: Float, halfH: Float) {
        // Polo de seccionamiento de neutro
        drawLine(color, Offset(-halfW, 0f), Offset(-10f, 0f), strokeWidth = strokeWidth)
        drawLine(color, Offset(-10f, 0f), Offset(4f, -8f), strokeWidth = strokeWidth)
        // Cruz de seccionamiento
        drawLine(color, Offset(-6f, -7f), Offset(-2f, -1f), strokeWidth = 1.2f)
        drawLine(color, Offset(-2f, -7f), Offset(-6f, -1f), strokeWidth = 1.2f)
        drawLine(color, Offset(4f, 0f), Offset(halfW, 0f), strokeWidth = strokeWidth)
    }

    private fun DrawScope.drawContactInterrupteur(color: Color, strokeWidth: Float, halfW: Float, halfH: Float) {
        // Interruptor simple abierto en línea horizontal
        drawLine(color, Offset(-halfW, 0f), Offset(-8f, 0f), strokeWidth = strokeWidth)
        // Palanca basculante
        drawLine(color, Offset(-8f, 0f), Offset(8f, -10f), strokeWidth = strokeWidth)
        // Borne receptor
        drawCircle(color, radius = 2.0f, center = Offset(8f, 0f))
        drawLine(color, Offset(8f, 0f), Offset(halfW, 0f), strokeWidth = strokeWidth)
    }

    private fun DrawScope.drawLampeAvecTerre(color: Color, fillColor: Color, strokeWidth: Float, halfW: Float, halfH: Float) {
        val radius = 12f
        // Lámpara (círculo con cruz)
        drawCircle(color = fillColor, radius = radius, center = Offset.Zero)
        drawCircle(color = color, radius = radius, center = Offset.Zero, style = Stroke(width = strokeWidth))

        val d = radius * 0.707f
        drawLine(color, Offset(-d, -d), Offset(d, d), strokeWidth = strokeWidth)
        drawLine(color, Offset(-d, d), Offset(d, -d), strokeWidth = strokeWidth)

        // Bornes laterales (L y N)
        drawLine(color, Offset(-halfW, 0f), Offset(-radius, 0f), strokeWidth = strokeWidth)
        drawLine(color, Offset(radius, 0f), Offset(halfW, 0f), strokeWidth = strokeWidth)

        // Símbolo de conexión a tierra PE en la parte inferior (arco de masa)
        drawLine(color, Offset(0f, radius), Offset(0f, halfH - 4f), strokeWidth = 1.5f)
        drawArc(
            color = color,
            startAngle = 0f,
            sweepAngle = 180f,
            useCenter = false,
            topLeft = Offset(-6f, halfH - 8f),
            size = Size(12f, 8f),
            style = Stroke(width = 1.5f)
        )
    }

    // ========================================================
    // RENDERERS: SCHÉMA MULTIFILAIRE (IMAGEN 2)
    // ========================================================

    private fun DrawScope.drawDisjoncteurMulti(color: Color, fillColor: Color, strokeWidth: Float, halfW: Float, halfH: Float) {
        // Envolvente modular rectangular con esquinas redondeadas
        drawRect(color = fillColor, topLeft = Offset(-halfW, -halfH), size = Size(halfW * 2, halfH * 2))
        drawRect(color = color, topLeft = Offset(-halfW, -halfH), size = Size(halfW * 2, halfH * 2), style = Stroke(width = strokeWidth))

        // 4 Bornas de tornillo circulares características (2 arriba, 2 abajo)
        val bornesRadius = 3f
        val leftX = -12f
        val rightX = 12f

        // Bornas superiores
        drawCircle(color, bornesRadius, Offset(leftX, -halfH + 6f), style = Stroke(1.2f))
        drawCircle(color, bornesRadius, Offset(rightX, -halfH + 6f), style = Stroke(1.2f))

        // Bornas inferiores
        drawCircle(color, bornesRadius, Offset(leftX, halfH - 6f), style = Stroke(1.2f))
        drawCircle(color, bornesRadius, Offset(rightX, halfH - 6f), style = Stroke(1.2f))

        // Contactos bipolares acoplados mecánicamente
        drawLine(color, Offset(leftX, -halfH + 9f), Offset(leftX, -4f), strokeWidth = 1.2f)
        drawLine(color, Offset(leftX, -4f), Offset(leftX - 4f, 4f), strokeWidth = 1.2f)
        drawLine(color, Offset(leftX, 4f), Offset(leftX, halfH - 9f), strokeWidth = 1.2f)

        drawLine(color, Offset(rightX, -halfH + 9f), Offset(rightX, -4f), strokeWidth = 1.2f)
        drawLine(color, Offset(rightX, -4f), Offset(rightX - 4f, 4f), strokeWidth = 1.2f)

        // Disparador térmico/magnético en el polo derecho
        val p = Path().apply {
            moveTo(rightX, 4f)
            lineTo(rightX + 3f, 6f)
            lineTo(rightX - 3f, 8f)
            lineTo(rightX, 10f)
            lineTo(rightX, halfH - 9f)
        }
        drawPath(p, color = color, style = Stroke(width = 1.2f))

        // Línea punteada de enlace mecánico bipolar
        drawLine(
            color = color.copy(alpha = 0.6f),
            start = Offset(leftX - 2f, 0f),
            end = Offset(rightX - 2f, 0f),
            strokeWidth = 1.0f
        )
    }

    private fun DrawScope.drawDifferentielMulti(color: Color, fillColor: Color, strokeWidth: Float, halfW: Float, halfH: Float) {
        // Envolvente
        drawRect(color = fillColor, topLeft = Offset(-halfW, -halfH), size = Size(halfW * 2, halfH * 2))
        drawRect(color = color, topLeft = Offset(-halfW, -halfH), size = Size(halfW * 2, halfH * 2), style = Stroke(width = strokeWidth))

        val leftX = -12f
        val rightX = 12f
        val bornesRadius = 3f

        // 4 Bornas
        drawCircle(color, bornesRadius, Offset(leftX, -halfH + 6f), style = Stroke(1.2f))
        drawCircle(color, bornesRadius, Offset(rightX, -halfH + 6f), style = Stroke(1.2f))
        drawCircle(color, bornesRadius, Offset(leftX, halfH - 6f), style = Stroke(1.2f))
        drawCircle(color, bornesRadius, Offset(rightX, halfH - 6f), style = Stroke(1.2f))

        // Paso por los contactos
        drawLine(color, Offset(leftX, -halfH + 9f), Offset(leftX, -5f), strokeWidth = 1.2f)
        drawLine(color, Offset(leftX, -5f), Offset(leftX - 4f, 3f), strokeWidth = 1.2f)
        drawLine(color, Offset(leftX, 3f), Offset(leftX, halfH - 9f), strokeWidth = 1.2f)

        drawLine(color, Offset(rightX, -halfH + 9f), Offset(rightX, -5f), strokeWidth = 1.2f)
        drawLine(color, Offset(rightX, -5f), Offset(rightX - 4f, 3f), strokeWidth = 1.2f)
        drawLine(color, Offset(rightX, 3f), Offset(rightX, halfH - 9f), strokeWidth = 1.2f)

        // Toroide diferencial
        drawOval(
            color = color,
            topLeft = Offset(-16f, -3f),
            size = Size(32f, 8f),
            style = Stroke(width = 1.5f)
        )
    }

    private fun DrawScope.drawInterrupteurMural(color: Color, strokeWidth: Float, halfW: Float, halfH: Float) {
        // Silueta característica de caja de mecanismo de empotrar (perfil en sombrerete / T invertida)
        val path = Path().apply {
            moveTo(-halfW * 0.7f, -halfH)
            lineTo(-halfW * 0.7f, -halfH * 0.4f)
            lineTo(-halfW, -halfH * 0.4f)
            lineTo(-halfW, halfH * 0.4f)
            lineTo(-halfW * 0.7f, halfH * 0.4f)
            lineTo(-halfW * 0.7f, halfH)
            lineTo(halfW * 0.7f, halfH)
            lineTo(halfW * 0.7f, halfH * 0.4f)
            lineTo(halfW, halfH * 0.4f)
            lineTo(halfW, -halfH * 0.4f)
            lineTo(halfW * 0.7f, -halfH * 0.4f)
            lineTo(halfW * 0.7f, -halfH)
        }
        drawPath(path, color = color, style = Stroke(width = strokeWidth))

        // Contacto basculante central
        drawLine(color, Offset(-8f, 6f), Offset(8f, -4f), strokeWidth = strokeWidth)
        drawCircle(color, radius = 2f, center = Offset(-8f, 6f))
    }

    private fun DrawScope.drawLampeDclMulti(color: Color, fillColor: Color, strokeWidth: Float, halfW: Float, halfH: Float) {
        // Envolvente cruciforme de caja DCL
        val path = Path().apply {
            moveTo(-halfW * 0.4f, -halfH)
            lineTo(halfW * 0.4f, -halfH)
            lineTo(halfW * 0.4f, -halfH * 0.4f)
            lineTo(halfW, -halfH * 0.4f)
            lineTo(halfW, halfH * 0.4f)
            lineTo(halfW * 0.4f, halfH * 0.4f)
            lineTo(halfW * 0.4f, halfH)
            lineTo(-halfW * 0.4f, halfH)
            lineTo(-halfW * 0.4f, halfH * 0.4f)
            lineTo(-halfW, halfH * 0.4f)
            lineTo(-halfW, -halfH * 0.4f)
            lineTo(-halfW * 0.4f, -halfH * 0.4f)
            close()
        }
        drawPath(path, color = color, style = Stroke(width = strokeWidth))

        // Símbolo de lámpara en el centro
        val r = 10f
        drawCircle(color = fillColor, radius = r, center = Offset.Zero)
        drawCircle(color = color, radius = r, center = Offset.Zero, style = Stroke(width = 1.5f))
        val d = r * 0.707f
        drawLine(color, Offset(-d, -d), Offset(d, d), strokeWidth = 1.5f)
        drawLine(color, Offset(-d, d), Offset(d, -d), strokeWidth = 1.5f)
    }

    private fun DrawScope.drawBorneWago(color: Color, bodyColor: Color, strokeWidth: Float, halfW: Float, halfH: Float) {
        // Borna rápida de empalme Wago (cuerpo compacto con 3 alvéolos)
        drawRect(
            color = bodyColor,
            topLeft = Offset(-8f, -halfH * 0.8f),
            size = Size(16f, halfH * 1.6f)
        )
        drawRect(
            color = color,
            topLeft = Offset(-8f, -halfH * 0.8f),
            size = Size(16f, halfH * 1.6f),
            style = Stroke(width = 1.2f)
        )
        drawCircle(color = Color(0xFF00E5FF), radius = 2.5f, center = Offset(0f, -halfH * 0.5f))
        drawCircle(color = Color(0xFF00E5FF), radius = 2.5f, center = Offset(0f, 0f))
        drawCircle(color = Color(0xFF00E5FF), radius = 2.5f, center = Offset(0f, halfH * 0.5f))
    }

    private fun DrawScope.drawBarretteTerre(color: Color, strokeWidth: Float, halfW: Float, halfH: Float) {
        // Borna superior e hilo hacia el símbolo de tierra
        drawCircle(color = Color(0xFF43A047), radius = 3.5f, center = Offset(0f, -halfH * 0.5f), style = Stroke(1.5f))
        drawLine(Color(0xFF43A047), Offset(0f, -halfH * 0.5f + 3.5f), Offset(0f, 2f), strokeWidth = 2.0f)

        // Trazo de tierra reglamentario (3 líneas horizontales decrecientes)
        drawLine(Color(0xFF43A047), Offset(-12f, 2f), Offset(12f, 2f), strokeWidth = 2.5f)
        drawLine(Color(0xFF43A047), Offset(-8f, 6f), Offset(8f, 6f), strokeWidth = 2.0f)
        drawLine(Color(0xFF43A047), Offset(-4f, 10f), Offset(4f, 10f), strokeWidth = 1.5f)
    }

    // ========================================================
    // SÍMBOLOS ESTÁNDAR PREVIOS
    // ========================================================

    private fun DrawScope.drawCircuitBreaker(color: Color, fillColor: Color, strokeWidth: Float, halfW: Float, halfH: Float) {
        drawRect(color = fillColor, topLeft = Offset(-halfW, -halfH), size = Size(halfW * 2, halfH * 2))
        drawRect(color = color, topLeft = Offset(-halfW, -halfH), size = Size(halfW * 2, halfH * 2), style = Stroke(width = strokeWidth))
        drawLine(color, Offset(0f, -halfH), Offset(0f, -8f), strokeWidth = strokeWidth)
        drawLine(color, Offset(0f, 8f), Offset(0f, halfH), strokeWidth = strokeWidth)
        drawLine(color, Offset(0f, 8f), Offset(-8f, -6f), strokeWidth = strokeWidth)
        drawRect(color = color, topLeft = Offset(-4f, -4f), size = Size(8f, 8f), style = Stroke(width = 1.2f))
    }

    private fun DrawScope.drawDifferential(color: Color, fillColor: Color, strokeWidth: Float, halfW: Float, halfH: Float) {
        drawRect(color = fillColor, topLeft = Offset(-halfW, -halfH), size = Size(halfW * 2, halfH * 2))
        drawRect(color = color, topLeft = Offset(-halfW, -halfH), size = Size(halfW * 2, halfH * 2), style = Stroke(width = strokeWidth))
        drawLine(color, Offset(0f, -halfH), Offset(0f, -8f), strokeWidth = strokeWidth)
        drawLine(color, Offset(0f, 8f), Offset(0f, halfH), strokeWidth = strokeWidth)
        drawLine(color, Offset(0f, 8f), Offset(-8f, -6f), strokeWidth = strokeWidth)
        drawOval(color = color, topLeft = Offset(-12f, -6f), size = Size(24f, 12f), style = Stroke(width = 1.5f))
    }

    private fun DrawScope.drawPowerOutlet(color: Color, fillColor: Color, strokeWidth: Float, halfW: Float, halfH: Float) {
        val radius = halfW * 0.85f
        drawArc(color = fillColor, startAngle = 180f, sweepAngle = 180f, useCenter = true, topLeft = Offset(-radius, -radius + 4f), size = Size(radius * 2, radius * 2))
        drawArc(color = color, startAngle = 180f, sweepAngle = 180f, useCenter = false, topLeft = Offset(-radius, -radius + 4f), size = Size(radius * 2, radius * 2), style = Stroke(width = strokeWidth))
        drawLine(color = color, start = Offset(-radius, 4f), end = Offset(radius, 4f), strokeWidth = strokeWidth)
        drawLine(color = color, start = Offset(0f, 4f), end = Offset(0f, -halfH + 4f), strokeWidth = strokeWidth)
        drawCircle(color, radius = 2f, center = Offset(-6f, -2f))
        drawCircle(color, radius = 2f, center = Offset(6f, -2f))
    }

    private fun DrawScope.drawLightPoint(color: Color, fillColor: Color, strokeWidth: Float, halfW: Float, halfH: Float) {
        val radius = halfW * 0.75f
        drawCircle(color = fillColor, radius = radius, center = Offset.Zero)
        drawCircle(color = color, radius = radius, center = Offset.Zero, style = Stroke(width = strokeWidth))
        val d = radius * 0.707f
        drawLine(color, Offset(-d, -d), Offset(d, d), strokeWidth = strokeWidth)
        drawLine(color, Offset(-d, d), Offset(d, -d), strokeWidth = strokeWidth)
    }

    private fun DrawScope.drawSwitchSimple(color: Color, fillColor: Color, strokeWidth: Float, halfW: Float, halfH: Float) {
        val radius = halfW * 0.5f
        drawCircle(color = fillColor, radius = radius, center = Offset.Zero)
        drawCircle(color = color, radius = radius, center = Offset.Zero, style = Stroke(width = strokeWidth))
        drawLine(color, Offset(0f, 0f), Offset(halfW * 0.8f, -halfH * 0.8f), strokeWidth = strokeWidth)
        drawLine(color, Offset(halfW * 0.8f, -halfH * 0.8f), Offset(halfW * 0.8f, -halfH * 0.4f), strokeWidth = strokeWidth)
    }
}
