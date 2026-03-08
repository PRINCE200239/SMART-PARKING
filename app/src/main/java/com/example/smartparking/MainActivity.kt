package com.example.smartparking

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import kotlin.random.Random

// --- DATA MODELS ---
data class Booking(
    val slot: String,
    val date: String,
    val time: String,
    var paymentStatus: String
)

data class User(
    val email: String,
    val password: String,
    val name: String,
    val phone: String,
    val carModel: String,
    val plateNo: String
)

// --- COLORS ---
val CoolBlueGradient = Brush.verticalGradient(listOf(Color(0xFF2C3E50), Color(0xFF4CA1AF)))
val PleasantBannerColor = Color(0xFF34495E)
val PrimaryNavy = Color(0xFF0D365A)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            var currentScreen by remember { mutableStateOf("loading") }
            val bookingList = remember { mutableStateListOf<Booking>() }
            var lastAssignedSlot by remember { mutableStateOf("") }
            var showCheckBookingDialog by remember { mutableStateOf(false) }

            val totalSlots = 100
            val remainingSlots = totalSlots - bookingList.size

            val userDatabase = remember {
                mutableStateListOf(User("test@parking.com", "123456", "Rahul", "+91 9999999999", "SUV", "DL-01-0001"))
            }
            var loggedInUser by remember { mutableStateOf<User?>(null) }

            LaunchedEffect(Unit) {
                delay(2000)
                currentScreen = "login"
            }

            // --- UPGRADED INTERACTIVE CHECK BOOKING DIALOG ---
            if (showCheckBookingDialog) {
                AlertDialog(
                    onDismissRequest = { showCheckBookingDialog = false },
                    confirmButton = {
                        TextButton(onClick = { showCheckBookingDialog = false }) {
                            Text("Dismiss", color = PleasantBannerColor)
                        }
                    },
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.List, contentDescription = null, tint = PleasantBannerColor)
                            Spacer(Modifier.width(8.dp))
                            Text("Active Bookings", fontWeight = FontWeight.Bold)
                        }
                    },
                    text = {
                        if (bookingList.isEmpty()) {
                            Box(Modifier.fillMaxWidth().padding(vertical = 20.dp), contentAlignment = Alignment.Center) {
                                Text("No active booking found.", color = Color.Gray)
                            }
                        } else {
                            LazyColumn(modifier = Modifier.heightIn(max = 400.dp)) {
                                items(bookingList) { booking ->
                                    UpgradedBookingItem(
                                        booking = booking,
                                        onPay = {
                                            showCheckBookingDialog = false
                                            currentScreen = "payment_page"
                                        },
                                        onCancel = {
                                            bookingList.remove(booking)
                                        }
                                    )
                                }
                            }
                        }
                    }
                )
            }

            Crossfade(targetState = currentScreen, label = "screen_transition") { screen ->
                when (screen) {
                    "loading" -> LoadingPage()
                    "login" -> AuthPage(
                        userDatabase = userDatabase,
                        onAuthSuccess = { user ->
                            loggedInUser = user
                            currentScreen = "main"
                        }
                    )
                    "main" -> MainDashboard(
                        user = loggedInUser ?: User("", "", "", "", "", ""),
                        allotedCount = bookingList.size,
                        remainingCount = remainingSlots,
                        onSignOff = {
                            loggedInUser = null
                            currentScreen = "login"
                        },
                        onAdvanceBooking = { currentScreen = "booking_form" },
                        onCheckBooking = { showCheckBookingDialog = true },
                        onPaymentClick = { currentScreen = "payment_page" }
                    )
                    "booking_form" -> AdvanceBookingForm(
                        onBack = { currentScreen = "main" },
                        onConfirm = { slot, date, time, status ->
                            bookingList.add(Booking(slot, date, time, status))
                            lastAssignedSlot = slot
                            currentScreen = "success"
                        }
                    )
                    "payment_page" -> PaymentPage(
                        onBack = { currentScreen = "main" },
                        onSuccess = {
                            bookingList.forEachIndexed { index, b ->
                                if (b.paymentStatus == "Pending") bookingList[index] = b.copy(paymentStatus = "Paid")
                            }
                            currentScreen = "main"
                        }
                    )
                    "success" -> SuccessScreen(lastAssignedSlot) { currentScreen = "main" }
                }
            }
        }
    }
}

