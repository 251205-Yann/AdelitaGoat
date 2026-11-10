package mx.edu.upchiapas.adelitagoat.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import mx.edu.upchiapas.adelitagoat.data.CartAddResult
import mx.edu.upchiapas.adelitagoat.domain.*
import mx.edu.upchiapas.adelitagoat.ui.components.*
import mx.edu.upchiapas.adelitagoat.ui.theme.*
import mx.edu.upchiapas.adelitagoat.ui.viewmodel.CustomerViewModel

@Composable
fun StallConflictDialog(
    conflict: CartAddResult.ConflictDifferentStall?,
    onConfirmReplace: () -> Unit,
    onDismiss: () -> Unit
) {
    if (conflict == null) return
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "Apartado de un solo puesto",
                fontWeight = FontWeight.Bold,
                color = AdelitaNavy
            )
        },
        text = {
            Text(
                "Actualmente tienes productos de '${conflict.currentStallName}' en tu apartado. ¿Deseas vaciar el apartado actual y agregar '${conflict.newProduct.name}' de '${conflict.newProduct.stallName}'?",
                fontSize = 14.sp
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirmReplace,
                colors = ButtonDefaults.buttonColors(containerColor = AdelitaCoral)
            ) {
                Text("Reemplazar apartado")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Conservar actual", color = AdelitaNavy)
            }
        }
    )
}

