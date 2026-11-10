package mx.edu.upchiapas.adelitagoat.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import mx.edu.upchiapas.adelitagoat.BuildConfig
import mx.edu.upchiapas.adelitagoat.domain.UserDto
import mx.edu.upchiapas.adelitagoat.domain.UserRole
import mx.edu.upchiapas.adelitagoat.ui.components.AdelitaBrandTitle
import mx.edu.upchiapas.adelitagoat.ui.components.AdelitaLogoBadge
import mx.edu.upchiapas.adelitagoat.ui.components.CoralPrimaryButton
import mx.edu.upchiapas.adelitagoat.ui.theme.*
import mx.edu.upchiapas.adelitagoat.ui.viewmodel.AuthViewModel

@Composable
fun WelcomeScreen(
    onStartClick: () -> Unit,
    onLoginClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 28.dp, vertical = 36.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Spacer(modifier = Modifier.height(32.dp))

            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                AdelitaLogoBadge(size = 88.dp, iconSize = 46.dp)
                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    text = "AdelitaGOAT",
                    color = AdelitaNavy,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Aparta tu comida, evita filas.",
                    color = AdelitaTextMuted,
                    fontSize = 15.sp,
                    textAlign = TextAlign.Center
                )
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CoralPrimaryButton(
                    text = "Comenzar  →",
                    onClick = onStartClick
                )
                Spacer(modifier = Modifier.height(18.dp))
                Text(
                    text = buildAnnotatedString {
                        withStyle(SpanStyle(color = AdelitaTextMuted)) {
                            append("¿Ya tienes cuenta? ")
                        }
                        withStyle(
                            SpanStyle(
                                color = AdelitaNavy,
                                fontWeight = FontWeight.Bold,
                                textDecoration = TextDecoration.Underline
                            )
                        ) {
                            append("Iniciar sesión")
                        }
                    },
                    fontSize = 14.sp,
                    modifier = Modifier
                        .clickable { onLoginClick() }
                        .padding(8.dp)
                )
            }
        }
    }
}

