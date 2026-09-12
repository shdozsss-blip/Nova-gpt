package com.example.ui.screens.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.NovaStarLogo
import com.example.ui.theme.*
import com.example.ui.viewmodel.AdminViewModel

@Composable
fun AdminLoginScreen(
    adminViewModel: AdminViewModel,
    onLoginSuccess: () -> Unit,
    onBackToApp: () -> Unit,
    modifier: Modifier = Modifier
) {
    var username by remember { mutableStateOf("shahniar") }
    var password by remember { mutableStateOf("shahniar2026") }
    var passwordVisible by remember { mutableStateOf(false) }

    val loginError by adminViewModel.loginError.collectAsState()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBg)
            .padding(24.dp)
            .statusBarsPadding()
            .navigationBarsPadding(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth(0.9f)
        ) {
            NovaStarLogo(size = 72.dp)

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Nova",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = "Chatbot AI",
                fontSize = 12.sp,
                color = NovaPurple,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(28.dp))

            Text(
                text = "Admin Login",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Text(
                text = "Enter your credentials to access admin panel",
                fontSize = 13.sp,
                color = DarkTextSecondary,
                modifier = Modifier.padding(top = 4.dp, bottom = 24.dp)
            )

            // Username input
            OutlinedTextField(
                value = username,
                onValueChange = { username = it },
                label = { Text("Username") },
                leadingIcon = {
                    Icon(Icons.Outlined.Person, contentDescription = null, tint = NovaBlue)
                },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("admin_username_input"),
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = NovaBlue,
                    unfocusedBorderColor = DarkBorder,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    unfocusedContainerColor = DarkSurface,
                    focusedContainerColor = DarkSurface
                )
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Password input
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                leadingIcon = {
                    Icon(Icons.Outlined.Lock, contentDescription = null, tint = NovaBlue)
                },
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            imageVector = if (passwordVisible) Icons.Outlined.Visibility else Icons.Outlined.VisibilityOff,
                            contentDescription = "Toggle password visibility",
                            tint = DarkTextSecondary
                        )
                    }
                },
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = {
                    if (adminViewModel.login(username, password)) {
                        onLoginSuccess()
                    }
                }),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("admin_password_input"),
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = NovaBlue,
                    unfocusedBorderColor = DarkBorder,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    unfocusedContainerColor = DarkSurface,
                    focusedContainerColor = DarkSurface
                )
            )

            if (loginError != null) {
                Text(
                    text = loginError!!,
                    color = StatusRed,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Login Button
            Button(
                onClick = {
                    if (adminViewModel.login(username, password)) {
                        onLoginSuccess()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .clip(RoundedCornerShape(26.dp))
                    .testTag("admin_login_button"),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                contentPadding = PaddingValues()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(NovaButtonGradient),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Login",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Back to App
            TextButton(
                onClick = onBackToApp,
                modifier = Modifier.testTag("admin_back_button")
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = DarkTextSecondary, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Back to App", color = DarkTextSecondary, fontSize = 14.sp)
                }
            }
        }
    }
}
