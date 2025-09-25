package edu.usc.csci571.artsyapp.screens

import android.util.Log
import android.util.Patterns
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
import edu.usc.csci571.artsyapp.models.RegisterRequest
import kotlinx.coroutines.launch
import org.json.JSONObject
import androidx.compose.ui.text.withStyle
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.foundation.shape.RoundedCornerShape
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(navController: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val isDark = isSystemInDarkTheme()
    val appBarColor = if (isDark) Color(0xFF283C6C) else Color(0xFFDDE5FF)
    val backgroundColor = if (isDark) Color.Black else Color.White
    val textColor = if (isDark) Color.White else Color.Black
    val snackbarHostState = remember { SnackbarHostState() }

    var fullname by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var fullnameError by remember { mutableStateOf<String?>(null) }
    var emailError by remember { mutableStateOf<String?>(null) }
    var passwordError by remember { mutableStateOf<String?>(null) }
    var fullnameFieldFocused by remember { mutableStateOf(false) }
    var emailFieldFocused by remember { mutableStateOf(false) }
    var passwordFieldFocused by remember { mutableStateOf(false) }
    val fullnameInteractionSource = remember { MutableInteractionSource() }
    val emailInteractionSource = remember { MutableInteractionSource() }
    val passwordInteractionSource = remember { MutableInteractionSource() }
    var showSnackbar by remember { mutableStateOf(false) }
    var snackbarMessage by remember { mutableStateOf("") }

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
        fullnameError = if (fullname.isBlank()) "Full name is required" else null
        emailError = if (email.isBlank()) "Email is required" 
                    else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) "Invalid email format"
                    else null
        passwordError = if (password.isBlank()) "Password is required" else null
    }

    // Function to validate a specific field
    val validateField = { field: String ->
        when (field) {
            "fullname" -> fullnameError = if (fullname.isBlank()) "Full name is required" else null
            "email" -> emailError = if (email.isBlank()) "Email is required" 
                                  else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) "Invalid email format"
                                  else null
            "password" -> passwordError = if (password.isBlank()) "Password is required" else null
        }
    }

    if (showSnackbar) {
        Snackbar(
            modifier = Modifier.padding(16.dp),
            action = {
                TextButton(onClick = { showSnackbar = false }) {
                    Text("Dismiss")
                }
            }
        ) {
            Text(snackbarMessage)
        }
    }

    Scaffold(
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState) {
                Snackbar(
                    snackbarData = it,
                    containerColor = Color.Black,
                    contentColor = Color.White,
                    shape = RoundedCornerShape(4.dp)
                )
            }
        },
        topBar = {
            TopAppBar(
                title = { Text("Register", color = textColor) },
                navigationIcon = {
                    IconButton(onClick = { 
                        navController.navigate("home") {
                            popUpTo("register") { inclusive = true }
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
                value = fullname,
                onValueChange = { 
                    fullname = it
                    fullnameError = null 
                },
                label = { Text("Full name", color = if (fullnameError != null) Color.Red else if (fullnameInteractionSource.collectIsFocusedAsState().value) Color(0xFF90CAF9) else Color.Black) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Next
                ),
                keyboardActions = KeyboardActions(
                    onNext = { validateField("fullname") }
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = Color.Transparent,
                        shape = MaterialTheme.shapes.medium.copy(all = androidx.compose.foundation.shape.CornerSize(12.dp))
                    ),
                isError = fullnameError != null,
                colors = textFieldColors,
                shape = MaterialTheme.shapes.medium.copy(all = androidx.compose.foundation.shape.CornerSize(12.dp)),
                interactionSource = fullnameInteractionSource
            )
            if (fullnameError != null) {
                Text(
                    text = fullnameError!!,
                    color = Color.Red,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
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

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password", color = if (passwordError != null) Color.Red else if (passwordInteractionSource.collectIsFocusedAsState().value) Color(0xFF90CAF9) else Color.Black) },
                singleLine = true,
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
                interactionSource = passwordInteractionSource,
                visualTransformation = PasswordVisualTransformation()
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
                    if (fullnameError == null && emailError == null && passwordError == null) {
                        scope.launch {
                            isLoading = true
                            try {
                                val request = RegisterRequest(fullname, email, password)
                                val response = RetrofitClient.apiService.register(request)
                                val responseBodyStr = response.body()?.string() ?: response.errorBody()?.string() ?: ""

                                if (response.isSuccessful) {
                                    try {
                                        val json = JSONObject(responseBodyStr)
                                        val user = json.optJSONObject("user")
                                        val name = user?.optString("fullname", "User") ?: "User"
                                        val email = user?.optString("email", "") ?: ""
                                        val profileImageUrl = user?.optString("profileImageUrl")
                                            .takeIf { !it.isNullOrBlank() }
                                        
                                        // Create and update user session
                                        val newUser = edu.usc.csci571.artsyapp.session.User(
                                            id = email,
                                            fullName = name,
                                            email = email,
                                            profileImageUrl = profileImageUrl
                                        )
                                        edu.usc.csci571.artsyapp.session.UserSession.updateUser(newUser)
                                        
                                        // Load favorites and navigate
                                        scope.launch {
                                            // Load favorites first
                                            edu.usc.csci571.artsyapp.session.UserSession.loadFavorites()
                                            
                                            // Navigate to home with registration success flag
                                            navController.navigate("home?showRegistrationSuccess=true") {
                                                // Clear the entire back stack
                                                popUpTo(0) { inclusive = true }
                                            }
                                        }
                                    } catch (e: Exception) {
                                        emailError = "Registration failed. Please try again."
                                    }
                                } else {
                                    try {
                                        val errorJson = JSONObject(responseBodyStr)
                                        emailError = errorJson.optString("message", "Registration failed")
                                    } catch (e: Exception) {
                                        emailError = "Registration failed. Please try again."
                                    }
                                }
                            } catch (e: Exception) {
                                emailError = "Network error. Please try again."
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
                Text("Register")
            }

            Spacer(modifier = Modifier.height(16.dp))

            val annotatedText = buildAnnotatedString {
                append("Already have an account? ")
                pushStringAnnotation(tag = "login", annotation = "login")
                withStyle(SpanStyle(color = Color(0xFF0A1F44), textDecoration = TextDecoration.Underline)) {
                    append("Login")
                }
                pop()
            }

            ClickableText(
                text = annotatedText,
                onClick = { offset ->
                    annotatedText.getStringAnnotations(tag = "login", start = offset, end = offset)
                        .firstOrNull()?.let {
                            navController.navigate("login")
                        }
                }
            )
        }
    }
}
