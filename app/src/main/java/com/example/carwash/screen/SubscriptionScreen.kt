package com.example.carwash.screen

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.carwash.model.User

@Composable
fun SubscriptionScreen(
    user: User?,
    onCreateCheckoutSession: ((String) -> Unit) -> Unit = {},
    onActivateSubscription: ((Boolean) -> Unit) -> Unit = {},
    onSubscriptionSuccess: () -> Unit = {},
    onLogout: () -> Unit = {}
) {
    val context = LocalContext.current
    var isLoading by remember { mutableStateOf(false) }

    val isExpired = user?.computedTrialExpired == true
    val trialDaysLeft = user?.remainingTrialDays ?: 0

    val onPayMongoClick = {
        isLoading = true
        onCreateCheckoutSession { checkoutUrl ->
            isLoading = false
            try {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(checkoutUrl))
                context.startActivity(intent)
            } catch (_: Exception) {
                Toast.makeText(context, "Opening PayMongo payment page...", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val onEmailSupportClick = {
        val supportEmail = "custoworks1@gmail.com"
        val subject = Uri.encode("Paid via PayMongo (₱199) - Account Activation")
        val body = Uri.encode(
            "Hi Support,\n\n" +
                    "I completed my PayMongo payment for ₱199.00, but my subscription has not reflected yet.\n\n" +
                    "Account Name: ${user?.name ?: "-"}\n" +
                    "Account Email: ${user?.email ?: "-"}\n" +
                    "User UID: ${user?.uid ?: "-"}\n\n" +
                    "Attached is my proof of payment / GCash receipt.\n\n" +
                    "Please activate my subscription. Thank you!"
        )
        val uri = Uri.parse("mailto:$supportEmail?subject=$subject&body=$body")
        val intent = Intent(Intent.ACTION_SENDTO, uri)
        try {
            context.startActivity(intent)
        } catch (_: Exception) {
            Toast.makeText(context, "Please send email to $supportEmail", Toast.LENGTH_LONG).show()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Spacer(Modifier.height(16.dp))

            // Status Icon
            Surface(
                modifier = Modifier.size(80.dp),
                shape = CircleShape,
                color = if (isExpired) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (isExpired) Icons.Default.Lock else Icons.Default.Star,
                        contentDescription = null,
                        modifier = Modifier.size(40.dp),
                        tint = if (isExpired) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            // Status Title
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = if (isExpired) "7-Day Free Trial Expired" else "7-Day Free Trial Active",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isExpired) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = if (isExpired)
                        "Your 7-day free trial has ended. Monthly payment of ₱199.00 via PayMongo is required to continue."
                    else
                        "You have $trialDaysLeft day(s) remaining in your free trial. Upgrade anytime to Pro!",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.outline
                )
            }

            // Pricing Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Carwash Sale Tracker Pro Subscription",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = "₱199.00",
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = " / month",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

                    // Benefits List
                    ProBenefitRow("Unlimited Carwash Sales & Dashboard Records")
                    ProBenefitRow("Work Teams & Staff Performance Tracking")
                    ProBenefitRow("Carwash Boy Commission Management")
                    ProBenefitRow("Instant GCash, Maya, QRPh & Card via PayMongo")
                    ProBenefitRow("Secure Multi-Tenant Cloud Storage")
                }
            }

            // PayMongo Payment Buttons
            Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = { onPayMongoClick() },
                    enabled = !isLoading,
                    modifier = Modifier.fillMaxWidth().height(54.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                    } else {
                        Icon(Icons.Default.Payment, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Pay via PayMongo (₱199 GCash/Maya/Card)", fontWeight = FontWeight.Bold)
                    }
                }

                OutlinedButton(
                    onClick = { onEmailSupportClick() },
                    modifier = Modifier.fillMaxWidth().height(54.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Default.Email, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(8.dp))
                    Text("Paid but didn't reflect? Email Support", fontWeight = FontWeight.Bold)
                }

                if (isExpired) {
                    TextButton(
                        onClick = onLogout,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Sign Out", color = MaterialTheme.colorScheme.error)
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun ProBenefitRow(text: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
        )
        Spacer(Modifier.width(10.dp))
        Text(text = text, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
    }
}
