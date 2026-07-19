package com.example.vaultkey.screens

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Password
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.vaultkey.data.AppController
import com.example.vaultkey.data.SignupForm
import androidx.compose.foundation.verticalScroll

@Composable
fun SignupScreen(
    navController: NavHostController,
    appController: AppController
){
    var username: String by remember { mutableStateOf("") }
    var email: String by remember { mutableStateOf("") }
    var password: String by remember { mutableStateOf("") }
    var fullName: String by remember { mutableStateOf("") }
    var phoneNumber: String by remember { mutableStateOf("") }
    val focusManager = LocalFocusManager.current
    Scaffold(
        modifier = Modifier
            .pointerInput(Unit) {
                detectTapGestures {
                    focusManager.clearFocus()
                }
            }
    ) {innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .padding(innerPadding)
                .navigationBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "Create an Account",
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Black
            )
            Spacer(Modifier.height(40.dp))
            textFieldOut(
                username,
                "Username",
                Icons.Default.Person,
                { username = it }
            )
            Spacer(Modifier.height(25.dp))
            textFieldOut(
                fullName,
                "Full Name",
                Icons.Default.Person,
                { fullName = it }
            )
            Spacer(Modifier.height(25.dp))
            textFieldOut(
                email,
                "Email",
                Icons.Default.Email,
                { email = it }
            )
            Spacer(Modifier.height(25.dp))
            textFieldOut(
                phoneNumber,
                "Phone Number",
                Icons.Default.Phone,
                { phoneNumber = it },
                keyboardType = KeyboardType.Phone
            )
            Spacer(Modifier.height(25.dp))
            textFieldOut(
                password,
                "Password",
                Icons.Default.Lock,
                { password = it },
                isPassword = true
            )
            Spacer(Modifier.height(30.dp))
            Button(
                onClick = {
                    appController.signup(
                        SignupForm(
                            username = username,
                            fullName = fullName,
                            email = email,
                            phoneNumber = phoneNumber,
                            password = password
                        )
                    ) {
                        navController.navigate("Home") {
                            popUpTo("signup") { inclusive = true }
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth(),
                shape = RoundedCornerShape(15.dp),
                enabled = !appController.loading
            ) {
                if (appController.loading) {
                    CircularProgressIndicator(strokeWidth = 2.dp)
                } else {
                    Text(
                        "Signup",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
            appController.consumeMessage()?.let { message ->
                Spacer(Modifier.height(12.dp))
                Text(message, color = MaterialTheme.colorScheme.error)
            }
            if (appController.validationErrors.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    horizontalAlignment = Alignment.Start
                ) {
                    appController.validationErrors.forEach { error ->
                        Text(
                            text = error,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
            Spacer(Modifier.height(15.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End
            ) {
                Text(
                    "Already have an account?",
                    style = MaterialTheme.typography.titleMedium
                )
                TextButton(
                    onClick = { navController.popBackStack() }
                ) {
                    Text(
                        "Login",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}



@Composable
fun textFieldOut(
    values: String,
    placeholder: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onValueChange: (String) -> Unit,
    keyboardType: KeyboardType = KeyboardType.Text,
    capitalization: KeyboardCapitalization = KeyboardCapitalization.None,
    isPassword: Boolean = false
){
    var revealPassword by remember { mutableStateOf(false) }
    OutlinedTextField(
        value = values,
        onValueChange = onValueChange,
        placeholder = {
            Text(
                placeholder
            )
        },
        leadingIcon = {
            Icon(
                icon,
                contentDescription = null
            )
        },
        trailingIcon = if (isPassword) {
            {
                IconButton(onClick = { revealPassword = !revealPassword }) {
                    Icon(
                        imageVector = if (revealPassword) Icons.Default.Password else Icons.Default.Lock,
                        contentDescription = if (revealPassword) "Hide password" else "Show password"
                    )
                }
            }
        } else null,
        visualTransformation = if (isPassword && !revealPassword) PasswordVisualTransformation() else VisualTransformation.None,
        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
            keyboardType = if (isPassword) KeyboardType.Password else keyboardType,
            capitalization = capitalization
        ),
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(15.dp),
        singleLine = true
    )
}