// --- NEW UPGRADED BOOKING ITEM COMPONENT ---
@Composable
fun UpgradedBookingItem(booking: Booking, onPay: () -> Unit, onCancel: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8F9FA)),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("Slot: ${booking.slot}", fontWeight = FontWeight.ExtraBold, color = Color(0xFFFB8C00), fontSize = 18.sp)
                    Text("📅 ${booking.date}", fontSize = 12.sp, color = Color.DarkGray)
                    Text("⏰ ${booking.time}", fontSize = 12.sp, color = Color.DarkGray)
                }
                // Status Badge
                val isPaid = booking.paymentStatus == "Paid" || booking.paymentStatus == "Cash"
                Surface(
                    color = if (isPaid) Color(0xFFE8F5E9) else Color(0xFFFFEBEE),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = booking.paymentStatus,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        color = if (isPaid) Color(0xFF2E7D32) else Color(0xFFC62828),
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                // Cancel Button
                OutlinedButton(
                    onClick = onCancel,
                    modifier = Modifier.height(36.dp),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, Color.LightGray),
                    contentPadding = PaddingValues(horizontal = 12.dp)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.Gray)
                    Spacer(Modifier.width(4.dp))
                    Text("Cancel", fontSize = 12.sp, color = Color.Gray)
                }

                if (booking.paymentStatus == "Pending") {
                    Spacer(Modifier.width(8.dp))
                    // Pay Now Button
                    Button(
                        onClick = onPay,
                        modifier = Modifier.height(36.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                        contentPadding = PaddingValues(horizontal = 12.dp)
                    ) {
                        Icon(Icons.Default.ShoppingCart, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Pay Now", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

// --- REST OF THE CODE REMAINS UNTOUCHED AS REQUESTED ---

@Composable
fun AuthPage(userDatabase: MutableList<User>, onAuthSuccess: (User) -> Unit) {
    var isLoginMode by remember { mutableStateOf(true) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var carModel by remember { mutableStateOf("") }
    var plateNo by remember { mutableStateOf("") }

    var passwordVisible by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    val accentOrange = Color(0xFFFB8C00)

    Column(
        modifier = Modifier.fillMaxSize().background(Color(0xFFF5F5F5)).padding(24.dp).verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center
    ) {
        Text(text = if (isLoginMode) "Log In" else "Sign Up", color = PleasantBannerColor, fontSize = 40.sp, fontWeight = FontWeight.ExtraBold)
        Text(text = "Smart Parking System", color = Color.Gray, fontSize = 18.sp, modifier = Modifier.padding(bottom = 32.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(8.dp)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                if (!isLoginMode) {
                    CustomAuthTextField(value = name, onValueChange = { name = it }, label = "Full Name", placeholder = "John Doe")
                    CustomAuthTextField(value = phone, onValueChange = { phone = it }, label = "Phone Number", placeholder = "+91 00000 00000")
                    CustomAuthTextField(value = carModel, onValueChange = { carModel = it }, label = "Car Model", placeholder = "e.g., Honda City")
                    CustomAuthTextField(value = plateNo, onValueChange = { plateNo = it }, label = "License Plate", placeholder = "ABC-1234")
                }

                CustomAuthTextField(value = email, onValueChange = { email = it }, label = "Email Address", placeholder = "example@mail.com")

                Text("Password", fontWeight = FontWeight.Bold, color = PleasantBannerColor, fontSize = 14.sp)
                OutlinedTextField(
                    value = password, onValueChange = { password = it },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    shape = RoundedCornerShape(12.dp),
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(imageVector = if (passwordVisible) Icons.Default.Info else Icons.Default.Lock, contentDescription = null, tint = PleasantBannerColor)
                        }
                    }
                )

                if (errorMessage.isNotEmpty()) {
                    Text(errorMessage, color = Color.Red, fontSize = 14.sp, modifier = Modifier.padding(vertical = 8.dp))
                }

                Spacer(Modifier.height(16.dp))

                Button(
                    onClick = {
                        if (isLoginMode) {
                            val user = userDatabase.find { it.email == email && it.password == password }
                            if (user != null) onAuthSuccess(user) else errorMessage = "Invalid Email or Password"
                        } else {
                            if (email.isNotEmpty() && password.length >= 6 && name.isNotEmpty() && phone.isNotEmpty() && carModel.isNotEmpty() && plateNo.isNotEmpty()) {
                                val newUser = User(email, password, name, phone, carModel, plateNo)
                                userDatabase.add(newUser)
                                onAuthSuccess(newUser)
                            } else errorMessage = "Please fill all details (Password min 6 chars)"
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(60.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PleasantBannerColor),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(if (isLoginMode) "SIGN IN" else "CREATE ACCOUNT", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color.White)
                }
            }
        }

        Spacer(Modifier.height(24.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(if (isLoginMode) "New to the app?" else "Already have an account?")
            TextButton(onClick = { isLoginMode = !isLoginMode; errorMessage = "" }) {
                Text(if (isLoginMode) "Register Now" else "Log In", color = accentOrange, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
            }
        }
    }
}

@Composable
fun CustomAuthTextField(value: String, onValueChange: (String) -> Unit, label: String, placeholder: String) {
    Text(label, fontWeight = FontWeight.Bold, color = PleasantBannerColor, fontSize = 14.sp)
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text(placeholder, fontSize = 13.sp) },
        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
        shape = RoundedCornerShape(12.dp)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainDashboard(
    user: User,
    allotedCount: Int,
    remainingCount: Int,
    onSignOff: () -> Unit,
    onAdvanceBooking: () -> Unit,
    onCheckBooking: () -> Unit,
    onPaymentClick: () -> Unit
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    var currentTime by remember { mutableStateOf(SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())) }
    var currentDate by remember { mutableStateOf(SimpleDateFormat("EEE, dd MMM yyyy", Locale.getDefault()).format(Date())) }

    LaunchedEffect(Unit) {
        while (true) {
            currentTime = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
            currentDate = SimpleDateFormat("EEE, dd MMM yyyy", Locale.getDefault()).format(Date())
            delay(1000)
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = Color.White,
                modifier = Modifier.width(300.dp)
            ) {
                ProfileDrawerContent(user, onSignOff) { scope.launch { drawerState.close() } }
            }
        }
    ) {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Text("SMART PARKING", letterSpacing = 2.sp, fontWeight = FontWeight.Black, color = Color.White)
                    },
                    actions = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.AccountCircle, "Profile", tint = Color.White, modifier = Modifier.size(32.dp))
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = PleasantBannerColor)
                )
            }
        ) { padding ->
            Column(modifier = Modifier.padding(padding).fillMaxSize().background(CoolBlueGradient).verticalScroll(rememberScrollState())) {
                Column(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
                    Text(text = "Hello, ${user.name}!", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Spacer(Modifier.height(8.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Card(modifier = Modifier.weight(1f), colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.15f)), shape = RoundedCornerShape(12.dp)) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.DateRange, null, tint = Color.White, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text(text = "Time", fontSize = 12.sp, color = Color.White.copy(0.7f))
                                }
                                Text(text = currentTime, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                                Text(text = currentDate, fontSize = 11.sp, color = Color.White.copy(0.8f))
                            }
                        }

                        Card(modifier = Modifier.weight(1f), colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.2f)), shape = RoundedCornerShape(12.dp)) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Place, null, tint = Color.White, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text(text = "Status", fontSize = 12.sp, color = Color.White.copy(0.7f))
                                }
                                Text(text = "Full: $allotedCount", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFFB74D))
                                Text(text = "Free: $remainingCount", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF81C784))
                            }
                        }
                    }
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp)
                        .padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(24.dp),
                    elevation = CardDefaults.cardElevation(6.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.map_graphic),
                        contentDescription = "Parking Location Map",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                Column(modifier = Modifier.padding(16.dp)) {
                    DashboardCard("Advance Booking", Color(0xFF81C784), icon = Icons.Default.DateRange, onClick = onAdvanceBooking)
                    DashboardCard("Check Booking", Color(0xFF4DB6AC), icon = Icons.Default.List, onClick = onCheckBooking)
                    DashboardCard("Booking Payment", Color(0xFFFFB74D), icon = Icons.Default.ShoppingCart, onClick = onPaymentClick)
                }
            }
        }
    }
}

