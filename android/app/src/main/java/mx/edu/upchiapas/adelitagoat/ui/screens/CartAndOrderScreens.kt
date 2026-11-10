package mx.edu.upchiapas.adelitagoat.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import mx.edu.upchiapas.adelitagoat.domain.*
import mx.edu.upchiapas.adelitagoat.ui.components.*
import mx.edu.upchiapas.adelitagoat.ui.theme.*
import mx.edu.upchiapas.adelitagoat.ui.viewmodel.AuthViewModel
import mx.edu.upchiapas.adelitagoat.ui.viewmodel.CustomerViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CartAndCheckoutScreen(
    customerViewModel: CustomerViewModel,
    onBack: () -> Unit,
    onExploreStalls: () -> Unit,
    onOrderConfirmed: (Long) -> Unit
) {
    val cart by customerViewModel.cart.collectAsState()
    val isSubmitting by customerViewModel.isSubmittingOrder.collectAsState()
    val checkoutError by customerViewModel.checkoutError.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Método de Pago y Apartado",
                            fontWeight = FontWeight.Bold,
                            color = AdelitaNavy,
                            fontSize = 18.sp
                        )
                        if (cart.stallName.isNotBlank()) {
                            Text(
                                text = cart.stallName,
                                fontSize = 12.sp,
                                color = AdelitaTextMuted
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver",
                            tint = AdelitaNavy
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        }
    ) { padding ->
        if (cart.isEmpty) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White)
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                EmptyStateCard(
                    title = "Tu apartado está vacío",
                    subtitle = "Explora los puestos frente a la UP Chiapas y agrega tus alimentos favoritos.",
                    icon = Icons.Default.ShoppingBag,
                    actionLabel = "Explorar puestos",
                    onAction = onExploreStalls
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White)
                    .padding(padding),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (checkoutError != null) {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = AdelitaCoralLight),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = AdelitaCoral)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = checkoutError ?: "",
                                    color = AdelitaTextDark,
                                    fontSize = 13.sp,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }

                item {
                    Text(
                        text = "Productos en tu apartado",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = AdelitaNavy
                    )
                }

                items(cart.items, key = { it.product.id }) { item ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            ProductVisualAvatar(imageKey = item.product.imageKey, size = 50.dp)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.product.name,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = AdelitaTextDark
                                )
                                Text(
                                    text = "${item.product.formattedPrice} c/u",
                                    fontSize = 12.sp,
                                    color = AdelitaTextMuted
                                )
                                Text(
                                    text = item.formattedLineTotal,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 14.sp,
                                    color = AdelitaNavy
                                )
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                IconButton(
                                    onClick = {
                                        customerViewModel.updateCartQuantity(item.product.id, item.quantity - 1)
                                    },
                                    modifier = Modifier
                                        .size(30.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(AdelitaSurfaceGray)
                                ) {
                                    Icon(
                                        imageVector = if (item.quantity == 1) Icons.Default.DeleteOutline else Icons.Default.Remove,
                                        contentDescription = "Quitar uno",
                                        tint = if (item.quantity == 1) AdelitaCoral else AdelitaNavy,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }

                                Text(
                                    text = item.quantity.toString(),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = AdelitaNavy
                                )

                                IconButton(
                                    onClick = {
                                        customerViewModel.updateCartQuantity(item.product.id, item.quantity + 1)
                                    },
                                    modifier = Modifier
                                        .size(30.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(AdelitaCoral)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = "Agregar uno",
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Notas opcionales para el vendedor
                item {
                    OutlinedTextField(
                        value = cart.notes,
                        onValueChange = { customerViewModel.updateCartNotes(it) },
                        label = { Text("Notas para el puesto (opcional)") },
                        placeholder = { Text("Ej. Salsa verde aparte, sin cebolla...") },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Tarjeta de Método de Pago estilo Figma Pantalla 5 ("Efectivo al recoger")
                item {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Método de Pago",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = AdelitaNavy
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(2.dp, AdelitaNavy, RoundedCornerShape(14.dp)),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(AdelitaNavy),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Payments,
                                    contentDescription = null,
                                    tint = Color.White
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Efectivo al recoger",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = AdelitaNavy
                                )
                                Text(
                                    text = "Paga en efectivo al recoger tu comanda en el mostrador",
                                    fontSize = 12.sp,
                                    color = AdelitaTextMuted
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Seleccionado",
                                tint = AdelitaNavy
                            )
                        }
                    }
                }

                // Resumen Total a pagar estilo Figma Pantalla 5
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = AdelitaSurfaceGray),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Subtotal:",
                                    color = AdelitaTextMuted,
                                    fontSize = 13.sp
                                )
                                Text(
                                    text = formatMxnCents(cart.subtotalCents),
                                    color = AdelitaTextDark,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 13.sp
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Total a pagar:",
                                    color = AdelitaTextDark,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                                Text(
                                    text = cart.formattedTotal,
                                    color = AdelitaNavy,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 20.sp
                                )
                            }
                            HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))
                            Text(
                                text = "Recoge tu pedido en mostrador presentando tu folio único.",
                                fontSize = 12.sp,
                                color = AdelitaTextMuted
                            )
                        }
                    }
                }

                item {
                    CoralPrimaryButton(
                        text = "Confirmar apartado (${cart.formattedTotal})",
                        loading = isSubmitting,
                        leadingIcon = Icons.Default.CheckCircleOutline,
                        onClick = {
                            customerViewModel.confirmOrder { created ->
                                onOrderConfirmed(created.id)
                            }
                        }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = null,
                            tint = AdelitaTextMuted,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "El puesto reservará tus insumos por 20 minutos",
                            fontSize = 11.sp,
                            color = AdelitaTextMuted
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun OrderTicketScreen(
    orderId: Long,
    customerViewModel: CustomerViewModel,
    onViewOrderStatus: (Long) -> Unit,
    onBackHome: () -> Unit
) {
    val selectedOrder by customerViewModel.selectedOrder.collectAsState()
    val lastConfirmed by customerViewModel.lastConfirmedOrder.collectAsState()
    val isLoading by customerViewModel.isLoadingOrderDetail.collectAsState()

    LaunchedEffect(orderId) {
        if (selectedOrder?.id != orderId) {
            customerViewModel.loadOrderDetail(orderId)
        }
    }

    val order = selectedOrder?.takeIf { it.id == orderId } ?: lastConfirmed?.takeIf { it.id == orderId }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color.White
    ) {
        if (order == null && isLoading) {
            LoadingStateBox("Generando comprobante oficial...")
        } else if (order != null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 22.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "AdelitaGOAT",
                    color = AdelitaNavy,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Text(
                    text = "📍 ${order.campusLocation}",
                    color = AdelitaTextMuted,
                    fontSize = 11.sp
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Círculo de confirmación estilo Figma Pantalla 6
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(AdelitaNavy),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Confirmado",
                        tint = AdelitaCoral,
                        modifier = Modifier.size(38.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = "¡Apartado confirmado!",
                    color = AdelitaNavy,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 22.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Tu pedido comenzará a prepararse al instante. No necesitas hacer fila al recoger.",
                    color = AdelitaTextMuted,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Tarjeta de Comprobante Oficial (AdelitaGOAT Ticket) estilo Figma Pantalla 6
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column {
                        // Barra superior azul noche del ticket
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(AdelitaNavy)
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "AdelitaGOAT Ticket",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = "Comprobante Oficial",
                                    color = AdelitaGold,
                                    fontSize = 11.sp
                                )
                            }
                            Surface(
                                color = Color.White.copy(alpha = 0.16f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = "💵 Físico",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Column(modifier = Modifier.padding(18.dp)) {
                            // Caja dorada del FOLIO
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                color = AdelitaGold.copy(alpha = 0.45f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(vertical = 12.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "FOLIO",
                                        color = AdelitaTextMuted,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp
                                    )
                                    Text(
                                        text = "#${order.folio}",
                                        color = AdelitaNavy,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 26.sp
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            Text(
                                text = "PUESTO",
                                fontSize = 11.sp,
                                color = AdelitaTextMuted,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = order.stallName,
                                fontSize = 15.sp,
                                color = AdelitaNavy,
                                fontWeight = FontWeight.Bold
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            Text(
                                text = "PRODUCTOS",
                                fontSize = 11.sp,
                                color = AdelitaTextMuted,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(6.dp))

                            order.items.forEach { item ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 3.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "${item.quantity}x ${item.productName}",
                                        fontSize = 13.sp,
                                        color = AdelitaTextDark,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text(
                                        text = item.formattedLineTotal,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = AdelitaTextDark
                                    )
                                }
                            }

                            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "MÉTODO DE PAGO",
                                    fontSize = 12.sp,
                                    color = AdelitaTextMuted,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Efectivo al recoger",
                                    fontSize = 13.sp,
                                    color = AdelitaTextDark,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Total",
                                    fontSize = 15.sp,
                                    color = AdelitaNavy,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = order.formattedTotal,
                                    fontSize = 20.sp,
                                    color = AdelitaNavy,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Presenta tu folio al recoger tu pedido.",
                                fontSize = 12.sp,
                                color = AdelitaTextMuted,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(22.dp))

                CoralPrimaryButton(
                    text = "Ver estado del pedido",
                    leadingIcon = Icons.Default.Schedule,
                    onClick = { onViewOrderStatus(order.id) }
                )

                Spacer(modifier = Modifier.height(10.dp))

                TextButton(onClick = onBackHome) {
                    Text(
                        text = "Volver al inicio",
                        color = AdelitaNavy,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}

@Composable
fun OrderHistoryScreen(
    customerViewModel: CustomerViewModel,
    onOpenOrderDetail: (Long) -> Unit,
    onRateOrder: (Long) -> Unit
) {
    val orders by customerViewModel.orders.collectAsState()
    val isLoading by customerViewModel.isLoadingOrders.collectAsState()
    val error by customerViewModel.ordersError.collectAsState()

    LaunchedEffect(Unit) {
        customerViewModel.loadCustomerOrders()
        while (true) {
            kotlinx.coroutines.delay(10_000)
            customerViewModel.loadCustomerOrders()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Mis Pedidos y Apartados",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 22.sp,
                    color = AdelitaNavy
                )
                Text(
                    text = "Sigue la preparación de tu comida en tiempo real",
                    fontSize = 12.sp,
                    color = AdelitaTextMuted
                )
            }
            IconButton(onClick = { customerViewModel.loadCustomerOrders() }) {
                Icon(Icons.Default.Refresh, contentDescription = "Actualizar pedidos", tint = AdelitaNavy)
            }
        }

        when {
            isLoading && orders.isEmpty() -> {
                LoadingStateBox("Cargando historial de pedidos...")
            }
            error != null && orders.isEmpty() -> {
                ErrorStateCard(
                    message = error ?: "Error al cargar pedidos",
                    onRetry = { customerViewModel.loadCustomerOrders() }
                )
            }
            orders.isEmpty() -> {
                EmptyStateCard(
                    title = "Aún no tienes apartados",
                    subtitle = "Tus apartados activos y entregados aparecerán aquí.",
                    icon = Icons.AutoMirrored.Filled.ReceiptLong
                )
            }
            else -> {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(orders, key = { it.id }) { order ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onOpenOrderDetail(order.id) },
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        color = AdelitaGold,
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text(
                                            text = "Folio #${order.folio}",
                                            color = AdelitaNavy,
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 13.sp,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                        )
                                    }
                                    OrderStatusBadge(status = order.status)
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                Text(
                                    text = order.stallName,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = AdelitaTextDark
                                )
                                Text(
                                    text = order.items.joinToString(", ") { "${it.quantity}x ${it.productName}" },
                                    fontSize = 13.sp,
                                    color = AdelitaTextMuted
                                )

                                Spacer(modifier = Modifier.height(10.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    PaymentStatusChip(status = order.paymentStatus)
                                    Text(
                                        text = order.formattedTotal,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 16.sp,
                                        color = AdelitaNavy
                                    )
                                }

                                if (order.status == OrderStatus.ENTREGADO && order.rating == null) {
                                    Spacer(modifier = Modifier.height(12.dp))
                                    CoralPrimaryButton(
                                        text = "Calificar mi experiencia",
                                        leadingIcon = Icons.Default.StarRate,
                                        onClick = { onRateOrder(order.id) }
                                    )
                                } else if (order.rating != null) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Star,
                                            contentDescription = null,
                                            tint = AdelitaGoldDark,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "Calificaste este pedido con ${order.rating.overallScore}/5 estrellas",
                                            fontSize = 12.sp,
                                            color = AdelitaNavy,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderDetailScreen(
    orderId: Long,
    customerViewModel: CustomerViewModel,
    onBack: () -> Unit,
    onOpenTicket: (Long) -> Unit,
    onRateOrder: (Long) -> Unit
) {
    val order by customerViewModel.selectedOrder.collectAsState()
    val isLoading by customerViewModel.isLoadingOrderDetail.collectAsState()
    val error by customerViewModel.orderDetailError.collectAsState()

    LaunchedEffect(orderId) {
        customerViewModel.loadOrderDetail(orderId)
        while (true) {
            kotlinx.coroutines.delay(10_000)
            customerViewModel.loadOrderDetail(orderId)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Estado de tu pedido",
                        fontWeight = FontWeight.Bold,
                        color = AdelitaNavy
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver",
                            tint = AdelitaNavy
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { customerViewModel.loadOrderDetail(orderId) }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refrescar estado", tint = AdelitaNavy)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        }
    ) { padding ->
        when {
            isLoading && order == null -> {
                Box(modifier = Modifier.padding(padding)) {
                    LoadingStateBox("Consultando estado del pedido...")
                }
            }
            error != null && order == null -> {
                Box(modifier = Modifier.padding(padding)) {
                    ErrorStateCard(
                        message = error ?: "Error al cargar el pedido",
                        onRetry = { customerViewModel.loadOrderDetail(orderId) }
                    )
                }
            }
            order != null -> {
                val current = order!!
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.White)
                        .padding(padding)
                        .verticalScroll(rememberScrollState())
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Cabecera estilo Boceto 8.6 + Figma
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(AdelitaGold),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = when (current.status) {
                                OrderStatus.CANCELADO -> Icons.Default.Cancel
                                OrderStatus.ENTREGADO -> Icons.Default.Verified
                                else -> Icons.Default.Check
                            },
                            contentDescription = null,
                            tint = AdelitaNavy,
                            modifier = Modifier.size(34.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = current.status.displayTitle(),
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 20.sp,
                        color = AdelitaNavy
                    )
                    Text(
                        text = "Folio #${current.folio}  •  ${current.stallName}",
                        fontSize = 13.sp,
                        color = AdelitaTextMuted
                    )

                    Spacer(modifier = Modifier.height(14.dp))
                    PaymentStatusChip(status = current.paymentStatus)

                    Spacer(modifier = Modifier.height(20.dp))

                    // Línea de tiempo vertical de progresión de estados (RECIBIDO -> EN_PREPARACION -> LISTO -> ENTREGADO)
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = AdelitaSurfaceGray),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Text(
                                text = "Progresión del pedido",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = AdelitaNavy
                            )
                            Spacer(modifier = Modifier.height(12.dp))

                            if (current.status == OrderStatus.CANCELADO) {
                                TimelineStepRow(
                                    title = "Pedido cancelado",
                                    subtitle = "El apartado fue cancelado antes de su entrega. No se realizó ningún cobro.",
                                    isCompleted = true,
                                    isCurrent = true,
                                    isLast = true,
                                    activeColor = AdelitaCoral
                                )
                            } else {
                                val steps = listOf(
                                    Triple(OrderStatus.RECIBIDO, "Recibido por el puesto", "Tu apartado fue registrado con folio #${current.folio}."),
                                    Triple(OrderStatus.EN_PREPARACION, "En preparación", "El vendedor está preparando tus alimentos."),
                                    Triple(OrderStatus.LISTO, "Listo para recoger", "Acude al mostrador con tu folio y realiza tu pago en efectivo."),
                                    Triple(OrderStatus.ENTREGADO, "Entregado y pagado en efectivo", "Pedido entregado y pagado presencialmente en mostrador.")
                                )
                                val currentIndex = steps.indexOfFirst { it.first == current.status }.coerceAtLeast(0)

                                steps.forEachIndexed { idx, (_, title, subtitle) ->
                                    TimelineStepRow(
                                        title = title,
                                        subtitle = subtitle,
                                        isCompleted = idx <= currentIndex,
                                        isCurrent = idx == currentIndex,
                                        isLast = idx == steps.lastIndex
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Detalle de productos y precios históricos
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Detalle del apartado",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = AdelitaNavy
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            current.items.forEach { item ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "${item.quantity}x ${item.productName} (${item.formattedUnitPrice})",
                                        fontSize = 13.sp,
                                        color = AdelitaTextDark,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text(
                                        text = item.formattedLineTotal,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = AdelitaNavy
                                    )
                                }
                            }
                            if (!current.notes.isNullOrBlank()) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Notas: ${current.notes}",
                                    fontSize = 12.sp,
                                    color = AdelitaTextMuted
                                )
                            }
                            HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Total en efectivo:", fontWeight = FontWeight.Bold, color = AdelitaTextDark)
                                Text(
                                    current.formattedTotal,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 17.sp,
                                    color = AdelitaNavy
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    OutlinedButton(
                        onClick = { onOpenTicket(current.id) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.ConfirmationNumber, contentDescription = null, tint = AdelitaNavy)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Ver Comprobante Oficial (Ticket)", color = AdelitaNavy, fontWeight = FontWeight.Bold)
                    }

                    if (current.status == OrderStatus.ENTREGADO && current.rating == null) {
                        Spacer(modifier = Modifier.height(12.dp))
                        CoralPrimaryButton(
                            text = "Calificar mi experiencia",
                            leadingIcon = Icons.Default.StarRate,
                            onClick = { onRateOrder(current.id) }
                        )
                    } else if (current.rating != null) {
                        Spacer(modifier = Modifier.height(14.dp))
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = AdelitaGold.copy(alpha = 0.35f)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    text = "Tu calificación registrada: ${current.rating.overallScore}/5 ★",
                                    fontWeight = FontWeight.Bold,
                                    color = AdelitaNavy
                                )
                                Text(
                                    text = "Calidad: ${current.rating.productQualityScore}/5 • Espera: ${current.rating.waitTimeScore}/5 • Higiene: ${current.rating.hygieneScore}/5 • Atención: ${current.rating.serviceAttentionScore}/5",
                                    fontSize = 12.sp,
                                    color = AdelitaTextDark
                                )
                                if (!current.rating.comment.isNullOrBlank()) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "\"${current.rating.comment}\"",
                                        fontSize = 12.sp,
                                        color = AdelitaTextMuted
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TimelineStepRow(
    title: String,
    subtitle: String,
    isCompleted: Boolean,
    isCurrent: Boolean,
    isLast: Boolean,
    activeColor: Color = AdelitaCoral
) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(
                        when {
                            isCurrent -> activeColor
                            isCompleted -> AdelitaNavy
                            else -> Color.LightGray
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isCompleted) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(13.dp)
                    )
                }
            }
            if (!isLast) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .height(36.dp)
                        .background(if (isCompleted) AdelitaNavy else Color.LightGray)
                )
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.padding(bottom = if (isLast) 0.dp else 14.dp)) {
            Text(
                text = title,
                fontWeight = if (isCurrent || isCompleted) FontWeight.Bold else FontWeight.Medium,
                fontSize = 14.sp,
                color = if (isCompleted) AdelitaTextDark else AdelitaTextMuted
            )
            Text(
                text = subtitle,
                fontSize = 12.sp,
                color = AdelitaTextMuted
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RateOrderScreen(
    orderId: Long,
    customerViewModel: CustomerViewModel,
    onBack: () -> Unit,
    onRatingSubmitted: () -> Unit
) {
    val order by customerViewModel.selectedOrder.collectAsState()
    val isSubmitting by customerViewModel.isSubmittingRating.collectAsState()
    val ratingError by customerViewModel.ratingError.collectAsState()

    var overallScore by remember { mutableIntStateOf(5) }
    var productQualityScore by remember { mutableIntStateOf(5) }
    var waitTimeScore by remember { mutableIntStateOf(5) }
    var hygieneScore by remember { mutableIntStateOf(5) }
    var serviceAttentionScore by remember { mutableIntStateOf(5) }
    var comment by remember { mutableStateOf("") }

    LaunchedEffect(orderId) {
        customerViewModel.clearRatingError()
        customerViewModel.loadOrderDetail(orderId)
    }

    val overallLabels = mapOf(
        1 to "Mala experiencia (1/5)",
        2 to "Experiencia regular (2/5)",
        3 to "Experiencia aceptable (3/5)",
        4 to "Muy buena experiencia (4/5)",
        5 to "Excelente experiencia (5/5)"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Calificar experiencia",
                            fontWeight = FontWeight.Bold,
                            color = AdelitaNavy,
                            fontSize = 18.sp
                        )
                        order?.let {
                            Text(
                                text = "Puesto ${it.stallName}",
                                fontSize = 12.sp,
                                color = AdelitaTextMuted
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver",
                            tint = AdelitaNavy
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 14.dp)
        ) {
            // Resumen del pedido estilo Figma Pantalla 7
            order?.let { current ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = AdelitaSurfaceGray),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ProductVisualAvatar(imageKey = "tacos", size = 50.dp)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = current.stallName,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = AdelitaTextDark
                            )
                            Text(
                                text = current.items.joinToString(", ") { "${it.quantity}x ${it.productName}" },
                                fontSize = 12.sp,
                                color = AdelitaTextMuted
                            )
                        }
                        Surface(
                            color = Color.White,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "#${current.folio}",
                                color = AdelitaNavy,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (ratingError != null) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    colors = CardDefaults.cardColors(containerColor = AdelitaCoralLight),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        text = ratingError ?: "",
                        color = AdelitaTextDark,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            // Valoración general con estrellas doradas estilo Figma Pantalla 7
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "¿Cómo fue tu experiencia?",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 18.sp,
                        color = AdelitaNavy
                    )
                    Text(
                        text = "Toca una estrella para evaluar el pedido global",
                        fontSize = 12.sp,
                        color = AdelitaTextMuted
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        for (star in 1..5) {
                            IconButton(
                                onClick = { overallScore = star },
                                modifier = Modifier.size(42.dp)
                            ) {
                                Icon(
                                    imageVector = if (star <= overallScore) Icons.Default.Star else Icons.Default.StarBorder,
                                    contentDescription = "$star estrellas",
                                    tint = if (star <= overallScore) AdelitaGoldDark else Color.LightGray,
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = overallLabels[overallScore] ?: "",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = AdelitaNavy
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Criterios detallados estilo Figma Pantalla 7
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    CriteriaSelectorRow(
                        title = "🍴 Calidad de los productos",
                        options = listOf("Excelente" to 5, "Buena" to 4, "Regular" to 3, "Baja" to 2),
                        selectedScore = productQualityScore,
                        onSelect = { productQualityScore = it }
                    )

                    CriteriaSelectorRow(
                        title = "🕒 Tiempo de atención en mostrador",
                        options = listOf("Rápido" to 5, "Adecuado" to 4, "Demorado" to 2),
                        selectedScore = waitTimeScore,
                        onSelect = { waitTimeScore = it }
                    )

                    CriteriaSelectorRow(
                        title = "🧼 Higiene del puesto",
                        options = listOf("Muy limpio" to 5, "Aceptable" to 4, "Descuidado" to 2),
                        selectedScore = hygieneScore,
                        onSelect = { hygieneScore = it }
                    )

                    CriteriaSelectorRow(
                        title = "😊 Atención recibida",
                        options = listOf("Amable" to 5, "Buena" to 4, "Regular" to 3),
                        selectedScore = serviceAttentionScore,
                        onSelect = { serviceAttentionScore = it }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Observaciones adicionales
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Observaciones adicionales",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = AdelitaNavy
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = comment,
                        onValueChange = { if (it.length <= 200) comment = it },
                        placeholder = { Text("Cuéntanos tu experiencia (opcional)...") },
                        minLines = 3,
                        maxLines = 4,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Mínimo 5 caracteres si deseas detallar",
                            fontSize = 11.sp,
                            color = AdelitaTextMuted
                        )
                        Text(
                            text = "${comment.length}/200",
                            fontSize = 11.sp,
                            color = AdelitaTextMuted
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Surface(
                color = AdelitaSurfaceGray,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.School,
                        contentDescription = null,
                        tint = AdelitaNavy,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Tus comentarios ayudan a otros estudiantes y mejoran los tiempos de preparación en el campus.",
                        fontSize = 11.sp,
                        color = AdelitaTextMuted
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            CoralPrimaryButton(
                text = "Enviar calificación  ▷",
                loading = isSubmitting,
                leadingIcon = Icons.AutoMirrored.Filled.Send,
                onClick = {
                    customerViewModel.submitRating(
                        orderId = orderId,
                        overallScore = overallScore,
                        productQualityScore = productQualityScore,
                        waitTimeScore = waitTimeScore,
                        hygieneScore = hygieneScore,
                        serviceAttentionScore = serviceAttentionScore,
                        comment = comment,
                        onSuccess = onRatingSubmitted
                    )
                }
            )
        }
    }
}

@Composable
private fun CriteriaSelectorRow(
    title: String,
    options: List<Pair<String, Int>>,
    selectedScore: Int,
    onSelect: (Int) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
            color = AdelitaTextDark
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            options.forEach { (label, score) ->
                val selected = selectedScore == score
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .height(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onSelect(score) },
                    color = if (selected) AdelitaNavy else AdelitaSurfaceGray,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = label,
                            color = if (selected) Color.White else AdelitaTextDark,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun FavoritesScreen(
    customerViewModel: CustomerViewModel,
    onOpenStall: (Long) -> Unit
) {
    val favorites by customerViewModel.favorites.collectAsState()
    val isLoading by customerViewModel.isLoadingFavorites.collectAsState()
    val error by customerViewModel.favoritesError.collectAsState()

    LaunchedEffect(Unit) {
        customerViewModel.loadFavorites()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Puestos Favoritos",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 22.sp,
                    color = AdelitaNavy
                )
                Text(
                    text = "Acceso rápido a tus puestos preferidos del campus",
                    fontSize = 12.sp,
                    color = AdelitaTextMuted
                )
            }
            IconButton(onClick = { customerViewModel.loadFavorites() }) {
                Icon(Icons.Default.Refresh, contentDescription = "Actualizar favoritos", tint = AdelitaNavy)
            }
        }

        when {
            isLoading && favorites.isEmpty() -> {
                LoadingStateBox("Cargando tus puestos favoritos...")
            }
            error != null && favorites.isEmpty() -> {
                ErrorStateCard(
                    message = error ?: "Error al cargar favoritos",
                    onRetry = { customerViewModel.loadFavorites() }
                )
            }
            favorites.isEmpty() -> {
                EmptyStateCard(
                    title = "Sin puestos favoritos aún",
                    subtitle = "Toca el ícono de corazón en cualquier puesto para guardarlo aquí.",
                    icon = Icons.Default.FavoriteBorder
                )
            }
            else -> {
                LazyColumn(contentPadding = PaddingValues(bottom = 24.dp)) {
                    items(favorites, key = { it.id }) { stall ->
                        StallSummaryCard(
                            stall = stall,
                            onOpenStall = { onOpenStall(stall.id) },
                            onToggleFavorite = {
                                customerViewModel.toggleFavorite(stall.id, stall.isFavorite)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ProfileScreen(
    user: UserDto?,
    authViewModel: AuthViewModel,
    onLoggedOut: () -> Unit
) {
    val baseUrl by authViewModel.baseUrl.collectAsState()
    var urlInput by remember(baseUrl) { mutableStateOf(baseUrl) }
    var urlSavedBanner by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .verticalScroll(rememberScrollState())
            .padding(22.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(12.dp))
        AdelitaLogoBadge(size = 76.dp, iconSize = 40.dp)
        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = user?.name ?: "Usuario AdelitaGOAT",
            fontWeight = FontWeight.ExtraBold,
            fontSize = 20.sp,
            color = AdelitaNavy
        )
        Text(
            text = user?.email ?: "",
            fontSize = 13.sp,
            color = AdelitaTextMuted
        )
        Spacer(modifier = Modifier.height(8.dp))
        Surface(
            color = AdelitaGold,
            shape = RoundedCornerShape(16.dp)
        ) {
            Text(
                text = if (user?.role == UserRole.VENDEDOR) "Rol: VENDEDOR DEL PUESTO" else "Rol: CLIENTE / ESTUDIANTE",
                color = AdelitaNavy,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = AdelitaSurfaceGray),
            shape = RoundedCornerShape(14.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Conexión con API Backend Ktor",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = AdelitaNavy
                )
                Text(
                    text = "Por defecto en el emulador Android: http://10.0.2.2:8080",
                    fontSize = 12.sp,
                    color = AdelitaTextMuted
                )
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = urlInput,
                    onValueChange = {
                        urlInput = it
                        urlSavedBanner = false
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = {
                        authViewModel.updateBaseUrl(urlInput)
                        urlSavedBanner = true
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AdelitaNavy),
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Guardar URL")
                }
                if (urlSavedBanner) {
                    Text(
                        text = "URL actualizada a ${authViewModel.baseUrl.value}",
                        color = AdelitaSuccessGreen,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        CoralPrimaryButton(
            text = "Cerrar sesión",
            leadingIcon = Icons.AutoMirrored.Filled.Logout,
            onClick = { authViewModel.logout(onLoggedOut) }
        )
    }
}