@Composable
fun AuthScreen(
    authViewModel: AuthViewModel,
    initialRegisterMode: Boolean = false,
    onAuthenticated: (UserDto) -> Unit
) {
    var isRegister by remember { mutableStateOf(initialRegisterMode) }
    var selectedRole by remember { mutableStateOf(UserRole.CLIENTE) }
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    val isLoading by authViewModel.isLoading.collectAsState()
    val errorMessage by authViewModel.errorMessage.collectAsState()

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AdelitaLogoBadge(size = 64.dp, iconSize = 34.dp)
            Spacer(modifier = Modifier.height(10.dp))
            AdelitaBrandTitle(fontSizeSp = 20)
            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = if (isRegister) "Crear cuenta en AdelitaGOAT" else "Bienvenido a AdelitaGOAT",
                color = AdelitaNavy,
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Puesto local frente a la Universidad Politécnica de Chiapas",
                color = AdelitaTextMuted,
                fontSize = 13.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Selector de Rol estilo Figma: "Soy Estudiante" vs "Vendedor del Puesto"
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(28.dp))
                    .background(AdelitaSurfaceGray)
                    .padding(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RoleSegmentButton(
                    title = "Soy Estudiante",
                    icon = Icons.Default.School,
                    selected = selectedRole == UserRole.CLIENTE,
                    modifier = Modifier.weight(1f),
                    onClick = {
                        selectedRole = UserRole.CLIENTE
                        authViewModel.clearError()
                    }
                )
                RoleSegmentButton(
                    title = "Vendedor del Puesto",
                    icon = Icons.Default.Storefront,
                    selected = selectedRole == UserRole.VENDEDOR,
                    modifier = Modifier.weight(1f),
                    onClick = {
                        selectedRole = UserRole.VENDEDOR
                        authViewModel.clearError()
                    }
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            if (errorMessage != null) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    colors = CardDefaults.cardColors(containerColor = AdelitaCoralLight),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.WarningAmber,
                            contentDescription = null,
                            tint = AdelitaCoral
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = errorMessage ?: "",
                            color = AdelitaTextDark,
                            fontSize = 13.sp
                        )
                    }
                }
            }

            if (isRegister) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Nombre completo",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp,
                        color = AdelitaTextDark
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = name,
                        onValueChange = {
                            name = it
                            authViewModel.clearError()
                        },
                        placeholder = { Text("Ej. Juan Pérez") },
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }

            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Correo electrónico",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp,
                    color = AdelitaTextDark
                )
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = email,
                    onValueChange = {
                        email = it
                        authViewModel.clearError()
                    },
                    placeholder = { Text("Correo") },
                    leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (isRegister) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Teléfono de contacto (opcional)",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp,
                        color = AdelitaTextDark
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        placeholder = { Text("961 123 4567") },
                        leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }

            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Contraseña",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp,
                    color = AdelitaTextDark
                )
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = password,
                    onValueChange = {
                        password = it
                        authViewModel.clearError()
                    },
                    placeholder = { Text("••••••••••••") },
                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = "Mostrar u ocultar contraseña"
                            )
                        }
                    },
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            CoralPrimaryButton(
                text = if (isRegister) "Crear cuenta  →" else "Iniciar sesión  →",
                loading = isLoading,
                onClick = {
                    if (isRegister) {
                        authViewModel.register(
                            name = name,
                            email = email,
                            password = password,
                            phone = phone,
                            role = selectedRole,
                            onSuccess = onAuthenticated
                        )
                    } else {
                        authViewModel.login(
                            email = email,
                            password = password,
                            onSuccess = onAuthenticated
                        )
                    }
                }
            )

            if (BuildConfig.ENABLE_DEMO_ACCOUNTS) {
                Spacer(modifier = Modifier.height(16.dp))

                // Acceso rápido a cuentas demo locales (visible únicamente cuando ENABLE_DEMO_ACCOUNTS=true)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = AdelitaSurfaceGray),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "Cuentas demo de desarrollo (.example.invalid):",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp,
                            color = AdelitaNavy
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    isRegister = false
                                    selectedRole = UserRole.CLIENTE
                                    email = "cliente1.demo@example.invalid"
                                    password = "Cliente123!"
                                    authViewModel.clearError()
                                },
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(vertical = 6.dp, horizontal = 8.dp)
                            ) {
                                Text("Cliente Demo", fontSize = 12.sp, color = AdelitaNavy)
                            }
                            OutlinedButton(
                                onClick = {
                                    isRegister = false
                                    selectedRole = UserRole.VENDEDOR
                                    email = "vendedor1.demo@example.invalid"
                                    password = "Vendedor123!"
                                    authViewModel.clearError()
                                },
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(vertical = 6.dp, horizontal = 8.dp)
                            ) {
                                Text("Vendedor Demo", fontSize = 12.sp, color = AdelitaNavy)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = buildAnnotatedString {
                    withStyle(SpanStyle(color = AdelitaTextMuted)) {
                        append(if (isRegister) "¿Ya tienes una cuenta? " else "¿No tienes una cuenta? ")
                    }
                    withStyle(SpanStyle(color = AdelitaCoral, fontWeight = FontWeight.Bold)) {
                        append(if (isRegister) "Inicia sesión aquí" else "Regístrate aquí")
                    }
                },
                fontSize = 14.sp,
                modifier = Modifier
                    .clickable {
                        isRegister = !isRegister
                        authViewModel.clearError()
                    }
                    .padding(8.dp)
            )
        }
    }
}

@Composable
private fun RoleSegmentButton(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .height(42.dp)
            .clip(RoundedCornerShape(24.dp))
            .clickable { onClick() },
        color = if (selected) AdelitaNavy else Color.Transparent,
        shape = RoundedCornerShape(24.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (selected) Color.White else AdelitaTextDark,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = title,
                color = if (selected) Color.White else AdelitaTextDark,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                fontSize = 12.sp
            )
        }
    }
}
