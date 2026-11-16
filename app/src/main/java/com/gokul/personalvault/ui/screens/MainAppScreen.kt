
package com.gokul.personalvault.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Note
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.TrendingDown
import androidx.compose.material.icons.outlined.TrendingUp
import androidx.compose.material.icons.outlined.Wallet
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.gokul.personalvault.ui.theme.VaultTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import com.gokul.personalvault.ui.screens.PasswordsScreen

private val Navy = Color(0xFF14213D)
private val Blue = Color(0xFF315CDB)
private val Background = Color(0xFFF4F6FB)
private val Muted = Color(0xFF64748B)
private val Green = Color(0xFF16845B)
private val Red = Color(0xFFD94848)

private data class VaultTab(
    val title: String,
    val icon: ImageVector
)

@Composable
fun MainAppScreen(
    email: String,
    householdId: String,
    currentUid: String,
    ownerUid: String,
    memberUids: List<String>,
    onSignOut: () -> Unit
) {
    val tabs = listOf(
        VaultTab("Home", Icons.Outlined.Home),
        VaultTab("Finance", Icons.Outlined.AccountBalanceWallet),
        VaultTab("Passwords", Icons.Outlined.Key),
        VaultTab("Documents", Icons.Outlined.Folder),
        VaultTab("Notes", Icons.Outlined.Note),
    )

    var selectedTab by remember { mutableIntStateOf(0) }

    VaultTheme {
        Scaffold(
            containerColor = Background,
            bottomBar = {
                NavigationBar(
                    containerColor = Color.White
                ) {
                    tabs.forEachIndexed { index, tab ->
                        NavigationBarItem(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            icon = {
                                Icon(
                                    imageVector = tab.icon,
                                    contentDescription = tab.title
                                )
                            },
                            label = { Text(tab.title) }
                        )
                    }
                }
            }
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                when (selectedTab) {
                    0 -> DashboardContent(
                        email = email,
                        onOpenFinance = { selectedTab = 1 },
                        onSignOut = onSignOut
                    )
                    1 -> PlaceholderContent(
                        title = "Finance",
                        description = "Manage your accounts, income, expenses and credit cards.",
                        icon = Icons.Outlined.Wallet
                    )

                    2 -> PasswordsScreen(
                        householdId = householdId,
                        currentUid = currentUid,
                        ownerUid = ownerUid,
                        memberUids = memberUids
                    )

                    3 -> PlaceholderContent(
                        title = "My Documents",
                        description = "Organise your files, folders and important documents.",
                        icon = Icons.Outlined.Folder
                    )

                    4 -> PlaceholderContent(
                        title = "Secure Notes",
                        description = "Keep your private notes and attachments protected.",
                        icon = Icons.Outlined.Note
                    )

                }
            }
        }
    }
}

@Composable
private fun DashboardContent(
    email: String,
    onOpenFinance: () -> Unit,
    onSignOut: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    "PERSONAL VAULT 2.0",
                    color = Muted,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Welcome back!",
                    color = Navy,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    email,
                    color = Muted,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Icon(
                    Icons.Outlined.Notifications,
                    contentDescription = "Notifications",
                    tint = Blue,
                    modifier = Modifier.size(27.dp)
                )
                androidx.compose.material3.TextButton(
                    onClick = onSignOut,
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
                ) {
                    Text("Sign out", color = Muted, style = MaterialTheme.typography.labelSmall)
                }
            }
        }

        Spacer(Modifier.height(22.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(
                containerColor = Navy
            )
        ) {
            Column(Modifier.padding(20.dp)) {
                Text(
                    "TOTAL BALANCE",
                    color = Color(0xFFB9C9E2),
                    style = MaterialTheme.typography.labelMedium
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "••••••••",
                    color = Color.White,
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Balances are hidden until finance data is connected.",
                    color = Color(0xFFCBD5E1),
                    style = MaterialTheme.typography.bodySmall
                )
                Spacer(Modifier.height(18.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("INDIA · INR", color = Color(0xFFB9C9E2))
                        Text("₹ —", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                    Column {
                        Text("OMAN · OMR", color = Color(0xFFB9C9E2))
                        Text("OMR —", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Spacer(Modifier.height(18.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SummaryCard(
                title = "Income",
                amount = "₹ —",
                icon = Icons.Outlined.TrendingUp,
                iconColor = Green,
                modifier = Modifier.weight(1f)
            )
            SummaryCard(
                title = "Expenses",
                amount = "₹ —",
                icon = Icons.Outlined.TrendingDown,
                iconColor = Red,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(Modifier.height(22.dp))

        Text(
            "Quick access",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Navy
        )

        Spacer(Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            QuickAction("Passwords", Icons.Outlined.Key)
            QuickAction("Documents", Icons.Outlined.Description)
            QuickAction("Notes", Icons.Outlined.Note)
            QuickAction(
                "Finance",
                Icons.Outlined.Wallet,
                onClick = onOpenFinance
            )
        }

        Spacer(Modifier.height(22.dp))

        Text(
            "Payment reminders",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Navy
        )

        Spacer(Modifier.height(10.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White
            )
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Outlined.AccountBalanceWallet,
                    contentDescription = null,
                    tint = Blue,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        "Credit card reminders",
                        fontWeight = FontWeight.SemiBold,
                        color = Navy
                    )
                    Text(
                        "Upcoming payment reminders will appear here.",
                        color = Muted,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                "Recent activity",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Navy
            )
            Icon(
                Icons.Outlined.ReceiptLong,
                contentDescription = null,
                tint = Muted
            )
        }

        Spacer(Modifier.height(8.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White
            )
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "No transactions yet",
                    color = Muted,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        Spacer(Modifier.height(8.dp))
        Text(
            "Demo dashboard · No real financial data is connected yet",
            color = Muted,
            style = MaterialTheme.typography.labelSmall
        )
    }
}

@Composable
private fun SummaryCard(
    title: String,
    amount: String,
    icon: ImageVector,
    iconColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        )
    ) {
        Column(Modifier.padding(14.dp)) {
            Icon(icon, contentDescription = null, tint = iconColor)
            Spacer(Modifier.height(8.dp))
            Text(title, color = Muted)
            Text(
                amount,
                color = Navy,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun QuickAction(
    title: String,
    icon: ImageVector,
    onClick: () -> Unit = {}
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Card(
            onClick = onClick,
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White
            )
        ) {
            Column(
                modifier = Modifier
                    .size(64.dp)
                    .padding(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(icon, contentDescription = title, tint = Blue)
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            title,
            color = Muted,
            style = MaterialTheme.typography.labelSmall
        )
    }
}

@Composable
private fun PlaceholderContent(
    title: String,
    description: String,
    icon: ImageVector
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = Blue,
            modifier = Modifier.size(64.dp)
        )
        Spacer(Modifier.height(18.dp))
        Text(
            title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = Navy
        )
        Spacer(Modifier.height(8.dp))
        Text(
            description,
            color = Muted
        )
        Spacer(Modifier.height(18.dp))
        Text(
            "This section will be implemented in the next stage.",
            color = Muted,
            style = MaterialTheme.typography.bodySmall
        )
    }
}
