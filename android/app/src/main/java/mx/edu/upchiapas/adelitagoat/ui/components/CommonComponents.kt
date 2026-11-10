package mx.edu.upchiapas.adelitagoat.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import mx.edu.upchiapas.adelitagoat.domain.OrderStatus
import mx.edu.upchiapas.adelitagoat.domain.PaymentStatus
import mx.edu.upchiapas.adelitagoat.ui.theme.*

@Composable
fun AdelitaLogoBadge(size: Dp = 72.dp, iconSize: Dp = 38.dp) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(size * 0.28f))
            .background(AdelitaNavy),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.RestaurantMenu,
            contentDescription = "Logotipo AdelitaGOAT",
            tint = Color.White,
            modifier = Modifier.size(iconSize)
        )
    }
}

@Composable
fun AdelitaBrandTitle(fontSizeSp: Int = 22) {
    Text(
        text = buildAnnotatedString {
            withStyle(SpanStyle(color = AdelitaNavy, fontWeight = FontWeight.ExtraBold)) {
                append("Adelita")
            }
            withStyle(SpanStyle(color = AdelitaCoral, fontWeight = FontWeight.ExtraBold)) {
                append("GOAT")
            }
        },
        fontSize = fontSizeSp.sp
    )
}

@Composable
fun CoralPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    leadingIcon: ImageVector? = null
) {
    Button(
        onClick = onClick,
        enabled = enabled && !loading,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = AdelitaCoral,
            contentColor = Color.White,
            disabledContainerColor = AdelitaCoral.copy(alpha = 0.45f),
            disabledContentColor = Color.White
        )
    ) {
        if (loading) {
            CircularProgressIndicator(
                color = Color.White,
                strokeWidth = 2.5.dp,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
        } else if (leadingIcon != null) {
            Icon(
                imageVector = leadingIcon,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
        }
        Text(
            text = text,
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp
        )
    }
}

@Composable
fun GoldRatingChip(rating: Double, count: Int? = null) {
    Surface(
        color = AdelitaGold,
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Star,
                contentDescription = "Calificación",
                tint = AdelitaNavy,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = if (count != null && count > 0) "$rating ($count)" else "$rating",
                color = AdelitaNavy,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp
            )
        }
    }
}

@Composable
fun OrderStatusBadge(status: OrderStatus) {
    val (bg, fg) = when (status) {
        OrderStatus.RECIBIDO -> AdelitaGold to AdelitaNavy
        OrderStatus.EN_PREPARACION -> Color(0xFFE3F2FD) to AdelitaNavy
        OrderStatus.LISTO -> AdelitaSuccessBg to AdelitaSuccessGreen
        OrderStatus.ENTREGADO -> Color(0xFFE8EAF6) to AdelitaNavy
        OrderStatus.CANCELADO -> AdelitaCoralLight to AdelitaCoral
    }
    Surface(
        color = bg,
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(
            text = status.displayTitle(),
            color = fg,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
        )
    }
}

@Composable
fun PaymentStatusChip(status: PaymentStatus) {
    val (bg, fg) = when (status) {
        PaymentStatus.PENDIENTE_EFECTIVO -> AdelitaWarningBg to AdelitaNavy
        PaymentStatus.COBRADO_EN_EFECTIVO -> AdelitaSuccessBg to AdelitaSuccessGreen
        PaymentStatus.CANCELADO -> AdelitaSurfaceGray to AdelitaTextMuted
    }
    Surface(
        color = bg,
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (status == PaymentStatus.COBRADO_EN_EFECTIVO) Icons.Default.CheckCircle else Icons.Default.Payments,
                contentDescription = null,
                tint = fg,
                modifier = Modifier.size(15.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = status.displayLabel(),
                color = fg,
                fontWeight = FontWeight.SemiBold,
                fontSize = 12.sp
            )
        }
    }
}

@Composable
fun ProductVisualAvatar(imageKey: String, size: Dp = 64.dp) {
    val (icon, tint, bg) = when {
        imageKey.contains("horchata") || imageKey.contains("bebida") || imageKey.contains("jugo") ->
            Triple(Icons.Default.LocalDrink, AdelitaNavy, Color(0xFFE8EEF9))
        imageKey.contains("torta") ->
            Triple(Icons.Default.LunchDining, AdelitaCoral, AdelitaCoralLight)
        imageKey.contains("hamburguesa") ->
            Triple(Icons.Default.Fastfood, AdelitaNavy, AdelitaGold.copy(alpha = 0.55f))
        imageKey.contains("quesadilla") ->
            Triple(Icons.Default.LocalPizza, AdelitaGoldDark, AdelitaWarningBg)
        else ->
            Triple(Icons.Default.Restaurant, AdelitaCoral, AdelitaSurfaceGray)
    }
    Box(
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(12.dp))
            .background(bg),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(size * 0.5f)
        )
    }
}

@Composable
fun LoadingStateBox(message: String = "Cargando información...") {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = AdelitaCoral)
            Spacer(modifier = Modifier.height(12.dp))
            Text(text = message, color = AdelitaTextMuted, fontSize = 14.sp)
        }
    }
}

@Composable
fun ErrorStateCard(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(containerColor = AdelitaCoralLight),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.ErrorOutline,
                contentDescription = "Error",
                tint = AdelitaCoral,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = message,
                color = AdelitaTextDark,
                textAlign = TextAlign.Center,
                fontSize = 14.sp
            )
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(containerColor = AdelitaNavy)
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Reintentar")
            }
        }
    }
}

@Composable
fun EmptyStateCard(
    title: String,
    subtitle: String,
    icon: ImageVector = Icons.Default.Storefront,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(containerColor = AdelitaSurfaceGray),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = AdelitaNavy,
                modifier = Modifier.size(42.dp)
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = AdelitaNavy,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = subtitle,
                color = AdelitaTextMuted,
                fontSize = 13.sp,
                textAlign = TextAlign.Center
            )
            if (actionLabel != null && onAction != null) {
                Spacer(modifier = Modifier.height(14.dp))
                OutlinedButton(onClick = onAction) {
                    Text(actionLabel, color = AdelitaNavy, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}
