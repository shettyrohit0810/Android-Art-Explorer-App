package edu.usc.csci571.artsyapp.screens

import android.util.Log
import android.util.Patterns
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import edu.usc.csci571.artsyapp.network.RetrofitClient
import edu.usc.csci571.artsyapp.models.LoginRequest
import kotlinx.coroutines.launch
import org.json.JSONObject
import androidx.compose.ui.text.withStyle
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import edu.usc.csci571.artsyapp.session.User
import edu.usc.csci571.artsyapp.session.UserSession

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(navController: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val isDark = isSystemInDarkTheme()
    val appBarColor = if (isDark) Color(0xFF283C6C) else Color(0xFFDDE5FF)
    val backgroundColor = if (isDark) Color.Black else Color.White
    val textColor = if (isDark) Color.White else Color.Black

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var emailError by remember { mutableStateOf<String?>(null) }
    var passwordError by remember { mutableStateOf<String?>(null) }
    var loginError by remember { mutableStateOf<String?>(null) }
    var emailFieldFocused by remember { mutableStateOf(false) }
    var passwordFieldFocused by remember { mutableStateOf(false) }
    val emailInteractionSource = remember { MutableInteractionSource() }
    val passwordInteractionSource = remember { MutableInteractionSource() }

    val textFieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = Color(0xFF90CAF9),
        unfocusedBorderColor = Color(0xFF666666),
        errorBorderColor = Color.Red,
        focusedLabelColor = Color(0xFF90CAF9),
        unfocusedLabelColor = Color.Black,
        errorLabelColor = Color.Red,
        focusedTextColor = Color.Black,
        unfocusedTextColor = Color.Black,
        errorTextColor = Color.Black,
        cursorColor = Color(0xFF90CAF9)
    )

    // Function to validate fields
    val validateFields = {
        emailError = if (email.isBlank()) "Email cannot be empty" 
                    else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) "Invalid email format"
                    else null
        passwordError = if (password.isBlank()) "Password cannot be empty" else null
        loginError = null
    }

    // Function to validate a specific field
    val validateField = { field: String ->
        when (field) {
            "email" -> emailError = if (email.isBlank()) "Email cannot be empty" 
                                  else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) "Invalid email format"
                                  else null
            "password" -> passwordError = if (password.isBlank()) "Password cannot be empty" else null
        }
        loginError = null
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Login", color = textColor) },
                navigationIcon = {
                    IconButton(onClick = { 
                        navController.navigate("home") {
                            popUpTo("login") { inclusive = true }
                        }
                    }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = textColor)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = appBarColor,
                    navigationIconContentColor = textColor,
                    actionIconContentColor = textColor
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundColor)
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            OutlinedTextField(
                value = email,
                onValueChange = { 
                    email = it
                    emailError = null
                    loginError = null
                },
                label = { Text("Enter email", color = if (emailError != null) Color.Red else if (emailInteractionSource.collectIsFocusedAsState().value) Color(0xFF90CAF9) else Color.Black) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next
                ),
                keyboardActions = KeyboardActions(
                    onNext = { validateField("email") }
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = Color.Transparent,
                        shape = MaterialTheme.shapes.medium.copy(all = androidx.compose.foundation.shape.CornerSize(12.dp))
                    ),
                isError = emailError != null,
                colors = textFieldColors,
                shape = MaterialTheme.shapes.medium.copy(all = androidx.compose.foundation.shape.CornerSize(12.dp)),
                interactionSource = emailInteractionSource
            )
            if (emailError != null) {
                Text(
                    text = emailError!!,
                    color = Color.Red,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = password,
                onValueChange = { 
                    password = it
                    passwordError = null
                    loginError = null
                },
                label = { Text("Password", color = if (passwordError != null) Color.Red else if (passwordInteractionSource.collectIsFocusedAsState().value) Color(0xFF90CAF9) else Color.Black) },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = { validateField("password") }
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = Color.Transparent,
                        shape = MaterialTheme.shapes.medium.copy(all = androidx.compose.foundation.shape.CornerSize(12.dp))
                    ),
                isError = passwordError != null,
                colors = textFieldColors,
                shape = MaterialTheme.shapes.medium.copy(all = androidx.compose.foundation.shape.CornerSize(12.dp)),
                interactionSource = passwordInteractionSource
            )
            if (passwordError != null) {
                Text(
                    text = passwordError!!,
                    color = Color.Red,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    validateFields()
                    if (emailError == null && passwordError == null) {
                        scope.launch {
                            isLoading = true
                            try {
                                val response = RetrofitClient.apiService.login(LoginRequest(email, password))
                                val bodyString = response.body()?.string() ?: response.errorBody()?.string() ?: ""

                                if (response.isSuccessful) {
                                    val json = JSONObject(bodyString)
                                    val userJson = json.getJSONObject("user")

                                    val name = userJson.optString("fullname", "User")
                                    val emailAddr = userJson.optString("email", "")
                                    val avatarUrl = userJson.optString("profileImageUrl")
                                        .takeIf { it.isNotBlank() }

                                    val user = User(
                                        id = emailAddr,
                                        fullName = name,
                                        email = emailAddr,
                                        profileImageUrl = avatarUrl
                                    )
                                    
                                    UserSession.updateUser(user)
                                    UserSession.loadFavorites()

                                    Toast.makeText(context, "Welcome $name!", Toast.LENGTH_SHORT).show()
                                    navController.navigate("home") {
                                        popUpTo("login") { inclusive = true }
                                    }
                                } else {
                                    loginError = "Username or password is incorrect"
                                }
                            } catch (e: Exception) {
                                loginError = "Network error. Please try again."
                            } finally {
                                isLoading = false
                            }
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF34446C),
                    contentColor = Color.White,
                    disabledContainerColor = Color(0xFF34446C).copy(alpha = 0.5f)
                )
            ) {
                Text("Login")
            }

            if (loginError != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = loginError!!,
                    color = Color.Red,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            val annotatedText = buildAnnotatedString {
                append("Don't have an account yet? ")
                pushStringAnnotation(tag = "REGISTER", annotation = "register")
                withStyle(SpanStyle(color = Color(0xFF0A1F44), textDecoration = TextDecoration.Underline)) {
                    append("Register")
                }
                pop()
            }

            ClickableText(
                text = annotatedText,
                onClick = { offset ->
                    annotatedText.getStringAnnotations("REGISTER", offset, offset)
                        .firstOrNull()?.let {
                            navController.navigate("register")
                        }
                }
            )
        }
    }
}
