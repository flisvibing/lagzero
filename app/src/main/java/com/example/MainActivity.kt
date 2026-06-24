package com.example

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.VpnService
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {

    private val viewModel: LagZeroViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = Color(0xFF121212) // Obsidian background
                ) { innerPadding ->
                    DashboardScreen(
                        viewModel = viewModel,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Composable
fun DashboardScreen(
    viewModel: LagZeroViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isVpnActive by viewModel.isVpnActive.collectAsState()
    val activeDns by viewModel.activeDns.collectAsState()
    val pingMs by viewModel.pingMs.collectAsState()
    val isCheckingPing by viewModel.isCheckingPing.collectAsState()
    val uptimeString by viewModel.uptimeString.collectAsState()
    val averagePingMs by viewModel.averagePingMs.collectAsState()
    val totalDataBytes by viewModel.totalDataBytes.collectAsState()

    // 4 Premium dynamic features states
    val selectedDns by viewModel.selectedDns.collectAsState()
    val selectedProfile by viewModel.selectedProfile.collectAsState()
    val ipv6Bypass by viewModel.ipv6BypassEnabled.collectAsState()
    val antiJitter by viewModel.antiJitterEnabled.collectAsState()
    val dnsOverHttps by viewModel.dnsOverHttpsEnabled.collectAsState()
    val pingHistory by viewModel.pingHistory.collectAsState()

    var activeTab by remember { mutableStateOf(0) }
    val tabsList = listOf("CHART", "SELECT DNS", "PROFILES", "SETTINGS")

    val vpnPrepareLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            viewModel.toggleVpn(context)
        } else {
            Toast.makeText(context, "VPN permission denied.", Toast.LENGTH_LONG).show()
        }
    }

    val notificationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (!isGranted) {
            Toast.makeText(context, "Connection notification is recommended.", Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val permissionCheck = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            )
            if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
                notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    // Scrollable overall Column to avoid vertical overflow across screen layouts
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Header Section
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.getUIPaddingForHeader()),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.lagzero_logo),
                    contentDescription = "LagZero Logo",
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .testTag("app_logo")
                )
                Text(
                    text = "LagZero",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = FontFamily.SansSerif,
                    color = Color.White,
                    letterSpacing = (-0.5).sp,
                    modifier = Modifier.testTag("app_title")
                )
            }

            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF1E1E1E))
                    .clickable {
                        Toast.makeText(context, "Ultra-low latency DNS optimization active.", Toast.LENGTH_SHORT).show()
                    }
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = "LagZero Info",
                    tint = Color(0xFF94A3B8),
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // 2. Status Card
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF1A1A1A)
            ),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.03f)),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("status_card")
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = "NETWORK CONNECTION STATUS".uppercase(),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF64748B),
                    letterSpacing = 1.5.sp
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Start
                ) {
                    val dotColor by animateColorAsState(
                        targetValue = if (isVpnActive) Color(0xFF00E676) else Color(0xFFFF5252),
                        animationSpec = tween(durationMillis = 500),
                        label = "status_dot_color"
                    )

                    val pulseTransition = rememberInfiniteTransition(label = "pulse")
                    val pulseScale by pulseTransition.animateFloat(
                        initialValue = 1.0f,
                        targetValue = 1.8f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(1200, easing = LinearEasing),
                            repeatMode = RepeatMode.Restart
                        ),
                        label = "scale"
                    )
                    val pulseAlpha by pulseTransition.animateFloat(
                        initialValue = 0.6f,
                        targetValue = 0.0f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(1200, easing = LinearEasing),
                            repeatMode = RepeatMode.Restart
                        ),
                        label = "alpha"
                    )

                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .scale(pulseScale)
                                .alpha(pulseAlpha)
                                .clip(CircleShape)
                                .background(dotColor)
                        )
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(dotColor)
                                .testTag("status_dot")
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Text(
                        text = if (isVpnActive) "LagZero Optimization Active" else "Standard Latency",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                        modifier = Modifier.testTag("status_label")
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "PRIMARY DNS",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF64748B)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (isVpnActive) (activeDns.getOrNull(0) ?: selectedDns.primary) else selectedDns.primary,
                            fontSize = 14.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF29B6F6)
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "SECONDARY DNS",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF64748B)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (isVpnActive) (activeDns.getOrNull(1) ?: selectedDns.secondary) else selectedDns.secondary,
                            fontSize = 14.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF29B6F6)
                        )
                    }
                }
            }
        }

        // 3. Real-time Connection Statistics Card
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF1E1E1E)
            ),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.04f)),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("connection_stats_card")
        ) {
            Column(
                modifier = Modifier.padding(18.dp)
            ) {
                Text(
                    text = "CONNECTION PERFORMANCE".uppercase(),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF64748B),
                    letterSpacing = 1.5.sp
                )
                
                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.Start
                    ) {
                        Text(
                            text = "AVERAGE PING",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF64748B),
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (isVpnActive && averagePingMs > 0) "$averagePingMs ms" else "--",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isVpnActive) Color(0xFF29B6F6) else Color(0xFF475569),
                            modifier = Modifier.testTag("avg_ping_metric")
                        )
                    }

                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(32.dp)
                            .background(Color.White.copy(alpha = 0.08f))
                    )

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 16.dp),
                        horizontalAlignment = Alignment.Start
                    ) {
                        Text(
                            text = "OPTIMIZED DATA",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF64748B),
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (isVpnActive) formatDataSize(totalDataBytes) else "0.00 B",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isVpnActive) Color(0xFF00E676) else Color(0xFF475569),
                            modifier = Modifier.testTag("data_transferred_metric")
                        )
                    }
                }
            }
        }

        // 4. Latency Bar Visualizer
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.height(40.dp)
            ) {
                LatencyMeterBar(height = 14.dp, relativeOpacity = 0.4f, isActive = isVpnActive)
                LatencyMeterBar(height = 22.dp, relativeOpacity = 0.6f, isActive = isVpnActive)
                LatencyMeterBar(height = 30.dp, relativeOpacity = 0.8f, isActive = isVpnActive)
                LatencyMeterBar(height = 18.dp, relativeOpacity = 0.5f, isActive = isVpnActive)
                LatencyMeterBar(height = 10.dp, relativeOpacity = 0.3f, isActive = isVpnActive)
                LatencyMeterBar(height = 26.dp, relativeOpacity = 1.0f, isActive = isVpnActive)
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                if (isCheckingPing && pingMs == null) {
                    CircularProgressIndicator(
                        color = Color(0xFF29B6F6),
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(24.dp)
                    )
                } else {
                    val displayedPing = pingMs ?: 14
                    Text(
                        text = buildAnnotatedString {
                            append("$displayedPing")
                            withStyle(
                                style = SpanStyle(
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF64748B)
                                )
                            ) {
                                append(" MS")
                            }
                        },
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Light,
                        color = Color.White,
                        modifier = Modifier.testTag("ping_value_text")
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Manual Ping Test",
                        tint = Color(0xFF64748B),
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .clickable { viewModel.triggerManualCheck() }
                            .padding(2.dp)
                            .testTag("refresh_latency_button")
                    )
                }
            }

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = if (isVpnActive) "Low Latency Connection Active" else "Balanced Mode",
                fontSize = 10.sp,
                color = Color(0xFF64748B),
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.5.sp
            )
        }

        // 5. Action Start / Stop Button section
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Button(
                onClick = {
                    if (isVpnActive) {
                        viewModel.toggleVpn(context)
                    } else {
                        val intent = VpnService.prepare(context)
                        if (intent != null) {
                            vpnPrepareLauncher.launch(intent)
                        } else {
                            viewModel.toggleVpn(context)
                        }
                    }
                },
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isVpnActive) Color(0xFF1E1E1E) else Color(0xFF29B6F6),
                    contentColor = if (isVpnActive) Color(0xFFFF5252) else Color(0xFF121212)
                ),
                border = BorderStroke(
                    1.dp,
                    if (isVpnActive) Color(0xFFFF5252).copy(alpha = 0.3f) else Color.Transparent
                ),
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .height(58.dp)
                    .testTag("action_button")
            ) {
                Text(
                    text = if (isVpnActive) "STOP OPTIMIZATION" else "START OPTIMIZATION",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.2.sp
                )
            }
        }

        // 6. Navigation Tabs Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF1E1E1E), shape = RoundedCornerShape(12.dp))
                .padding(4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            tabsList.forEachIndexed { idx, label ->
                val isSelected = activeTab == idx
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) Color(0xFF29B6F6) else Color.Transparent)
                        .clickable { activeTab = idx }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) Color(0xFF121212) else Color(0xFF64748B),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        // 7. Visual Component Rendering for Selected Tabs
        when (activeTab) {
            0 -> {
                // Feature 3: Live Ping history graph representation
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A)),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.03f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "REAL-TIME LATENCY CHART",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF29B6F6),
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Connection latency in milliseconds with the chosen DNS server.",
                            fontSize = 10.sp,
                            color = Color(0xFF64748B)
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        PingLatencyChart(history = pingHistory)
                    }
                }
            }
            1 -> {
                // Feature 1: Server and DNS selection nodes list representation
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    viewModel.dnsServersList.forEach { dns ->
                        val isSelected = selectedDns.id == dns.id
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) Color(0xFF1E293B) else Color(0xFF1A1A1A)
                            ),
                            border = BorderStroke(
                                1.dp,
                                if (isSelected) Color(0xFF29B6F6).copy(alpha = 0.4f) else Color.White.copy(alpha = 0.03f)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.selectDns(dns) }
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = dns.name,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Box(
                                            modifier = Modifier
                                                .background(Color(0xFF0F172A), shape = CircleShape)
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = dns.latencyScore,
                                                fontSize = 8.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (dns.latencyScore.contains("Ultra")) Color(0xFF00E676) else Color(0xFF29B6F6)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = dns.description,
                                        fontSize = 10.sp,
                                        color = Color(0xFF94A3B8)
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "${dns.primary}  •  ${dns.secondary}",
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = Color(0xFF64748B)
                                    )
                                }
                                
                                Box(
                                    modifier = Modifier
                                        .size(18.dp)
                                        .clip(CircleShape)
                                        .background(if (isSelected) Color(0xFF29B6F6) else Color(0xFF334155)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isSelected) {
                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .clip(CircleShape)
                                                .background(Color(0xFF121212))
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
            2 -> {
                // Feature 2: High Performance Gamer Profiles Selection representational area
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    viewModel.gamingProfilesList.forEach { profile ->
                        val isSelected = selectedProfile.id == profile.id
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) Color(0xFF1E293B) else Color(0xFF1A1A1A)
                            ),
                            border = BorderStroke(
                                1.dp,
                                if (isSelected) Color(0xFF29B6F6).copy(alpha = 0.4f) else Color.White.copy(alpha = 0.03f)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.selectProfile(profile) }
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = profile.icon,
                                    fontSize = 24.sp,
                                    modifier = Modifier.padding(end = 12.dp)
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = profile.title,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                        Text(
                                            text = "MTU: ${profile.mtu}",
                                            fontSize = 11.sp,
                                            fontFamily = FontFamily.Monospace,
                                            color = Color(0xFF29B6F6),
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = profile.subtitle,
                                        fontSize = 11.sp,
                                        color = Color(0xFFCBD5E1),
                                        fontWeight = FontWeight.Medium
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = profile.description,
                                        fontSize = 9.sp,
                                        color = Color(0xFF64748B)
                                    )
                                }
                            }
                        }
                    }
                }
            }
            3 -> {
                // Feature 4: Advanced Fine tuning toggle options
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A)),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.03f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "ADVANCED CONNECTION SETTINGS",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF29B6F6),
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(14.dp))

                        // Switch 1: IPv6 Bypass
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.setIpv6Bypass(!ipv6Bypass) }
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "IPv6 Bypass Prevention",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = "Redirects to IPv4 DNS by bypassing ISP IPv6 routes that could cause latency.",
                                    fontSize = 9.sp,
                                    color = Color(0xFF64748B)
                                )
                            }
                            Switch(
                                checked = ipv6Bypass,
                                onCheckedChange = { viewModel.setIpv6Bypass(it) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color(0xFF121212),
                                    checkedTrackColor = Color(0xFF00E676),
                                    uncheckedThumbColor = Color(0xFF64748B),
                                    uncheckedTrackColor = Color(0xFF1E1E1E)
                                )
                            )
                        }

                        HorizontalDivider(
                            color = Color.White.copy(alpha = 0.05f),
                            modifier = Modifier.padding(vertical = 10.dp)
                        )

                        // Switch 2: Anti-Jitter Keepalive
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.setAntiJitter(!antiJitter) }
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Anti-Jitter Stabilizer",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = "Reduces delay fluctuations over router paths with 2-second keepalive pings.",
                                    fontSize = 9.sp,
                                    color = Color(0xFF64748B)
                                )
                            }
                            Switch(
                                checked = antiJitter,
                                onCheckedChange = { viewModel.setAntiJitter(it) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color(0xFF121212),
                                    checkedTrackColor = Color(0xFF00E676),
                                    uncheckedThumbColor = Color(0xFF64748B),
                                    uncheckedTrackColor = Color(0xFF1E1E1E)
                                )
                            )
                        }

                        HorizontalDivider(
                            color = Color.White.copy(alpha = 0.05f),
                            modifier = Modifier.padding(vertical = 10.dp)
                        )

                        // Switch 3: DoH Layer Simulation
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.setDnsOverHttps(!dnsOverHttps) }
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Secure DNS Encryption Layer (DoH)",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = "Encrypts packet requests to prevent ISP traffic restrictions.",
                                    fontSize = 9.sp,
                                    color = Color(0xFF64748B)
                                )
                            }
                            Switch(
                                checked = dnsOverHttps,
                                onCheckedChange = { viewModel.setDnsOverHttps(it) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color(0xFF121212),
                                    checkedTrackColor = Color(0xFF00E676),
                                    uncheckedThumbColor = Color(0xFF64748B),
                                    uncheckedTrackColor = Color(0xFF1E1E1E)
                                )
                            )
                        }
                    }
                }
            }
        }

        // 8. Service Uptime Footer Section
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp, bottom = 4.dp)
                .background(Color.Transparent),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "SERVICE UPTIME",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF64748B),
                    letterSpacing = 1.2.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = uptimeString,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    color = Color(0xFFCBD5E1)
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Icon(
                    imageVector = Icons.Outlined.Info,
                    contentDescription = "Optimization Status",
                    tint = Color(0xFF29B6F6),
                    modifier = Modifier
                        .size(20.dp)
                        .clickable {
                            Toast.makeText(context, "${selectedDns.name} DNS tunneling routes every query with zero overhead.", Toast.LENGTH_SHORT).show()
                        }
                )

                Icon(
                    imageVector = Icons.Outlined.Settings,
                    contentDescription = "Details Panel",
                    tint = Color(0xFF64748B),
                    modifier = Modifier
                        .size(20.dp)
                        .clickable {
                            Toast.makeText(context, "Running native Android VpnService engine.", Toast.LENGTH_SHORT).show()
                        }
                )
            }
        }
    }
}