@Composable
fun CustomerHomeScreen(
    currentUser: UserDto?,
    customerViewModel: CustomerViewModel,
    onOpenStall: (Long) -> Unit,
    onOpenCart: () -> Unit
) {
    val categories by customerViewModel.categories.collectAsState()
    val selectedCategory by customerViewModel.selectedCategory.collectAsState()
    val searchQuery by customerViewModel.searchQuery.collectAsState()
    val stalls by customerViewModel.stalls.collectAsState()
    val isLoading by customerViewModel.isLoadingStalls.collectAsState()
    val error by customerViewModel.stallsError.collectAsState()
    val cart by customerViewModel.cart.collectAsState()
    val conflict by customerViewModel.pendingStallConflict.collectAsState()

    LaunchedEffect(Unit) {
        customerViewModel.loadHomeCatalog()
    }

    StallConflictDialog(
        conflict = conflict,
        onConfirmReplace = { customerViewModel.confirmReplaceCartWithNewStall() },
        onDismiss = { customerViewModel.dismissStallConflict() }
    )

    val firstName = currentUser?.name?.split(" ")?.firstOrNull() ?: "Estudiante"
    val allCategoryNames = remember(categories) {
        listOf("Todos") + categories.map { it.name }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.White)) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = if (cart.isEmpty) 24.dp else 96.dp)
        ) {
            // Encabezado superior estilo Figma Pantalla 3
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            AdelitaLogoBadge(size = 36.dp, iconSize = 20.dp)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "AdelitaGOAT",
                                    color = AdelitaNavy,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.LocationOn,
                                        contentDescription = null,
                                        tint = AdelitaCoral,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Text(
                                        text = "Frente a la Universidad Politécnica de Chiapas",
                                        color = AdelitaTextMuted,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }

                        IconButton(onClick = { customerViewModel.loadHomeCatalog() }) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Actualizar puestos",
                                tint = AdelitaNavy
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "¡Hola, $firstName!",
                        color = AdelitaTextMuted,
                        fontSize = 14.sp
                    )
                    Text(
                        text = "¿Qué vas a pedir hoy?",
                        color = AdelitaNavy,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 22.sp
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Barra de búsqueda
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { customerViewModel.updateSearchQuery(it) },
                        placeholder = { Text("Buscar en el menú o puesto...") },
                        leadingIcon = {
                            Icon(Icons.Default.Search, contentDescription = null, tint = AdelitaTextMuted)
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { customerViewModel.updateSearchQuery("") }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Limpiar búsqueda")
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedContainerColor = AdelitaSurfaceGray,
                            focusedContainerColor = Color.White,
                            unfocusedBorderColor = Color.Transparent,
                            focusedBorderColor = AdelitaNavy
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // Chips de categorías
            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(allCategoryNames) { catName ->
                        val isSelected = selectedCategory.equals(catName, ignoreCase = true)
                        FilterChip(
                            selected = isSelected,
                            onClick = { customerViewModel.selectCategory(catName) },
                            label = {
                                Text(
                                    text = catName,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) Color.White else AdelitaTextDark
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
                }
                Spacer(modifier = Modifier.height(14.dp))
            }

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Puestos disponibles cerca de ti",
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = AdelitaTextDark
                    )
                    Text(
                        text = "${stalls.size} puestos",
                        fontSize = 12.sp,
                        color = AdelitaTextMuted
                    )
                }
            }

            when {
                isLoading && stalls.isEmpty() -> {
                    item { LoadingStateBox("Buscando puestos disponibles...") }
                }
                error != null && stalls.isEmpty() -> {
                    item {
                        ErrorStateCard(
                            message = error ?: "Error de red",
                            onRetry = { customerViewModel.loadHomeCatalog() }
                        )
                    }
                }
                stalls.isEmpty() -> {
                    item {
                        val isFiltered = searchQuery.isNotBlank() || !selectedCategory.equals("Todos", ignoreCase = true)
                        if (isFiltered) {
                            EmptyStateCard(
                                title = "No se encontraron puestos para este filtro",
                                subtitle = "Intenta con otro término de búsqueda o selecciona la categoría 'Todos'.",
                                actionLabel = "Ver todos los puestos",
                                onAction = {
                                    customerViewModel.updateSearchQuery("")
                                    customerViewModel.selectCategory("Todos")
                                }
                            )
                        } else {
                            EmptyStateCard(
                                title = "Aún no hay puestos registrados",
                                subtitle = "Cuando un vendedor registre su puesto y menú en la plataforma, aparecerá aquí. Toca Actualizar para volver a consultar.",
                                icon = Icons.Default.Storefront,
                                actionLabel = "Actualizar catálogo",
                                onAction = { customerViewModel.loadHomeCatalog() }
                            )
                        }
                    }
                }
                else -> {
                    items(stalls, key = { it.id }) { stall ->
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

        if (!cart.isEmpty) {
            FloatingCartBanner(
                cart = cart,
                onClick = onOpenCart,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
            )
        }
    }
}

@Composable
fun StallSummaryCard(
    stall: StallSummaryDto,
    onOpenStall: () -> Unit,
    onToggleFavorite: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 7.dp)
            .clickable { onOpenStall() },
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
            ProductVisualAvatar(imageKey = stall.imageKey, size = 68.dp)
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stall.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = AdelitaTextDark,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = onToggleFavorite,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = if (stall.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Favorito",
                            tint = if (stall.isFavorite) AdelitaCoral else AdelitaTextMuted
                        )
                    }
                }

                Text(
                    text = stall.description,
                    color = AdelitaTextMuted,
                    fontSize = 12.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "📍 ${stall.campusLocation}",
                    color = AdelitaNavy,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    GoldRatingChip(rating = stall.averageRating, count = stall.ratingCount)
                    Surface(
                        color = AdelitaSurfaceGray,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Schedule,
                                contentDescription = null,
                                tint = AdelitaNavy,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "${stall.prepTimeMinutes} min espera",
                                fontSize = 11.sp,
                                color = AdelitaNavy,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                    Surface(
                        color = if (stall.isOpen) AdelitaCoralLight else AdelitaSurfaceGray,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = if (stall.isOpen) "Ver menú" else "Cerrado",
                            color = if (stall.isOpen) AdelitaCoral else AdelitaTextMuted,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StallDetailScreen(
    stallId: Long,
    customerViewModel: CustomerViewModel,
    onBack: () -> Unit,
    onOpenProduct: (Long) -> Unit,
    onOpenCart: () -> Unit
) {
    val detail by customerViewModel.selectedStallDetail.collectAsState()
    val isLoading by customerViewModel.isLoadingStallDetail.collectAsState()
    val error by customerViewModel.stallDetailError.collectAsState()
    val cart by customerViewModel.cart.collectAsState()
    val conflict by customerViewModel.pendingStallConflict.collectAsState()

    var selectedMenuCategory by remember { mutableStateOf("Todos") }

    LaunchedEffect(stallId) {
        customerViewModel.loadStallDetail(stallId)
    }

    StallConflictDialog(
        conflict = conflict,
        onConfirmReplace = { customerViewModel.confirmReplaceCartWithNewStall() },
        onDismiss = { customerViewModel.dismissStallConflict() }
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = detail?.stall?.name ?: "Menú del Puesto",
                        fontWeight = FontWeight.Bold,
                        color = AdelitaNavy,
                        fontSize = 18.sp
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
                    detail?.stall?.let { stall ->
                        IconButton(onClick = {
                            customerViewModel.toggleFavorite(stall.id, stall.isFavorite)
                        }) {
                            Icon(
                                imageVector = if (stall.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = "Favorito",
                                tint = if (stall.isFavorite) AdelitaCoral else AdelitaNavy
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        bottomBar = {
            if (!cart.isEmpty) {
                Surface(
                    color = Color.White,
                    shadowElevation = 8.dp
                ) {
                    Box(modifier = Modifier.padding(16.dp)) {
                        CoralPrimaryButton(
                            text = "Ver apartado (${cart.totalItemCount} productos • ${cart.formattedTotal})",
                            leadingIcon = Icons.Default.ShoppingBag,
                            onClick = onOpenCart
                        )
                    }
                }
            }
        }
    ) { padding ->
        when {
            isLoading && detail == null -> {
                Box(modifier = Modifier.padding(padding)) {
                    LoadingStateBox("Cargando menú del puesto...")
                }
            }
            error != null && detail == null -> {
                Box(modifier = Modifier.padding(padding)) {
                    ErrorStateCard(
                        message = error ?: "Error al cargar el puesto",
                        onRetry = { customerViewModel.loadStallDetail(stallId) }
                    )
                }
            }
            detail != null -> {
                val stall = detail!!.stall
                val products = detail!!.products
                val menuCategories = remember(products) {
                    listOf("Todos") + products.map { it.categoryName }.distinct()
                }
                val filteredProducts = remember(products, selectedMenuCategory) {
                    if (selectedMenuCategory == "Todos") products
                    else products.filter { it.categoryName.equals(selectedMenuCategory, ignoreCase = true) }
                }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.White)
                        .padding(padding),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    // Cabecera del puesto
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 8.dp),
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
                                        text = stall.name,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 18.sp,
                                        color = AdelitaNavy,
                                        modifier = Modifier.weight(1f)
                                    )
                                    GoldRatingChip(rating = stall.averageRating, count = stall.ratingCount)
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = stall.description,
                                    color = AdelitaTextDark,
                                    fontSize = 13.sp
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.LocationOn,
                                        contentDescription = null,
                                        tint = AdelitaCoral,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "${stall.campusLocation} • ${stall.prepTimeMinutes} min espera",
                                        color = AdelitaTextMuted,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                    }

                    // Chips de categorías del puesto
                    item {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(menuCategories) { cat ->
                                val selected = selectedMenuCategory == cat
                                FilterChip(
                                    selected = selected,
                                    onClick = { selectedMenuCategory = cat },
                                    label = {
                                        Text(
                                            text = cat,
                                            color = if (selected) Color.White else AdelitaTextDark,
                                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
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
                        }
                    }

                    // Encabezado "Menú" igual a Figma Pantalla 3
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Menú",
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = AdelitaTextDark
                            )
                            Text(
                                text = stall.name,
                                fontSize = 12.sp,
                                color = AdelitaTextMuted
                            )
                        }
                    }

                    if (filteredProducts.isEmpty()) {
                        item {
                            EmptyStateCard(
                                title = "Sin productos en esta categoría",
                                subtitle = "Prueba otra categoría del menú."
                            )
                        }
                    } else {
                        items(filteredProducts, key = { it.id }) { product ->
                            MenuProductRowCard(
                                product = product,
                                onOpenDetail = { onOpenProduct(product.id) },
                                onQuickAdd = { customerViewModel.addProductToCart(product, 1) }
                            )
                        }
                    }

                    // Reseñas recientes de otros estudiantes
                    if (detail!!.recentRatings.isNotEmpty()) {
                        item {
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Opiniones recientes de estudiantes",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = AdelitaNavy,
                                modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
                            )
                        }
                        items(detail!!.recentRatings.take(3), key = { it.id }) { rating ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp, vertical = 5.dp),
                                colors = CardDefaults.cardColors(containerColor = AdelitaSurfaceGray),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = rating.customerName,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 13.sp,
                                            color = AdelitaTextDark
                                        )
                                        GoldRatingChip(rating = rating.overallScore.toDouble())
                                    }
                                    if (!rating.comment.isNullOrBlank()) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "\"${rating.comment}\"",
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
}

@Composable
fun MenuProductRowCard(
    product: ProductDto,
    onOpenDetail: () -> Unit,
    onQuickAdd: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp)
            .clickable { onOpenDetail() },
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ProductVisualAvatar(imageKey = product.imageKey, size = 60.dp)
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = product.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = if (product.available) AdelitaTextDark else AdelitaTextMuted
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = product.description,
                    fontSize = 12.sp,
                    color = AdelitaTextMuted,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = product.formattedPrice,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 14.sp,
                        color = AdelitaNavy
                    )
                    if (!product.available) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            color = AdelitaCoralLight,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "No disponible",
                                color = AdelitaCoral,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            // Botón "+" coral igual al diseño de Figma
            Surface(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .clickable(enabled = product.available) { onQuickAdd() },
                color = if (product.available) AdelitaCoral else AdelitaSurfaceGray,
                shape = RoundedCornerShape(10.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Agregar ${product.name}",
                        tint = if (product.available) Color.White else AdelitaTextMuted,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductDetailScreen(
    productId: Long,
    customerViewModel: CustomerViewModel,
    onBack: () -> Unit,
    onAddedNavigateToCart: () -> Unit
) {
    val product by customerViewModel.selectedProduct.collectAsState()
    val isLoading by customerViewModel.isLoadingProduct.collectAsState()
    val error by customerViewModel.productError.collectAsState()
    val conflict by customerViewModel.pendingStallConflict.collectAsState()

    var quantity by remember { mutableIntStateOf(1) }

    LaunchedEffect(productId) {
        quantity = 1
        customerViewModel.loadProductDetail(productId)
    }

    StallConflictDialog(
        conflict = conflict,
        onConfirmReplace = {
            customerViewModel.confirmReplaceCartWithNewStall()
            onAddedNavigateToCart()
        },
        onDismiss = { customerViewModel.dismissStallConflict() }
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Detalle de Producto",
                        fontWeight = FontWeight.Bold,
                        color = AdelitaNavy,
                        fontSize = 18.sp
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
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        bottomBar = {
            product?.let { prod ->
                Surface(
                    color = Color.White,
                    shadowElevation = 8.dp
                ) {
                    Box(modifier = Modifier.padding(16.dp)) {
                        val lineTotal = formatMxnCents(prod.priceCents * quantity.toLong())
                        CoralPrimaryButton(
                            text = if (prod.available) "Agregar al apartado ($lineTotal)" else "Producto agotado",
                            enabled = prod.available,
                            leadingIcon = Icons.Default.AddShoppingCart,
                            onClick = {
                                customerViewModel.addProductToCart(prod, quantity)
                                if (customerViewModel.pendingStallConflict.value == null) {
                                    onAddedNavigateToCart()
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { padding ->
        when {
            isLoading && product == null -> {
                Box(modifier = Modifier.padding(padding)) {
                    LoadingStateBox("Cargando detalle del producto...")
                }
            }
            error != null && product == null -> {
                Box(modifier = Modifier.padding(padding)) {
                    ErrorStateCard(
                        message = error ?: "Error al cargar producto",
                        onRetry = { customerViewModel.loadProductDetail(productId) }
                    )
                }
            }
            product != null -> {
                val prod = product!!
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.White)
                        .padding(padding)
                        .verticalScroll(rememberScrollState())
                ) {
                    // Área superior de imagen de producto estilo Figma Pantalla 4
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(210.dp)
                            .background(AdelitaSurfaceGray),
                        contentAlignment = Alignment.Center
                    ) {
                        ProductVisualAvatar(imageKey = prod.imageKey, size = 110.dp)
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(22.dp)
                    ) {
                        Text(
                            text = prod.name,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 22.sp,
                            color = AdelitaNavy
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Storefront,
                                contentDescription = null,
                                tint = AdelitaCoral,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "${prod.stallName} • ${prod.campusLocation}",
                                color = AdelitaCoral,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = prod.formattedPrice,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 22.sp,
                                color = AdelitaNavy
                            )
                            GoldRatingChip(rating = prod.stallRating)
                        }

                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 16.dp),
                            color = AdelitaSurfaceGray
                        )

                        Text(
                            text = "Descripción",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = AdelitaTextDark
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = prod.description,
                            color = AdelitaTextMuted,
                            fontSize = 14.sp,
                            lineHeight = 20.sp
                        )

                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 18.dp),
                            color = AdelitaSurfaceGray
                        )

                        // Selector de Cantidad igual a Figma Pantalla 4
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = AdelitaSurfaceGray),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "Cantidad",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = AdelitaTextDark
                                    )
                                    Text(
                                        text = "Selecciona porciones",
                                        fontSize = 12.sp,
                                        color = AdelitaTextMuted
                                    )
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Surface(
                                        modifier = Modifier
                                            .size(34.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .clickable(enabled = quantity > 1) {
                                                if (quantity > 1) quantity--
                                            },
                                        color = Color.White,
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Default.Remove,
                                                contentDescription = "Disminuir cantidad",
                                                tint = AdelitaNavy
                                            )
                                        }
                                    }

                                    Text(
                                        text = quantity.toString(),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        color = AdelitaNavy
                                    )

                                    Surface(
                                        modifier = Modifier
                                            .size(34.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .clickable {
                                                if (quantity < 25) quantity++
                                            },
                                        color = AdelitaCoral,
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Default.Add,
                                                contentDescription = "Aumentar cantidad",
                                                tint = Color.White
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
}

@Composable
fun FloatingCartBanner(
    cart: CartState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() },
        color = AdelitaNavy,
        shape = RoundedCornerShape(14.dp),
        shadowElevation = 6.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    color = AdelitaCoral,
                    shape = CircleShape
                ) {
                    Text(
                        text = cart.totalItemCount.toString(),
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "Tu apartado en ${cart.stallName}",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Text(
                        text = "Pago en efectivo al recoger",
                        color = AdelitaGold,
                        fontSize = 11.sp
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = cart.formattedTotal,
                    color = Color.White,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 15.sp
                )
                Spacer(modifier = Modifier.width(6.dp))
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = Color.White
                )
            }
        }
    }
}
