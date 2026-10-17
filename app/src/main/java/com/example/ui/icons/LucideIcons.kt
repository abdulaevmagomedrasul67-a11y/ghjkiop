package com.example.ui.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

object Lucide {

    private inline fun buildIcon(
        name: String,
        block: ImageVector.Builder.() -> Unit
    ): ImageVector = ImageVector.Builder(
        name = name,
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply(block).build()

    private val strokeColor = SolidColor(Color.Black)
    private const val STROKE_WIDTH = 1.5f

    val ArrowLeft: ImageVector = buildIcon("Lucide.ArrowLeft") {
        path(
            stroke = strokeColor,
            strokeLineWidth = STROKE_WIDTH,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        ) {
            moveTo(19f, 12f)
            lineTo(5f, 12f)
            moveTo(12f, 19f)
            lineTo(5f, 12f)
            lineTo(12f, 5f)
        }
    }

    val ArrowRight: ImageVector = buildIcon("Lucide.ArrowRight") {
        path(
            stroke = strokeColor,
            strokeLineWidth = STROKE_WIDTH,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        ) {
            moveTo(5f, 12f)
            lineTo(19f, 12f)
            moveTo(12f, 5f)
            lineTo(19f, 12f)
            lineTo(12f, 19f)
        }
    }

    val ArrowDown: ImageVector = buildIcon("Lucide.ArrowDown") {
        path(
            stroke = strokeColor,
            strokeLineWidth = STROKE_WIDTH,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        ) {
            moveTo(12f, 5f)
            lineTo(12f, 19f)
            moveTo(19f, 12f)
            lineTo(12f, 19f)
            lineTo(5f, 12f)
        }
    }

    val Check: ImageVector = buildIcon("Lucide.Check") {
        path(
            stroke = strokeColor,
            strokeLineWidth = STROKE_WIDTH,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        ) {
            moveTo(20f, 6f)
            lineTo(9f, 17f)
            lineTo(4f, 12f)
        }
    }

    val X: ImageVector = buildIcon("Lucide.X") {
        path(
            stroke = strokeColor,
            strokeLineWidth = STROKE_WIDTH,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        ) {
            moveTo(18f, 6f)
            lineTo(6f, 18f)
            moveTo(6f, 6f)
            lineTo(18f, 18f)
        }
    }

    val Plus: ImageVector = buildIcon("Lucide.Plus") {
        path(
            stroke = strokeColor,
            strokeLineWidth = STROKE_WIDTH,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        ) {
            moveTo(12f, 5f)
            lineTo(12f, 19f)
            moveTo(5f, 12f)
            lineTo(19f, 12f)
        }
    }

    val Settings: ImageVector = buildIcon("Lucide.Settings") {
        path(
            stroke = strokeColor,
            strokeLineWidth = STROKE_WIDTH,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        ) {
            moveTo(12.22f, 2f)
            horizontalLineTo(11.78f)
            lineTo(11.4f, 4.38f)
            lineTo(10.25f, 4.86f)
            lineTo(8.28f, 3.47f)
            lineTo(7.42f, 4.33f)
            lineTo(8.81f, 6.3f)
            lineTo(8.33f, 7.45f)
            lineTo(5.95f, 7.83f)
            verticalLineTo(8.72f)
            lineTo(8.33f, 9.1f)
            lineTo(8.81f, 10.25f)
            lineTo(7.42f, 12.22f)
            lineTo(8.28f, 13.08f)
            lineTo(10.25f, 11.69f)
            lineTo(11.4f, 12.17f)
            lineTo(11.78f, 14.55f)
            horizontalLineTo(12.22f)
            lineTo(12.6f, 12.17f)
            lineTo(13.75f, 11.69f)
            lineTo(15.72f, 13.08f)
            lineTo(16.58f, 12.22f)
            lineTo(15.19f, 10.25f)
            lineTo(15.67f, 9.1f)
            lineTo(18.05f, 8.72f)
            verticalLineTo(7.83f)
            lineTo(15.67f, 7.45f)
            lineTo(15.19f, 6.3f)
            lineTo(16.58f, 4.33f)
            lineTo(15.72f, 3.47f)
            lineTo(13.75f, 4.86f)
            lineTo(12.6f, 4.38f)
            close()
        }
    }

    val FileText: ImageVector = buildIcon("Lucide.FileText") {
        path(
            stroke = strokeColor,
            strokeLineWidth = STROKE_WIDTH,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        ) {
            moveTo(14.5f, 2f)
            horizontalLineTo(6f)
            curveTo(4.9f, 2f, 4f, 2.9f, 4f, 4f)
            verticalLineTo(20f)
            curveTo(4f, 21.1f, 4.9f, 22f, 6f, 22f)
            horizontalLineTo(18f)
            curveTo(19.1f, 22f, 20f, 21.1f, 20f, 20f)
            verticalLineTo(7.5f)
            lineTo(14.5f, 2f)
            close()
            moveTo(14f, 2f)
            verticalLineTo(8f)
            horizontalLineTo(20f)
            moveTo(8f, 13f)
            horizontalLineTo(16f)
            moveTo(8f, 17f)
            horizontalLineTo(14f)
        }
    }

    val Download: ImageVector = buildIcon("Lucide.Download") {
        path(
            stroke = strokeColor,
            strokeLineWidth = STROKE_WIDTH,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        ) {
            moveTo(21f, 15f)
            verticalLineTo(19f)
            curveTo(21f, 20.1f, 20.1f, 21f, 19f, 21f)
            horizontalLineTo(5f)
            curveTo(3.9f, 21f, 3f, 20.1f, 3f, 19f)
            verticalLineTo(15f)
            moveTo(7f, 10f)
            lineTo(12f, 15f)
            lineTo(17f, 10f)
            moveTo(12f, 15f)
            lineTo(12f, 3f)
        }
    }

    val Share: ImageVector = buildIcon("Lucide.Share") {
        path(
            stroke = strokeColor,
            strokeLineWidth = STROKE_WIDTH,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        ) {
            moveTo(4f, 12f)
            verticalLineTo(20f)
            curveTo(4f, 20.6f, 4.4f, 21f, 5f, 21f)
            horizontalLineTo(19f)
            curveTo(19.6f, 21f, 20f, 20.6f, 20f, 20f)
            verticalLineTo(12f)
            moveTo(16f, 6f)
            lineTo(12f, 2f)
            lineTo(8f, 6f)
            moveTo(12f, 2f)
            lineTo(12f, 15f)
        }
    }

    val Eye: ImageVector = buildIcon("Lucide.Eye") {
        path(
            stroke = strokeColor,
            strokeLineWidth = STROKE_WIDTH,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        ) {
            moveTo(2f, 12f)
            curveTo(5.5f, 5f, 18.5f, 5f, 22f, 12f)
            curveTo(18.5f, 19f, 5.5f, 19f, 2f, 12f)
            close()
            moveTo(12f, 15f)
            curveTo(13.66f, 15f, 15f, 13.66f, 15f, 12f)
            curveTo(15f, 10.34f, 13.66f, 9f, 12f, 9f)
            curveTo(10.34f, 9f, 9f, 10.34f, 9f, 12f)
            curveTo(9f, 13.66f, 10.34f, 15f, 12f, 15f)
            close()
        }
    }

    val EyeOff: ImageVector = buildIcon("Lucide.EyeOff") {
        path(
            stroke = strokeColor,
            strokeLineWidth = STROKE_WIDTH,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        ) {
            moveTo(2f, 2f)
            lineTo(22f, 22f)
            moveTo(9.88f, 9.88f)
            curveTo(9.34f, 10.42f, 9f, 11.17f, 9f, 12f)
            curveTo(9f, 13.66f, 10.34f, 15f, 12f, 15f)
            curveTo(12.83f, 15f, 13.58f, 14.66f, 14.12f, 14.12f)
            moveTo(10.73f, 5.08f)
            curveTo(11.15f, 5.03f, 11.57f, 5f, 12f, 5f)
            curveTo(18.5f, 5f, 22f, 12f, 22f, 12f)
            curveTo(20.94f, 14.12f, 19.33f, 15.84f, 17.39f, 17f)
            moveTo(6.61f, 6.61f)
            curveTo(4.62f, 7.82f, 3.03f, 9.71f, 2f, 12f)
            curveTo(5.5f, 19f, 18.5f, 19f, 19.5f, 18.5f)
        }
    }

    val Terminal: ImageVector = buildIcon("Lucide.Terminal") {
        path(
            stroke = strokeColor,
            strokeLineWidth = STROKE_WIDTH,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        ) {
            moveTo(4f, 17f)
            lineTo(10f, 11f)
            lineTo(4f, 5f)
            moveTo(12f, 19f)
            horizontalLineTo(20f)
        }
    }

    val GraduationCap: ImageVector = buildIcon("Lucide.GraduationCap") {
        path(
            stroke = strokeColor,
            strokeLineWidth = STROKE_WIDTH,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        ) {
            moveTo(22f, 10f)
            verticalLineTo(16f)
            moveTo(2f, 10f)
            lineTo(12f, 5f)
            lineTo(22f, 10f)
            lineTo(12f, 15f)
            close()
            moveTo(6f, 12.5f)
            verticalLineTo(17f)
            curveTo(6f, 18.5f, 8.7f, 20f, 12f, 20f)
            curveTo(15.3f, 20f, 18f, 18.5f, 18f, 17f)
            verticalLineTo(12.5f)
        }
    }

    val Globe: ImageVector = buildIcon("Lucide.Globe") {
        path(
            stroke = strokeColor,
            strokeLineWidth = STROKE_WIDTH,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        ) {
            moveTo(12f, 22f)
            curveTo(17.52f, 22f, 22f, 17.52f, 22f, 12f)
            curveTo(22f, 6.48f, 17.52f, 2f, 12f, 2f)
            curveTo(6.48f, 2f, 2f, 6.48f, 2f, 12f)
            curveTo(2f, 17.52f, 6.48f, 22f, 12f, 22f)
            close()
            moveTo(2f, 12f)
            horizontalLineTo(22f)
            moveTo(12f, 2f)
            curveTo(14.5f, 5f, 16f, 8.5f, 16f, 12f)
            curveTo(16f, 15.5f, 14.5f, 19f, 12f, 22f)
            curveTo(9.5f, 19f, 8f, 15.5f, 8f, 12f)
            curveTo(8f, 8.5f, 9.5f, 5f, 12f, 2f)
            close()
        }
    }

    val Lock: ImageVector = buildIcon("Lucide.Lock") {
        path(
            stroke = strokeColor,
            strokeLineWidth = STROKE_WIDTH,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        ) {
            moveTo(5f, 11f)
            horizontalLineTo(19f)
            verticalLineTo(21f)
            horizontalLineTo(5f)
            close()
            moveTo(8f, 11f)
            verticalLineTo(7f)
            curveTo(8f, 4.8f, 9.8f, 3f, 12f, 3f)
            curveTo(14.2f, 3f, 16f, 4.8f, 16f, 7f)
            verticalLineTo(11f)
        }
    }

    val History: ImageVector = buildIcon("Lucide.History") {
        path(
            stroke = strokeColor,
            strokeLineWidth = STROKE_WIDTH,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        ) {
            moveTo(3f, 12f)
            curveTo(3f, 7.03f, 7.03f, 3f, 12f, 3f)
            curveTo(15.7f, 3f, 18.88f, 5.25f, 20.25f, 8.5f)
            moveTo(3f, 3f)
            verticalLineTo(12f)
            horizontalLineTo(12f)
            moveTo(12f, 7f)
            verticalLineTo(12f)
            lineTo(15.5f, 14f)
            moveTo(21f, 12f)
            curveTo(21f, 16.97f, 16.97f, 21f, 12f, 21f)
            curveTo(8.75f, 21f, 5.92f, 19.28f, 4.4f, 16.65f)
        }
    }

    val Trash: ImageVector = buildIcon("Lucide.Trash") {
        path(
            stroke = strokeColor,
            strokeLineWidth = STROKE_WIDTH,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        ) {
            moveTo(3f, 6f)
            horizontalLineTo(21f)
            moveTo(19f, 6f)
            verticalLineTo(20f)
            curveTo(19f, 21f, 18f, 22f, 17f, 22f)
            horizontalLineTo(7f)
            curveTo(6f, 22f, 5f, 21f, 5f, 20f)
            verticalLineTo(6f)
            moveTo(8f, 6f)
            verticalLineTo(4f)
            curveTo(8f, 3f, 9f, 2f, 10f, 2f)
            horizontalLineTo(14f)
            curveTo(15f, 2f, 16f, 3f, 16f, 4f)
            verticalLineTo(6f)
        }
    }

    val Copy: ImageVector = buildIcon("Lucide.Copy") {
        path(
            stroke = strokeColor,
            strokeLineWidth = STROKE_WIDTH,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        ) {
            moveTo(9f, 9f)
            horizontalLineTo(20f)
            verticalLineTo(20f)
            horizontalLineTo(9f)
            close()
            moveTo(5f, 15f)
            horizontalLineTo(4f)
            curveTo(3.45f, 15f, 3f, 14.55f, 3f, 14f)
            verticalLineTo(4f)
            curveTo(3f, 3.45f, 3.45f, 3f, 4f, 3f)
            horizontalLineTo(14f)
            curveTo(14.55f, 3f, 15f, 3.45f, 15f, 4f)
            verticalLineTo(5f)
        }
    }

    val Search: ImageVector = buildIcon("Lucide.Search") {
        path(
            stroke = strokeColor,
            strokeLineWidth = STROKE_WIDTH,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        ) {
            moveTo(11f, 19f)
            curveTo(15.42f, 19f, 19f, 15.42f, 19f, 11f)
            curveTo(19f, 6.58f, 15.42f, 3f, 11f, 3f)
            curveTo(6.58f, 3f, 3f, 6.58f, 3f, 11f)
            curveTo(3f, 15.42f, 6.58f, 19f, 11f, 19f)
            close()
            moveTo(21f, 21f)
            lineTo(16.65f, 16.65f)
        }
    }

    val Refresh: ImageVector = buildIcon("Lucide.Refresh") {
        path(
            stroke = strokeColor,
            strokeLineWidth = STROKE_WIDTH,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        ) {
            moveTo(21.5f, 2f)
            verticalLineTo(7.5f)
            horizontalLineTo(16f)
            moveTo(21.34f, 15.57f)
            curveTo(20.45f, 18.72f, 17.7f, 21f, 14.39f, 21f)
            curveTo(10.15f, 21f, 6.72f, 17.58f, 6.72f, 13.34f)
            curveTo(6.72f, 9.87f, 9.03f, 6.94f, 12.2f, 6.04f)
            lineTo(21.5f, 7.5f)
        }
    }

    val MoreHorizontal: ImageVector = buildIcon("Lucide.MoreHorizontal") {
        path(
            stroke = strokeColor,
            strokeLineWidth = STROKE_WIDTH,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        ) {
            moveTo(12f, 12f)
            lineTo(12.01f, 12f)
            moveTo(19f, 12f)
            lineTo(19.01f, 12f)
            moveTo(5f, 12f)
            lineTo(5.01f, 12f)
        }
    }

    val Sparkles: ImageVector = buildIcon("Lucide.Sparkles") {
        path(
            stroke = strokeColor,
            strokeLineWidth = STROKE_WIDTH,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        ) {
            moveTo(12f, 3f)
            lineTo(14f, 9f)
            lineTo(20f, 11f)
            lineTo(14f, 13f)
            lineTo(12f, 19f)
            lineTo(10f, 13f)
            lineTo(4f, 11f)
            lineTo(10f, 9f)
            close()
        }
    }

    val AlertCircle: ImageVector = buildIcon("Lucide.AlertCircle") {
        path(
            stroke = strokeColor,
            strokeLineWidth = STROKE_WIDTH,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        ) {
            moveTo(12f, 22f)
            curveTo(17.52f, 22f, 22f, 17.52f, 22f, 12f)
            curveTo(22f, 6.48f, 17.52f, 2f, 12f, 2f)
            curveTo(6.48f, 2f, 2f, 6.48f, 2f, 12f)
            curveTo(2f, 17.52f, 6.48f, 22f, 12f, 22f)
            close()
            moveTo(12f, 8f)
            verticalLineTo(12f)
            moveTo(12f, 16f)
            horizontalLineTo(12.01f)
        }
    }

    val CheckCircle: ImageVector = buildIcon("Lucide.CheckCircle") {
        path(
            stroke = strokeColor,
            strokeLineWidth = STROKE_WIDTH,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        ) {
            moveTo(22f, 11.08f)
            verticalLineTo(12f)
            curveTo(22f, 17.52f, 17.52f, 22f, 12f, 22f)
            curveTo(6.48f, 22f, 2f, 17.52f, 2f, 12f)
            curveTo(2f, 6.48f, 6.48f, 2f, 12f, 2f)
            curveTo(14.84f, 2f, 17.41f, 3.19f, 19.22f, 5.1f)
            moveTo(22f, 4f)
            lineTo(12f, 14.01f)
            lineTo(9f, 11.01f)
        }
    }

    val BookOpen: ImageVector = buildIcon("Lucide.BookOpen") {
        path(
            stroke = strokeColor,
            strokeLineWidth = STROKE_WIDTH,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        ) {
            moveTo(2f, 3f)
            horizontalLineTo(8f)
            curveTo(10.2f, 3f, 12f, 4.8f, 12f, 7f)
            verticalLineTo(21f)
            curveTo(12f, 19.3f, 10.2f, 18f, 8f, 18f)
            horizontalLineTo(2f)
            close()
            moveTo(22f, 3f)
            horizontalLineTo(16f)
            curveTo(13.8f, 3f, 12f, 4.8f, 12f, 7f)
            verticalLineTo(21f)
            curveTo(12f, 19.3f, 13.8f, 18f, 16f, 18f)
            horizontalLineTo(22f)
            close()
        }
    }

    val User: ImageVector = buildIcon("Lucide.User") {
        path(
            stroke = strokeColor,
            strokeLineWidth = STROKE_WIDTH,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        ) {
            moveTo(19f, 21f)
            verticalLineTo(19f)
            curveTo(19f, 16.79f, 17.21f, 15f, 15f, 15f)
            horizontalLineTo(9f)
            curveTo(6.79f, 15f, 5f, 16.79f, 5f, 19f)
            verticalLineTo(21f)
            moveTo(12f, 11f)
            curveTo(14.21f, 11f, 16f, 9.21f, 16f, 7f)
            curveTo(16f, 4.79f, 14.21f, 3f, 12f, 3f)
            curveTo(9.79f, 3f, 8f, 4.79f, 8f, 7f)
            curveTo(8f, 9.21f, 9.79f, 11f, 12f, 11f)
            close()
        }
    }
}
