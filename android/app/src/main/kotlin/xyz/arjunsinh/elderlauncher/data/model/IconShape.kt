package xyz.arjunsinh.elderlauncher.data.model

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

enum class IconShape(val label: String) {
    Circle("Circle"),
    RoundedSquare("Rounded Square");

    val shape: Shape
        get() = when (this) {
            Circle -> CircleShape
            RoundedSquare -> RoundedCornerShape(16.dp)
        }
}
