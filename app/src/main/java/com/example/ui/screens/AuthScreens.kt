package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.R
import com.example.ui.components.SpleexWordmark
import com.example.ui.theme.SpleexPinkGlow
import com.example.ui.theme.SpleexPinkPrimary
import com.example.ui.theme.SpleexVioletAccent
import com.example.ui.viewmodel.AuthUiState

private enum class AuthTab { LOGIN, REGISTER, FORGOT }

@Composable
fun AuthFlowScreen(
    uiState: AuthUiState,
    hasSeenOnboarding: Boolean,
    onDismissOnboarding: () -> Unit,
    onLogin: (String, String) -> Unit,
    onRegister: (String, String, String, String, String, Uri?) -> Unit,
    onResetPassword: (String, String, String, () -> Unit) -> Unit,
    onDemoLogin: () -> Unit,
    onClearMessages: () -> Unit
) {
    if (!hasSeenOnboarding) {
        OnboardingScreen(onGetStarted = onDismissOnboarding)
        return
    }

    var activeTab by remember { mutableStateOf(AuthTab.LOGIN) }

    // Login state
    var loginIdentifier by remember { mutableStateOf("") }
    var loginPassword by remember { mutableStateOf("") }
    var showLoginPassword by remember { mutableStateOf(false) }

    // Register state
    var fullName by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var regPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var selectedAvatarUri by remember { mutableStateOf<Uri?>(null) }

    // Forgot password state
    var resetEmail by remember { mutableStateOf("") }
    var resetNewPass by remember { mutableStateOf("") }
    var resetConfirmPass by remember { mutableStateOf("") }

    val avatarPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) selectedAvatarUri = uri
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        // Editorial Banner Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(190.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.img_auth_banner),
                contentDescription = "SPLEEX hero artwork",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color.Black.copy(alpha = 0.25f),
                                MaterialTheme.colorScheme.background
                            )
                        )
                    )
            )
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(horizontal = 20.dp, vertical = 14.dp)
            ) {
                SpleexWordmark()
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Connect. Share. Discover.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 540.dp)
                .align(Alignment.CenterHorizontally)
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Segmented Auth Switch
            if (activeTab != AuthTab.FORGOT) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(4.dp)
                    ) {
                        AuthTabButton(
                            label = "Sign In",
                            selected = activeTab == AuthTab.LOGIN,
                            onClick = {
                                onClearMessages()
                                activeTab = AuthTab.LOGIN
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("tab_login")
                        )
                        AuthTabButton(
                            label = "Create Account",
                            selected = activeTab == AuthTab.REGISTER,
                            onClick = {
                                onClearMessages()
                                activeTab = AuthTab.REGISTER
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("tab_register")
                        )
                    }
                }
            }

            AnimatedVisibility(visible = uiState.errorMessage != null) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.error.copy(alpha = 0.14f)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Filled.ErrorOutline,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = uiState.errorMessage.orEmpty(),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            AnimatedVisibility(visible = uiState.successMessage != null) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        text = uiState.successMessage.orEmpty(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            when (activeTab) {
                AuthTab.LOGIN -> {
                    OutlinedTextField(
                        value = loginIdentifier,
                        onValueChange = { loginIdentifier = it },
                        label = { Text("Email or Username") },
                        leadingIcon = { Icon(Icons.Outlined.AlternateEmail, contentDescription = null) },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("login_identifier_input")
                    )

                    OutlinedTextField(
                        value = loginPassword,
                        onValueChange = { loginPassword = it },
                        label = { Text("Password") },
                        leadingIcon = { Icon(Icons.Outlined.Lock, contentDescription = null) },
                        trailingIcon = {
                            IconButton(onClick = { showLoginPassword = !showLoginPassword }) {
                                Icon(
                                    if (showLoginPassword) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                                    contentDescription = "Toggle password visibility"
                                )
                            }
                        },
                        visualTransformation = if (showLoginPassword) VisualTransformation.None else PasswordVisualTransformation(),
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("login_password_input")
                    )

                    TextButton(
                        onClick = {
                            onClearMessages()
                            activeTab = AuthTab.FORGOT
                        },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("Forgot password?")
                    }

                    Button(
                        onClick = { onLogin(loginIdentifier, loginPassword) },
                        enabled = !uiState.isLoading,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("login_submit_button")
                    ) {
                        if (uiState.isLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                        } else {
                            Text("Sign In to SPLEEX", style = MaterialTheme.typography.titleMedium)
                        }
                    }
                }

                AuthTab.REGISTER -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .border(
                                    2.dp,
                                    Brush.linearGradient(listOf(SpleexPinkPrimary, SpleexVioletAccent)),
                                    CircleShape
                                )
                                .clickable {
                                    avatarPicker.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                }
                                .testTag("register_avatar_picker"),
                            contentAlignment = Alignment.Center
                        ) {
                            if (selectedAvatarUri != null) {
                                AsyncImage(
                                    model = selectedAvatarUri,
                                    contentDescription = "Selected profile picture",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Icon(
                                    Icons.Outlined.AddAPhoto,
                                    contentDescription = "Upload profile photo",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text("Profile Picture (Optional)", style = MaterialTheme.typography.titleSmall)
                            Text(
                                "Tap to pick a photo from your device",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    OutlinedTextField(
                        value = fullName,
                        onValueChange = { fullName = it },
                        label = { Text("Full Name") },
                        leadingIcon = { Icon(Icons.Outlined.Person, contentDescription = null) },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("register_fullname_input")
                    )

                    OutlinedTextField(
                        value = username,
                        onValueChange = { username = it },
                        label = { Text("Unique Username") },
                        prefix = { Text("@") },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("register_username_input")
                    )

                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Email Address") },
                        leadingIcon = { Icon(Icons.Outlined.Email, contentDescription = null) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("register_email_input")
                    )

                    OutlinedTextField(
                        value = regPassword,
                        onValueChange = { regPassword = it },
                        label = { Text("Password (6+ characters)") },
                        leadingIcon = { Icon(Icons.Outlined.Lock, contentDescription = null) },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("register_password_input")
                    )

                    OutlinedTextField(
                        value = confirmPassword,
                        onValueChange = { confirmPassword = it },
                        label = { Text("Confirm Password") },
                        leadingIcon = { Icon(Icons.Outlined.VerifiedUser, contentDescription = null) },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("register_confirm_password_input")
                    )

                    Button(
                        onClick = {
                            onRegister(
                                fullName,
                                username,
                                email,
                                regPassword,
                                confirmPassword,
                                selectedAvatarUri
                            )
                        },
                        enabled = !uiState.isLoading,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("register_submit_button")
                    ) {
                        if (uiState.isLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                        } else {
                            Text("Create SPLEEX Account", style = MaterialTheme.typography.titleMedium)
                        }
                    }
                }

                AuthTab.FORGOT -> {
                    Text(
                        text = "Reset Password",
                        style = MaterialTheme.typography.headlineSmall
                    )
                    Text(
                        text = "Enter your registered email and choose a fresh password.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = resetEmail,
                        onValueChange = { resetEmail = it },
                        label = { Text("Registered Email") },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = resetNewPass,
                        onValueChange = { resetNewPass = it },
                        label = { Text("New Password") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = resetConfirmPass,
                        onValueChange = { resetConfirmPass = it },
                        label = { Text("Confirm New Password") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Button(
                        onClick = {
                            onResetPassword(resetEmail, resetNewPass, resetConfirmPass) {
                                activeTab = AuthTab.LOGIN
                            }
                        },
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                    ) {
                        Text("Update Password")
                    }
                    TextButton(
                        onClick = {
                            onClearMessages()
                            activeTab = AuthTab.LOGIN
                        },
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    ) {
                        Text("Back to Sign In")
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            OutlinedButton(
                onClick = onDemoLogin,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("demo_login_button")
            ) {
                Icon(Icons.Filled.Bolt, contentDescription = null, tint = SpleexPinkGlow)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Instant Preview as @aria_vibe")
            }

            Text(
                text = "Local on-device database active — all profiles, posts, photos, stories, reels, and chats persist across sessions.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp)
            )
        }
    }
}

@Composable
private fun AuthTabButton(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
        modifier = modifier.height(44.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun OnboardingScreen(onGetStarted: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Image(
            painter = painterResource(id = R.drawable.img_auth_banner),
            contentDescription = "SPLEEX splash art",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color.Black.copy(alpha = 0.25f),
                            Color(0xFF0B0A0F).copy(alpha = 0.92f)
                        )
                    )
                )
        )
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            SpleexWordmark()
            Text(
                text = "Share moments in high contrast.",
                style = MaterialTheme.typography.displaySmall,
                color = Color.White
            )
            Text(
                text = "Discover creators, drop multi-photo posts, vote in polls, react to 24h stories, watch reels, and message in real time.",
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White.copy(alpha = 0.8f)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Button(
                onClick = onGetStarted,
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("onboarding_start_button")
            ) {
                Text("Enter SPLEEX", style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}
