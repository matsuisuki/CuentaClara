package com.cuentaclara.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.BackHandler
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import java.text.NumberFormat
import java.util.Locale

private val Green = Color(0xFF286644)
private val Pale = Color(0xFFEAF2EC)

class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { CuentaClaraApp(onBiometric = { authenticate() }) }
    }

    private fun authenticate() {
        val manager = BiometricManager.from(this)
        if (manager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL) != BiometricManager.BIOMETRIC_SUCCESS) return
        val prompt = BiometricPrompt(this, ContextCompat.getMainExecutor(this),
            object : BiometricPrompt.AuthenticationCallback() {})
        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Desbloquear Cuenta Clara")
            .setSubtitle("Confirma tu identidad para acceder a tus finanzas")
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL)
            .build()
        prompt.authenticate(info)
    }
}

data class Entry(
    val id: Int,
    val kind: String,
    val title: String,
    val amount: Double,
    val currency: String,
    val dueDate: String,
    val paid: Double = 0.0
) {
    val remaining: Double get() = (amount - paid).coerceAtLeast(0.0)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CuentaClaraApp(onBiometric: () -> Unit) {
    var page by remember { mutableStateOf("Resumen") }
    var entries by remember { mutableStateOf(listOf(
        Entry(1, "Por cobrar", "Ejemplo: Carlos", 100.0, "USD", "2026-10-15", 30.0),
        Entry(2, "Deuda", "Ejemplo: Tarjeta", 80.0, "USD", "2026-10-18", 20.0),
        Entry(3, "Ahorro", "Fondo de emergencia", 250.0, "USD", "2026-10-20")
    )) }
    var rate by remember { mutableStateOf("") }
    var showAdd by remember { mutableStateOf(false) }
    var kind by remember { mutableStateOf("Por cobrar") }
    var title by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var currency by remember { mutableStateOf("USD") }
    var dueDate by remember { mutableStateOf("2026-10-15") }
    var payEntry by remember { mutableStateOf<Entry?>(null) }
    var payment by remember { mutableStateOf("") }
    var showCalendar by remember { mutableStateOf(false) }

    val usd = NumberFormat.getCurrencyInstance(Locale.US)
    val format: (Double, String) -> String = { n, c ->
        val nf = NumberFormat.getNumberInstance(Locale("es", "VE")).apply { minimumFractionDigits = 2; maximumFractionDigits = 2 }
        nf.format(n) + if (c == "USD") " $" else " Bs."
    }
    val receivable = entries.filter { it.kind == "Por cobrar" }.sumOf { it.remaining }
    val payable = entries.filter { it.kind == "Deuda" }.sumOf { it.remaining }
    val savings = entries.filter { it.kind == "Ahorro" }.sumOf { it.amount }

    MaterialTheme(colorScheme = lightColorScheme(primary = Green, secondary = Color(0xFF6A9B76), background = Color(0xFFF4F6F4))) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Column { Text("Cuenta Clara", fontWeight = FontWeight.Bold); Text("Finanzas personales", style = MaterialTheme.typography.labelSmall) } },
                    actions = { IconButton(onClick = onBiometric) { Icon(Icons.Default.Fingerprint, "Probar autenticación biométrica") } }
                )
            },
            bottomBar = {
                NavigationBar {
                    listOf("Resumen", "Movimientos", "Calendario", "Cambio").forEach { item ->
                        NavigationBarItem(
                            selected = page == item,
                            onClick = { page = item },
                            icon = {
                                Icon(when (item) {
                                    "Resumen" -> Icons.Default.Dashboard
                                    "Movimientos" -> Icons.Default.SwapHoriz
                                    "Calendario" -> Icons.Default.CalendarMonth
                                    else -> Icons.Default.CurrencyExchange
                                }, contentDescription = item)
                            },
                            label = { Text(item) }
                        )
                    }
                }
            }
        ) { padding ->
            Column(Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                when (page) {
                    "Resumen" -> {
                        Card(colors = CardDefaults.cardColors(containerColor = Pale)) {
                            Column(Modifier.padding(18.dp)) {
                                Text("Tu dinero, organizado", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Green)
                                Text("Moneda principal: dólares (USD)")
                                Text("Tasa BCV: ${rate.ifBlank { "sin configurar" }} Bs./USD")
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            SummaryCard("Por cobrar", format(receivable, "USD"), Modifier.weight(1f))
                            SummaryCard("Deudas", format(payable, "USD"), Modifier.weight(1f))
                        }
                        SummaryCard("Ahorros registrados", format(savings, "USD"), Modifier.fillMaxWidth())
                        Text("Próximos movimientos", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        entries.sortedBy { it.dueDate }.take(5).forEach { EntryCard(it, format) { payEntry = it; payment = ""; } }
                        Text("Los datos de ejemplo son temporales en esta versión inicial.", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    }
                    "Movimientos" -> {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            Text("Movimientos", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            Button(onClick = { showAdd = true }) { Icon(Icons.Default.Add, null); Text("Añadir") }
                        }
                        entries.forEach { EntryCard(it, format) { payEntry = it; payment = "" } }
                    }
                    "Calendario" -> {
                        Text("Agenda de pagos", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text("Selecciona una fecha para revisar vencimientos. En esta versión inicial se muestran los próximos registros.")
                        entries.sortedBy { it.dueDate }.forEach { EntryCard(it, format) { payEntry = it; payment = "" } }
                        OutlinedButton(onClick = { showAdd = true }) { Icon(Icons.Default.Event, null); Text("Añadir fecha de pago o cobro") }
                    }
                    else -> {
                        Text("Conversor USD / VES", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        OutlinedTextField(value = rate, onValueChange = { rate = it.filter { ch -> ch.isDigit() || ch == '.' || ch == ',' } }, label = { Text("Tasa Bs. por USD") }, modifier = Modifier.fillMaxWidth())
                        Text("La tasa se introduce manualmente en este prototipo. La conexión automática al BCV debe integrarse y validarse.")
                        if ((rate.replace(",", ".").toDoubleOrNull() ?: 0.0) > 0) {
                            Text("100 USD = " + format(100 * (rate.replace(",", ".").toDoubleOrNull() ?: 0.0), "VES"), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        if (showAdd) {
            AlertDialog(
                onDismissRequest = { showAdd = false },
                title = { Text("Nuevo registro") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        var menu by remember { mutableStateOf(false) }
                        ExposedDropdownMenuBox(expanded = menu, onExpandedChange = { menu = it }) {
                            OutlinedTextField(value = kind, onValueChange = {}, readOnly = true, label = { Text("Clasificación") }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(menu) }, modifier = Modifier.menuAnchor().fillMaxWidth())
                            ExposedDropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                                listOf("Ingreso", "Gasto", "Deuda", "Por cobrar", "Ahorro").forEach { k -> DropdownMenuItem(text = { Text(k) }, onClick = { kind = k; menu = false }) }
                            }
                        }
                        OutlinedTextField(title, { title = it }, label = { Text("Descripción/persona") }, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(amount, { amount = it }, label = { Text("Monto") }, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(currency, { currency = it.uppercase().take(3) }, label = { Text("Moneda: USD o VES") }, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(dueDate, { dueDate = it }, label = { Text("Fecha YYYY-MM-DD") }, modifier = Modifier.fillMaxWidth())
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        val n = amount.replace(",", ".").toDoubleOrNull()
                        if (title.isNotBlank() && n != null && n > 0 && currency in listOf("USD", "VES")) {
                            entries = entries + Entry((entries.maxOfOrNull { it.id } ?: 0) + 1, kind, title.trim(), n, currency, dueDate)
                            title = ""; amount = ""; showAdd = false
                        }
                    }) { Text("Guardar") }
                },
                dismissButton = { TextButton(onClick = { showAdd = false }) { Text("Cancelar") } }
            )
        }

        payEntry?.let { entry ->
            AlertDialog(
                onDismissRequest = { payEntry = null },
                title = { Text("Registrar pago parcial") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("${entry.title}: pendiente ${format(entry.remaining, entry.currency)}")
                        OutlinedTextField(payment, { payment = it }, label = { Text("Monto pagado/recibido") }, modifier = Modifier.fillMaxWidth())
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        val p = payment.replace(",", ".").toDoubleOrNull()
                        if (p != null && p > 0 && p <= entry.remaining) {
                            entries = entries.map { if (it.id == entry.id) it.copy(paid = it.paid + p) else it }
                            payEntry = null
                        }
                    }) { Text("Registrar") }
                },
                dismissButton = { TextButton(onClick = { payEntry = null }) { Text("Cancelar") } }
            )
        }
    }
}

@Composable
private fun SummaryCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier = modifier) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(label, style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Green)
        }
    }
}

@Composable
private fun EntryCard(entry: Entry, format: (Double, String) -> String, onPay: () -> Unit) {
    Card {
        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(entry.title, fontWeight = FontWeight.SemiBold)
                Text("${entry.kind} · ${entry.dueDate}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                Text("Total ${format(entry.amount, entry.currency)} · Pendiente ${format(entry.remaining, entry.currency)}", style = MaterialTheme.typography.bodySmall)
            }
            if (entry.kind == "Por cobrar" || entry.kind == "Deuda") {
                TextButton(onClick = onPay) { Text("Abonar") }
            }
        }
    }
}