@Composable
fun ProfileDrawerContent(user: User, onSignOff: () -> Unit, onClose: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().padding(20.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Text("User Profile", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = PleasantBannerColor)
            Spacer(Modifier.weight(1f))
            IconButton(onClick = onClose) { Icon(Icons.Default.Close, null, tint = Color.Gray) }
        }

        Spacer(Modifier.height(24.dp))

        Box(modifier = Modifier.size(70.dp).background(PleasantBannerColor, CircleShape), contentAlignment = Alignment.Center) {
            Icon(Icons.Default.Person, null, modifier = Modifier.size(40.dp), tint = Color.White)
        }

        Spacer(Modifier.height(12.dp))
        Text(user.name, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
        Text(user.email, color = Color.Gray, fontSize = 14.sp)

        HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp), thickness = 0.5.dp)

        ProfileInfoRow(Icons.Default.Call, "Phone", user.phone)
        ProfileInfoRow(Icons.Default.Settings, "Car Details", user.carModel)
        ProfileInfoRow(Icons.Default.Info, "Plate Number", user.plateNo)

        Spacer(Modifier.weight(1f))

        OutlinedButton(
            onClick = { /* Action */ },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp)
        ) {
            Icon(Icons.Default.Lock, null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Change Password")
        }

        Spacer(Modifier.height(8.dp))

        Button(
            onClick = onSignOff,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F)),
            shape = RoundedCornerShape(8.dp)
        ) {
            Icon(Icons.Default.ExitToApp, null, tint = Color.White)
            Spacer(Modifier.width(8.dp))
            Text("Sign Off", color = Color.White)
        }
    }
}

