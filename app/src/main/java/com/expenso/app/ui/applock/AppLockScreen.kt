package com.expenso.app.ui.applock

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Wallet
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.expenso.app.ui.components.Avatar

private val LockBackground = Color(0xFF0C0B18)
private val LockSurface = Color(0xFF1B1930)
private val LockLine = Color(0xFF2C2A46)
private val LockMuted = Color(0xFF9C99B8)
private val LockIndigo = Color(0xFF8B86FF)

@Composable
fun AppLockScreen(
    name: String,
    initials: String,
    avatarId: String,
    error: String?,
    onUnlock: (String) -> Unit,
    onClearError: () -> Unit
) {
    var pin by remember { mutableStateOf("") }

    LaunchedEffect(pin) {
        if (pin.length == 4) onUnlock(pin)
    }

    LaunchedEffect(error) {
        if (error != null) pin = ""
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF191634), Color(0xFF0D0C18), LockBackground)
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 30.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(36.dp))

            Box(
                modifier = Modifier
                    .size(62.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        Brush.linearGradient(listOf(Color(0xFF8A85FF), Color(0xFF5149E4)))
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Wallet,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(30.dp)
                )
            }

            Spacer(Modifier.height(14.dp))
            Text(
                text = "Expenso",
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.2.sp
            )

            Spacer(Modifier.height(40.dp))

            Avatar(
                initials = initials.ifBlank { "?" },
                avatarId = avatarId,
                size = 72.dp
            )

            Spacer(Modifier.height(14.dp))
            Text(
                text = "Welcome back",
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = name.ifBlank { "Expenso User" },
                color = LockMuted,
                fontSize = 14.sp,
                maxLines = 1,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(30.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                repeat(4) { index ->
                    val filled = index < pin.length
                    Box(
                        modifier = Modifier
                            .size(15.dp)
                            .clip(CircleShape)
                            .then(
                                if (filled) {
                                    Modifier.background(LockIndigo)
                                } else {
                                    Modifier.border(1.5.dp, LockLine, CircleShape)
                                }
                            )
                    )
                }
            }

            Spacer(Modifier.height(14.dp))
            Box(modifier = Modifier.height(24.dp)) {
                if (error != null) {
                    Text(
                        text = error,
                        color = Color(0xFFFF8A90),
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(Modifier.weight(1f))

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                listOf(
                    listOf("1", "2", "3"),
                    listOf("4", "5", "6"),
                    listOf("7", "8", "9"),
                    listOf("", "0", "back")
                ).forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(18.dp)
                    ) {
                        row.forEach { key ->
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(64.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                when (key) {
                                    "" -> Unit
                                    "back" -> KeypadButton(
                                        label = "Delete",
                                        onClick = {
                                            if (pin.isNotEmpty()) {
                                                pin = pin.dropLast(1)
                                                onClearError()
                                            }
                                        }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Backspace,
                                            contentDescription = "Delete digit",
                                            tint = Color.White.copy(alpha = 0.9f),
                                            modifier = Modifier.size(23.dp)
                                        )
                                    }
                                    else -> KeypadButton(
                                        label = key,
                                        onClick = {
                                            if (pin.length < 4) {
                                                onClearError()
                                                pin += key
                                            }
                                        }
                                    ) {
                                        Text(
                                            text = key,
                                            color = Color.White,
                                            fontSize = 24.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(22.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.Lock,
                    contentDescription = null,
                    tint = LockMuted,
                    modifier = Modifier.size(13.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "Your PIN is hashed and stored only on this device",
                    color = LockMuted,
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center
                )
            }
            Spacer(Modifier.height(18.dp))
        }
    }
}

@Composable
private fun KeypadButton(
    label: String,
    onClick: () -> Unit,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = Modifier
            .size(72.dp)
            .semantics { contentDescription = label }
            .clickable(onClick = onClick),
        shape = CircleShape,
        color = LockSurface,
        border = BorderStroke(1.dp, LockLine)
    ) {
        Box(contentAlignment = Alignment.Center) { content() }
    }
}