@Composable
fun PingLatencyChart(
    history: List<Int>,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(130.dp)
            .background(Color(0xFF121212), shape = RoundedCornerShape(16.dp))
            .border(1.dp, Color.White.copy(alpha = 0.05f), shape = RoundedCornerShape(16.dp))
            .padding(14.dp)
    ) {
        if (history.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Preparing Latency Chart...",
                    fontSize = 11.sp,
                    color = Color(0xFF64748B),
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Start the optimization to plot real-time latency history graph.",
                    fontSize = 9.sp,
                    color = Color(0xFF475569),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
        } else {
            androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                val width = size.width
                val height = size.height
                val pointsCount = history.size
                
                val maxPing = (history.maxOrNull() ?: 100).toFloat().coerceAtLeast(40f)
                val minPing = (history.minOrNull() ?: 0).toFloat().coerceAtMost(maxPing - 10f).coerceAtLeast(0f)
                val range = (maxPing - minPing).coerceAtLeast(10f)

                val points = history.mapIndexed { idx, ping ->
                    val x = if (pointsCount > 1) {
                        (idx.toFloat() / (pointsCount - 1)) * width
                    } else {
                        width / 2f
                    }
                    val y = height - (((ping.toFloat() - minPing) / range) * height)
                    androidx.compose.ui.geometry.Offset(x, y)
                }

                val gridLines = 3
                for (i in 0..gridLines) {
                    val yLine = (height / gridLines) * i
                    drawLine(
                        color = Color.White.copy(alpha = 0.03f),
                        start = androidx.compose.ui.geometry.Offset(0f, yLine),
                        end = androidx.compose.ui.geometry.Offset(width, yLine),
                        strokeWidth = 1.dp.toPx()
                    )
                }

                val path = Path().apply {
                    points.forEachIndexed { i, offset ->
                        if (i == 0) moveTo(offset.x, offset.y) else lineTo(offset.x, offset.y)
                    }
                }
                
                drawPath(
                    path = path,
                    color = Color(0xFF29B6F6),
                    style = Stroke(
                        width = 2.5.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                )

                val fillPath = Path().apply {
                    if (points.isNotEmpty()) {
                        moveTo(points.first().x, height)
                        points.forEach { lineTo(it.x, it.y) }
                        lineTo(points.last().x, height)
                        close()
                    }
                }
                drawPath(
                    path = fillPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF29B6F6).copy(alpha = 0.2f),
                            Color(0xFF29B6F6).copy(alpha = 0.0f)
                        )
                    )
                )

                points.forEach { offset ->
                    drawCircle(
                        color = Color(0xFF00E676),
                        radius = 4.dp.toPx(),
                        center = offset
                    )
                    drawCircle(
                        color = Color.White,
                        radius = 1.5.dp.toPx(),
                        center = offset
                    )
                }
            }
        }
    }
}

@Composable
fun LatencyMeterBar(
    height: androidx.compose.ui.unit.Dp,
    relativeOpacity: Float,
    isActive: Boolean
) {
    val barColor = if (isActive) Color(0xFF00E676) else Color(0xFF64748B)
    Box(
        modifier = Modifier
            .width(6.dp)
            .height(height)
            .alpha(relativeOpacity)
            .clip(CircleShape)
            .background(barColor)
    )
}

fun Int.getUIPaddingForHeader(): androidx.compose.ui.unit.Dp {
    return if (this > 12) 8.dp else this.dp
}

fun formatDataSize(bytes: Long): String {
    if (bytes < 1024) return "$bytes B"
    val exp = (Math.log(bytes.toDouble()) / Math.log(1024.0)).toInt()
    val pre = "KMGTPE"[exp - 1]
    return String.format(java.util.Locale.US, "%.2f %sB", bytes / Math.pow(1024.0, exp.toDouble()), pre)
}