@Composable
fun ProfileInfoRow(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = PleasantBannerColor, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(12.dp))
        Column {
            Text(label, fontSize = 11.sp, color = Color.Gray)
            Text(value, fontSize = 15.sp, fontWeight = FontWeight.Medium)
        }
    }
}

// NOTE: This old version is replaced by UpgradedBookingItem in the Dialog logic above.
@Composable
fun BookingItemRow(booking: Booking, onPay: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.9f))) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Slot: ${booking.slot}", fontWeight = FontWeight.Bold, color = Color(0xFFFB8C00))
                Text("Date: ${booking.date}", fontSize = 14.sp)
                val statusColor = if(booking.paymentStatus == "Paid") Color(0xFF4CAF50) else Color.Red
                Text("Status: ${booking.paymentStatus}", color = statusColor, fontWeight = FontWeight.Bold)
            }
            if (booking.paymentStatus == "Pending") {
                IconButton(onClick = onPay) { Icon(Icons.Default.ShoppingCart, null, tint = PleasantBannerColor) }
            }
        }
    }
}

@Composable
fun LoadingPage() {
    Column(modifier = Modifier.fillMaxSize().background(Color.White), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Image(painter = painterResource(id = R.drawable.parking_logo), contentDescription = null, modifier = Modifier.size(250.dp))
        Text("SMART PARKING", color = PleasantBannerColor, fontSize = 28.sp, fontWeight = FontWeight.Bold)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdvanceBookingForm(onBack: () -> Unit, onConfirm: (String, String, String, String) -> Unit) {
    val context = LocalContext.current
    val calendar = Calendar.getInstance()

    var dateInput by remember { mutableStateOf(SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(calendar.time)) }
    var timeIn by remember { mutableStateOf("10:00") }
    var timeOut by remember { mutableStateOf("12:00") }
    var selectedPayment by remember { mutableStateOf("Cash") }

    val datePickerDialog = DatePickerDialog(
        context, { _, y, m, d -> dateInput = "$d/${m + 1}/$y" },
        calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)
    ).apply {
        datePicker.minDate = System.currentTimeMillis() - 1000
    }

    fun showTimePicker(isEntry: Boolean) {
        TimePickerDialog(context, { _, hour, min ->
            val formatted = String.format("%02d:%02d", hour, min)
            if (isEntry) timeIn = formatted else timeOut = formatted
        }, 10, 0, true).show()
    }

    Scaffold(topBar = { TopAppBar(title = { Text("Plan Your Parking", color = Color.White) }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null, tint = Color.White) } }, colors = TopAppBarDefaults.topAppBarColors(containerColor = PleasantBannerColor)) }) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize().background(CoolBlueGradient).padding(16.dp).verticalScroll(rememberScrollState())) {
            Text("Reservation Details", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 12.dp))
            Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.95f))) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("Select Date", fontWeight = FontWeight.Bold, color = PleasantBannerColor)
                    OutlinedButton(onClick = { datePickerDialog.show() }, modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), shape = RoundedCornerShape(12.dp)) {
                        Icon(Icons.Default.DateRange, null)
                        Spacer(Modifier.width(8.dp))
                        Text(dateInput)
                    }
                    Spacer(Modifier.height(16.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Time In", fontWeight = FontWeight.Bold, color = PleasantBannerColor)
                            OutlinedButton(onClick = { showTimePicker(true) }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) { Text(timeIn) }
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Time Out", fontWeight = FontWeight.Bold, color = PleasantBannerColor)
                            OutlinedButton(onClick = { showTimePicker(false) }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) { Text(timeOut) }
                        }
                    }
                    Spacer(Modifier.height(24.dp))
                    Box(modifier = Modifier.fillMaxWidth().background(Color(0xFFE8F5E9), RoundedCornerShape(12.dp)).padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Info, null, tint = Color(0xFF2E7D32))
                            Spacer(Modifier.width(12.dp))
                            Text("Booking for $dateInput\n$timeIn to $timeOut", fontSize = 14.sp, color = Color(0xFF1B5E20))
                        }
                    }
                    Spacer(Modifier.height(24.dp))
                    Text("Payment Method", fontWeight = FontWeight.Bold, color = PleasantBannerColor)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = selectedPayment == "Cash", onClick = { selectedPayment = "Cash" })
                        Text("Cash")
                        Spacer(Modifier.width(16.dp))
                        RadioButton(selected = selectedPayment == "Online", onClick = { selectedPayment = "Online" })
                        Text("Online")
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
            Button(onClick = {
                val slot = "${('A'..'E').random()}-${Random.nextInt(1, 99)}"
                onConfirm(slot, dateInput, "$timeIn - $timeOut", if(selectedPayment == "Cash") "Cash" else "Pending")
            }, modifier = Modifier.fillMaxWidth().height(60.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFB8C00)), shape = RoundedCornerShape(16.dp)) {
                Text("CONFIRM RESERVATION", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentPage(onBack: () -> Unit, onSuccess: () -> Unit) {
    var selectedTab by remember { mutableStateOf(0) }
    var cardNumber by remember { mutableStateOf("") }
    var expiryDate by remember { mutableStateOf("") }
    var cvv by remember { mutableStateOf("") }
    var cardHolder by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Checkout", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(Color(0xFFF8F9FA))
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // --- Method Selector ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFE9ECEF), RoundedCornerShape(12.dp))
                    .padding(4.dp)
            ) {
                PaymentTabItem(
                    title = "Card",
                    isSelected = selectedTab == 0,
                    icon = Icons.Default.Add, // Using Add as a placeholder for CreditCard icon
                    modifier = Modifier.weight(1f)
                ) { selectedTab = 0 }

                PaymentTabItem(
                    title = "UPI QR",
                    isSelected = selectedTab == 1,
                    icon = Icons.Default.Share, // Using Share as a placeholder for QR icon
                    modifier = Modifier.weight(1f)
                ) { selectedTab = 1 }
            }

            Spacer(Modifier.height(24.dp))

            if (selectedTab == 0) {
                // --- ADVANCED CARD UI ---
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A))
                ) {
                    Column(Modifier.padding(24.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Credit Card", color = Color.White.copy(0.7f), fontSize = 14.sp)
                            Icon(Icons.Default.Star, null, tint = Color(0xFFFFD700))
                        }
                        Spacer(Modifier.weight(1f))
                        Text(
                            text = if (cardNumber.isEmpty()) "XXXX XXXX XXXX XXXX" else cardNumber,
                            color = Color.White,
                            fontSize = 22.sp,
                            letterSpacing = 2.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.weight(1f))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column {
                                Text("CARD HOLDER", color = Color.White.copy(0.5f), fontSize = 10.sp)
                                Text(if (cardHolder.isEmpty()) "YOUR NAME" else cardHolder.uppercase(), color = Color.White, fontSize = 14.sp)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("EXPIRES", color = Color.White.copy(0.5f), fontSize = 10.sp)
                                Text(if (expiryDate.isEmpty()) "MM/YY" else expiryDate, color = Color.White, fontSize = 14.sp)
                            }
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))

                // --- CARD INPUTS ---
                OutlinedTextField(
                    value = cardHolder,
                    onValueChange = { cardHolder = it },
                    label = { Text("Card Holder Name") },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                    shape = RoundedCornerShape(12.dp)
                )
                OutlinedTextField(
                    value = cardNumber,
                    onValueChange = { if (it.length <= 16) cardNumber = it },
                    label = { Text("Card Number") },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                    shape = RoundedCornerShape(12.dp)
                )
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = expiryDate,
                        onValueChange = { if (it.length <= 5) expiryDate = it },
                        label = { Text("MM/YY") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    )
                    OutlinedTextField(
                        value = cvv,
                        onValueChange = { if (it.length <= 3) cvv = it },
                        label = { Text("CVV") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        visualTransformation = PasswordVisualTransformation()
                    )
                }

                Spacer(Modifier.height(32.dp))
                Button(
                    onClick = onSuccess,
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("SECURELY PAY NOW", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }

            } else {
                // --- ADVANCED UPI QR UI ---
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Text("Scan the QR code to pay", color = Color.Gray, fontSize = 14.sp)
                    Spacer(Modifier.height(16.dp))

                    Box(
                        modifier = Modifier
                            .size(260.dp)
                            .background(Color.White, RoundedCornerShape(24.dp))
                            .border(2.dp, Color(0xFFE9ECEF), RoundedCornerShape(24.dp))
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        // Mock QR Graphic
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = null,
                                modifier = Modifier.size(150.dp),
                                tint = Color(0xFFF5F5F5)
                            )
                            Text("BHIM UPI", fontWeight = FontWeight.Black, color = Color.LightGray)
                        }
                        // Scanning animation line (Visual Only)
                        Box(Modifier.fillMaxWidth().height(2.dp).background(Color(0xFF4CAF50)).align(Alignment.TopCenter))
                    }

                    Spacer(Modifier.height(24.dp))

                    Text("Accepted Apps", fontSize = 12.sp, color = Color.Gray)
                    Row(
                        modifier = Modifier.padding(top = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Surface(Modifier.size(40.dp), shape = CircleShape, color = Color(0xFFE3F2FD)) { }
                        Surface(Modifier.size(40.dp), shape = CircleShape, color = Color(0xFFFFF3E0)) { }
                        Surface(Modifier.size(40.dp), shape = CircleShape, color = Color(0xFFE8F5E9)) { }
                    }

                    Spacer(Modifier.height(40.dp))
                    Button(
                        onClick = onSuccess,
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PleasantBannerColor),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("I HAVE PAID", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun PaymentTabItem(title: String, isSelected: Boolean, icon: ImageVector, modifier: Modifier, onClick: () -> Unit) {
    Surface(
        modifier = modifier
            .padding(2.dp)
            .clickable { onClick() },
        color = if (isSelected) Color.White else Color.Transparent,
        shape = RoundedCornerShape(10.dp),
        shadowElevation = if (isSelected) 2.dp else 0.dp
    ) {
        Row(
            modifier = Modifier.padding(vertical = 10.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, null, tint = if (isSelected) PleasantBannerColor else Color.Gray, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(title, color = if (isSelected) PleasantBannerColor else Color.Gray, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }
    }
}

@Composable
fun SuccessScreen(slot: String, onHome: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().background(PleasantBannerColor), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Booking Confirmed!", color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Bold)
        Card(modifier = Modifier.padding(20.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFFB8C00))) {
            Text(text = slot, modifier = Modifier.padding(30.dp), fontSize = 50.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
        }
        Button(onClick = onHome) { Text("Back to Home") }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardCard(title: String, circleColor: Color, icon: ImageVector? = null, onClick: () -> Unit = {}) {
    Card(onClick = onClick, shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.95f)), modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp).height(85.dp)) {
        Row(modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(45.dp).background(circleColor, CircleShape), contentAlignment = Alignment.Center) {
                if (icon != null) {
                    Icon(imageVector = icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
                }
            }
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, fontWeight = FontWeight.Bold, fontSize = 17.sp, color = PleasantBannerColor)
                Text(text = "View details", fontSize = 13.sp, color = Color.Gray)
            }
            Icon(Icons.Default.KeyboardArrowRight, null, tint = PleasantBannerColor)
        }
    }
}