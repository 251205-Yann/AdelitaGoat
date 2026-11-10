package mx.edu.upchiapas.adelitagoat.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import mx.edu.upchiapas.adelitagoat.domain.*
import mx.edu.upchiapas.adelitagoat.ui.components.*
import mx.edu.upchiapas.adelitagoat.ui.theme.*
import mx.edu.upchiapas.adelitagoat.ui.viewmodel.VendorViewModel
import java.util.Locale

@Composable
fun VendorOrdersScreen(vendorViewModel: VendorViewModel) {
    val vendorStalls by vendorViewModel.vendorStalls.collectAsState()
    val selectedStallId by vendorViewModel.selectedStallId.collectAsState()
    val categories by vendorViewModel.categories.collectAsState()
    val vendorOrders by vendorViewModel.vendorOrders.collectAsState()
    val statusFilter by vendorViewModel.statusFilter.collectAsState()
    val isLoading by vendorViewModel.isLoading.collectAsState()
    val errorMessage by vendorViewModel.errorMessage.collectAsState()
    val actionMessage by vendorViewModel.actionMessage.collectAsState()

    var showStallDialog by remember { mutableStateOf(false) }
    var editingStall by remember { mutableStateOf<StallSummaryDto?>(null) }

    val currentStallDetail = vendorStalls.firstOrNull { it.stall.id == selectedStallId }
        ?: vendorStalls.firstOrNull()

    LaunchedEffect(Unit) {
        vendorViewModel.loadVendorDashboard()
        while (true) {
            kotlinx.coroutines.delay(10_000)
            vendorViewModel.loadVendorDashboard()
        }
    }

    if (showStallDialog) {
        StallFormDialog(
            categories = categories,
            existingStall = editingStall,
            onDismiss = {
                showStallDialog = false
                editingStall = null
            },
            onSave = { name, desc, location, category, prepMinutes, isOpen, imgKey ->
                vendorViewModel.saveStall(
                    editingStallId = editingStall?.id,
                    name = name,
                    description = desc,
                    campusLocation = location,
                    category = category,
                    prepTimeMinutesInput = prepMinutes,
                    isOpen = isOpen,
                    imageKey = imgKey,
                    onSaved = {
                        showStallDialog = false
                        editingStall = null
                    }
                )
            }
        )
    }

    val filterOptions: List<Pair<String, OrderStatus?>> = listOf(
        "Todos" to null,
        "Recibidos" to OrderStatus.RECIBIDO,
        "En preparación" to OrderStatus.EN_PREPARACION,
        "Listos" to OrderStatus.LISTO,
        "Entregados" to OrderStatus.ENTREGADO,
        "Cancelados" to OrderStatus.CANCELADO
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        // Cabecera Panel Vendedor
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(AdelitaNavy)
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Panel de Vendedor — Pedidos",
                    color = Color.White,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 19.sp
                )
                Text(
                    text = "Gestiona tu puesto, comandas y cobro en efectivo",
                    color = AdelitaGold,
                    fontSize = 12.sp
                )
            }
            IconButton(onClick = { vendorViewModel.loadVendorDashboard() }) {
                Icon(Icons.Default.Refresh, contentDescription = "Actualizar", tint = Color.White)
            }
        }

        // Selector de puesto propio y botón para agregar puesto
        if (vendorStalls.isNotEmpty()) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                items(vendorStalls, key = { it.stall.id }) { detail ->
                    val isSelected = detail.stall.id == currentStallDetail?.stall?.id
                    FilterChip(
                        selected = isSelected,
                        onClick = { vendorViewModel.selectVendorStall(detail.stall.id) },
                        label = {
                            Text(
                                text = detail.stall.name,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) Color.White else AdelitaNavy
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Storefront,
                                contentDescription = null,
                                tint = if (isSelected) Color.White else AdelitaNavy,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        shape = RoundedCornerShape(20.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = AdelitaCoral,
                            containerColor = AdelitaSurfaceGray
                        ),
                        border = null
                    )
                }
                item {
                    AssistChip(
                        onClick = {
                            editingStall = null
                            showStallDialog = true
                        },
                        label = { Text("+ Nuevo puesto", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = AdelitaNavy) },
                        shape = RoundedCornerShape(20.dp)
                    )
                }
            }
        }

        if (currentStallDetail != null) {
            VendorStallSummaryBanner(
                stall = currentStallDetail.stall,
                onEditStall = {
                    editingStall = currentStallDetail.stall
                    showStallDialog = true
                },
                onToggleOpen = {
                    vendorViewModel.toggleStallOpenStatus(currentStallDetail.stall)
                }
            )
        }

        // Filtro por estado del pedido
        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(filterOptions) { (label, status) ->
                val selected = statusFilter == status
                FilterChip(
                    selected = selected,
                    onClick = { vendorViewModel.setOrderStatusFilter(status) },
                    label = {
                        Text(
                            text = label,
                            fontSize = 12.sp,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                            color = if (selected) Color.White else AdelitaTextDark
                        )
                    },
                    shape = RoundedCornerShape(18.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = AdelitaNavy,
                        containerColor = AdelitaSurfaceGray
                    ),
                    border = null
                )
            }
        }

        if (actionMessage != null) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp),
                colors = CardDefaults.cardColors(containerColor = AdelitaSuccessBg),
                shape = RoundedCornerShape(10.dp)
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = AdelitaSuccessGreen)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = actionMessage ?: "",
                        color = AdelitaTextDark,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = { vendorViewModel.clearMessages() },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Cerrar", modifier = Modifier.size(16.dp))
                    }
                }
            }
        }

        if (errorMessage != null) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp),
                colors = CardDefaults.cardColors(containerColor = AdelitaCoralLight),
                shape = RoundedCornerShape(10.dp)
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = AdelitaCoral)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = errorMessage ?: "",
                        color = AdelitaTextDark,
                        fontSize = 12.sp,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = { vendorViewModel.clearMessages() },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Cerrar", modifier = Modifier.size(16.dp))
                    }
                }
            }
        }

        when {
            isLoading && vendorStalls.isEmpty() && vendorOrders.isEmpty() -> {
                LoadingStateBox("Cargando panel del vendedor...")
            }
            vendorStalls.isEmpty() -> {
                EmptyStateCard(
                    title = "Aún no has registrado tu puesto",
                    subtitle = "Registra el nombre, descripción, ubicación concreta, categoría y tiempo estimado de preparación de tu puesto para comenzar a operar.",
                    icon = Icons.Default.Storefront,
                    actionLabel = "Registrar mi puesto",
                    onAction = {
                        editingStall = null
                        showStallDialog = true
                    }
                )
            }
            vendorOrders.isEmpty() -> {
                EmptyStateCard(
                    title = "No hay pedidos en este filtro",
                    subtitle = "Los nuevos apartados de clientes aparecerán aquí con su folio y detalle.",
                    icon = Icons.Default.Inbox
                )
            }
            else -> {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(vendorOrders, key = { it.id }) { order ->
                        VendorOrderCard(
                            order = order,
                            onAdvanceStatus = { nextStatus ->
                                vendorViewModel.updateOrderStatus(order, nextStatus)
                            },
                            onCancelOrder = {
                                vendorViewModel.updateOrderStatus(order, OrderStatus.CANCELADO)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun VendorOrderCard(
    order: OrderDto,
    onAdvanceStatus: (OrderStatus) -> Unit,
    onCancelOrder: () -> Unit
) {
    val timeLabel = remember(order.createdAt) {
        order.createdAt.substringAfter("T", "").take(5).ifEmpty { order.createdAt.take(16) }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = AdelitaGold,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "Folio #${order.folio}",
                            color = AdelitaNavy,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 14.sp,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "🕒 $timeLabel hrs",
                        fontSize = 12.sp,
                        color = AdelitaTextMuted
                    )
                }
                OrderStatusBadge(status = order.status)
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "Cliente: ${order.customerName}",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = AdelitaTextDark
            )
            Text(
                text = "Puesto: ${order.stallName}",
                fontSize = 12.sp,
                color = AdelitaTextMuted
            )

            Spacer(modifier = Modifier.height(8.dp))

            Surface(
                color = AdelitaSurfaceGray,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    order.items.forEach { item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "${item.quantity}x ${item.productName}",
                                fontWeight = FontWeight.SemiBold,
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
                    if (!order.notes.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Nota del cliente: ${order.notes}",
                            fontSize = 12.sp,
                            color = AdelitaCoral,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

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
                    fontSize = 18.sp,
                    color = AdelitaNavy
                )
            }

            // Botones de transición válida de estado
            val nextStatus = order.status.nextVendorStatus()
            if (nextStatus != null || order.status.canCancel()) {
                Spacer(modifier = Modifier.height(14.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (order.status.canCancel()) {
                        OutlinedButton(
                            onClick = onCancelOrder,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(0.4f)
                        ) {
                            Text("Cancelar", color = AdelitaCoral, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }

                    if (nextStatus != null) {
                        val actionLabel = when (nextStatus) {
                            OrderStatus.EN_PREPARACION -> "Pasar a En preparación"
                            OrderStatus.LISTO -> "Marcar Listo para recoger"
                            OrderStatus.ENTREGADO -> "Entregar y Cobrar Efectivo"
                            else -> "Avanzar"
                        }
                        Button(
                            onClick = { onAdvanceStatus(nextStatus) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (nextStatus == OrderStatus.ENTREGADO) AdelitaSuccessGreen else AdelitaCoral
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(0.6f)
                        ) {
                            Text(
                                text = actionLabel,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun VendorMenuScreen(vendorViewModel: VendorViewModel) {
    val vendorStalls by vendorViewModel.vendorStalls.collectAsState()
    val selectedStallId by vendorViewModel.selectedStallId.collectAsState()
    val categories by vendorViewModel.categories.collectAsState()
    val isLoading by vendorViewModel.isLoading.collectAsState()
    val errorMessage by vendorViewModel.errorMessage.collectAsState()
    val actionMessage by vendorViewModel.actionMessage.collectAsState()

    var showProductDialog by remember { mutableStateOf(false) }
    var editingProduct by remember { mutableStateOf<ProductDto?>(null) }
    var showStallDialog by remember { mutableStateOf(false) }
    var editingStall by remember { mutableStateOf<StallSummaryDto?>(null) }

    LaunchedEffect(Unit) {
        vendorViewModel.loadVendorDashboard()
        while (true) {
            kotlinx.coroutines.delay(10_000)
            vendorViewModel.loadVendorDashboard()
        }
    }

    val currentStallDetail = vendorStalls.firstOrNull { it.stall.id == selectedStallId }
        ?: vendorStalls.firstOrNull()

    if (showStallDialog) {
        StallFormDialog(
            categories = categories,
            existingStall = editingStall,
            onDismiss = {
                showStallDialog = false
                editingStall = null
            },
            onSave = { name, desc, location, category, prepMinutes, isOpen, imgKey ->
                vendorViewModel.saveStall(
                    editingStallId = editingStall?.id,
                    name = name,
                    description = desc,
                    campusLocation = location,
                    category = category,
                    prepTimeMinutesInput = prepMinutes,
                    isOpen = isOpen,
                    imageKey = imgKey,
                    onSaved = {
                        showStallDialog = false
                        editingStall = null
                    }
                )
            }
        )
    }

    if (showProductDialog && currentStallDetail != null) {
        ProductFormDialog(
            stallId = currentStallDetail.stall.id,
            stallName = currentStallDetail.stall.name,
            categories = categories,
            existingProduct = editingProduct,
            onDismiss = {
                showProductDialog = false
                editingProduct = null
            },
            onSave = { catId, name, desc, pricePesos, available, imgKey ->
                vendorViewModel.saveProduct(
                    stallId = currentStallDetail.stall.id,
                    editingProductId = editingProduct?.id,
                    categoryId = catId,
                    name = name,
                    description = desc,
                    pricePesosInput = pricePesos,
                    available = available,
                    imageKey = imgKey,
                    onSaved = {
                        showProductDialog = false
                        editingProduct = null
                    }
                )
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(AdelitaNavy)
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Gestión de Puesto y Menú",
                    color = Color.White,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 19.sp
                )
                Text(
                    text = "Configura tu puesto, productos, precios y disponibilidad",
                    color = AdelitaGold,
                    fontSize = 12.sp
                )
            }
            Button(
                onClick = {
                    if (currentStallDetail == null) {
                        editingStall = null
                        showStallDialog = true
                    } else {
                        editingProduct = null
                        showProductDialog = true
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = AdelitaCoral),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = if (currentStallDetail == null) "Puesto" else "Nuevo",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }
        }

        if (vendorStalls.isNotEmpty()) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                items(vendorStalls, key = { it.stall.id }) { detail ->
                    val isSelected = detail.stall.id == currentStallDetail?.stall?.id
                    FilterChip(
                        selected = isSelected,
                        onClick = { vendorViewModel.selectVendorStall(detail.stall.id) },
                        label = {
                            Text(
                                text = detail.stall.name,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) Color.White else AdelitaNavy
                            )
                        },
                        shape = RoundedCornerShape(20.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = AdelitaNavy,
                            containerColor = AdelitaSurfaceGray
                        ),
                        border = null
                    )
                }
                item {
                    AssistChip(
                        onClick = {
                            editingStall = null
                            showStallDialog = true
                        },
                        label = { Text("+ Nuevo puesto", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = AdelitaNavy) },
                        shape = RoundedCornerShape(20.dp)
                    )
                }
            }
        }

        if (currentStallDetail != null) {
            VendorStallSummaryBanner(
                stall = currentStallDetail.stall,
                onEditStall = {
                    editingStall = currentStallDetail.stall
                    showStallDialog = true
                },
                onToggleOpen = {
                    vendorViewModel.toggleStallOpenStatus(currentStallDetail.stall)
                }
            )
        }

        if (actionMessage != null) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 4.dp),
                colors = CardDefaults.cardColors(containerColor = AdelitaSuccessBg),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(
                    text = actionMessage ?: "",
                    color = AdelitaTextDark,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(10.dp)
                )
            }
        }

        if (errorMessage != null) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 4.dp),
                colors = CardDefaults.cardColors(containerColor = AdelitaCoralLight),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(
                    text = errorMessage ?: "",
                    color = AdelitaTextDark,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(10.dp)
                )
            }
        }

        when {
            isLoading && currentStallDetail == null -> {
                LoadingStateBox("Cargando menú de tu puesto...")
            }
            currentStallDetail == null -> {
                EmptyStateCard(
                    title = "Aún no has registrado tu puesto",
                    subtitle = "Registra los datos de tu puesto (nombre, descripción, ubicación concreta, categoría y tiempo de preparación) antes de agregar productos a tu menú.",
                    icon = Icons.Default.Storefront,
                    actionLabel = "Registrar mi puesto",
                    onAction = {
                        editingStall = null
                        showStallDialog = true
                    }
                )
            }
            currentStallDetail.products.isEmpty() -> {
                EmptyStateCard(
                    title = "Tu menú aún no tiene productos",
                    subtitle = "Toca el botón '+ Nuevo' para registrar tu primer platillo o bebida.",
                    actionLabel = "Agregar primer producto",
                    onAction = {
                        editingProduct = null
                        showProductDialog = true
                    }
                )
            }
            else -> {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(currentStallDetail.products, key = { it.id }) { product ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                ProductVisualAvatar(imageKey = product.imageKey, size = 56.dp)
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = product.name,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = AdelitaTextDark
                                    )
                                    Text(
                                        text = "${product.categoryName} • ${product.formattedPrice}",
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 13.sp,
                                        color = AdelitaNavy
                                    )
                                    Text(
                                        text = product.description,
                                        fontSize = 12.sp,
                                        color = AdelitaTextMuted,
                                        maxLines = 2
                                    )
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = if (product.available) "Disponible" else "Agotado",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (product.available) AdelitaSuccessGreen else AdelitaCoral
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Switch(
                                            checked = product.available,
                                            onCheckedChange = {
                                                vendorViewModel.toggleProductAvailability(product)
                                            }
                                        )
                                    }
                                    TextButton(
                                        onClick = {
                                            editingProduct = product
                                            showProductDialog = true
                                        }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Edit,
                                            contentDescription = null,
                                            modifier = Modifier.size(15.dp),
                                            tint = AdelitaNavy
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Editar", fontSize = 12.sp, color = AdelitaNavy, fontWeight = FontWeight.Bold)
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

@Composable
private fun ProductFormDialog(
    stallId: Long,
    stallName: String,
    categories: List<CategoryDto>,
    existingProduct: ProductDto?,
    onDismiss: () -> Unit,
    onSave: (Long, String, String, String, Boolean, String) -> Unit
) {
    var name by remember { mutableStateOf(existingProduct?.name ?: "") }
    var description by remember { mutableStateOf(existingProduct?.description ?: "") }
    var pricePesos by remember {
        mutableStateOf(
            if (existingProduct != null) {
                String.format(Locale.US, "%.2f", existingProduct.priceCents / 100.0)
            } else ""
        )
    }
    var selectedCategoryId by remember {
        mutableLongStateOf(existingProduct?.categoryId ?: categories.firstOrNull()?.id ?: 1L)
    }
    var available by remember { mutableStateOf(existingProduct?.available ?: true) }
    var imageKey by remember { mutableStateOf(existingProduct?.imageKey ?: "taco") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(
                    text = if (existingProduct == null) "Nuevo producto" else "Editar producto",
                    fontWeight = FontWeight.Bold,
                    color = AdelitaNavy
                )
                Text(
                    text = stallName,
                    fontSize = 12.sp,
                    color = AdelitaTextMuted
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nombre del platillo o bebida") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Descripción e ingredientes") },
                    minLines = 2,
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = pricePesos,
                    onValueChange = { pricePesos = it },
                    label = { Text("Precio en MXN (ej. 55.00)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = "Categoría:",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp,
                    color = AdelitaTextDark
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(categories, key = { it.id }) { cat ->
                        val selected = selectedCategoryId == cat.id
                        FilterChip(
                            selected = selected,
                            onClick = {
                                selectedCategoryId = cat.id
                                imageKey = cat.slug
                            },
                            label = { Text(cat.name, fontSize = 11.sp) }
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Disponible para apartar", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    Switch(checked = available, onCheckedChange = { available = it })
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(selectedCategoryId, name, description, pricePesos, available, imageKey)
                },
                colors = ButtonDefaults.buttonColors(containerColor = AdelitaCoral)
            ) {
                Text("Guardar", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", color = AdelitaNavy)
            }
        }
    )
}

@Composable
private fun VendorStallSummaryBanner(
    stall: StallSummaryDto,
    onEditStall: () -> Unit,
    onToggleOpen: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp),
        colors = CardDefaults.cardColors(containerColor = AdelitaSurfaceGray),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stall.name,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 15.sp,
                        color = AdelitaNavy
                    )
                    Text(
                        text = "📍 ${stall.campusLocation} • ${stall.category} • ~${stall.prepTimeMinutes} min",
                        fontSize = 11.sp,
                        color = AdelitaTextMuted
                    )
                }
                TextButton(onClick = onEditStall) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = null,
                        tint = AdelitaNavy,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Editar puesto", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = AdelitaNavy)
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (stall.isOpen) "Estado: ABIERTO (recibiendo apartados)" else "Estado: CERRADO (no recibe apartados)",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (stall.isOpen) AdelitaSuccessGreen else AdelitaCoral
                )
                Switch(
                    checked = stall.isOpen,
                    onCheckedChange = { onToggleOpen() }
                )
            }
        }
    }
}

@Composable
private fun StallFormDialog(
    categories: List<CategoryDto>,
    existingStall: StallSummaryDto?,
    onDismiss: () -> Unit,
    onSave: (String, String, String, String, String, Boolean, String) -> Unit
) {
    var name by remember { mutableStateOf(existingStall?.name ?: "") }
    var description by remember { mutableStateOf(existingStall?.description ?: "") }
    var campusLocation by remember { mutableStateOf(existingStall?.campusLocation ?: "") }
    var selectedCategoryName by remember {
        mutableStateOf(existingStall?.category ?: categories.firstOrNull()?.name ?: "Tacos")
    }
    var prepTimeMinutes by remember {
        mutableStateOf(existingStall?.prepTimeMinutes?.toString() ?: "10")
    }
    var isOpen by remember { mutableStateOf(existingStall?.isOpen ?: true) }
    var imageKey by remember {
        mutableStateOf(existingStall?.imageKey ?: categories.firstOrNull()?.slug ?: "tacos")
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (existingStall == null) "Registrar puesto" else "Editar puesto",
                fontWeight = FontWeight.Bold,
                color = AdelitaNavy
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nombre del puesto") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Descripción del puesto y especialidad") },
                    minLines = 2,
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = campusLocation,
                    onValueChange = { campusLocation = it },
                    label = { Text("Ubicación textual concreta") },
                    placeholder = { Text("Ej. Portón principal, acera norte") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = prepTimeMinutes,
                    onValueChange = { prepTimeMinutes = it },
                    label = { Text("Tiempo estimado de preparación (minutos)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = "Categoría principal:",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp,
                    color = AdelitaTextDark
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(categories, key = { it.id }) { cat ->
                        val selected = selectedCategoryName.equals(cat.name, ignoreCase = true)
                        FilterChip(
                            selected = selected,
                            onClick = {
                                selectedCategoryName = cat.name
                                imageKey = cat.slug
                            },
                            label = { Text(cat.name, fontSize = 11.sp) }
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Puesto abierto al público", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    Switch(checked = isOpen, onCheckedChange = { isOpen = it })
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        name,
                        description,
                        campusLocation,
                        selectedCategoryName,
                        prepTimeMinutes,
                        isOpen,
                        imageKey
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = AdelitaCoral)
            ) {
                Text(
                    text = if (existingStall == null) "Registrar puesto" else "Guardar cambios",
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", color = AdelitaNavy)
            }
        }
    )
}
