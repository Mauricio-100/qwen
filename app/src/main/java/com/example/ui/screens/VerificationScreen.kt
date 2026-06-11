package com.example.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.viewmodel.ProfileViewModel
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun VerificationScreen(viewModel: ProfileViewModel) {
    val verificationStatus by viewModel.verificationStatus.collectAsState()
    var birthDate by remember { mutableStateOf("2000-01-01") }

    LaunchedEffect(Unit) {
        viewModel.loadVerificationStatus()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (verificationStatus == null) {
            CircularProgressIndicator(modifier = Modifier.padding(top = 32.dp))
            return@Column
        }

        val status = verificationStatus!!

        if (status.isVerified) {
            // Astrological Sign Dynamic UI Content
            Spacer(modifier = Modifier.height(32.dp))
            AnimatedZodiacBadge(zodiacSign = status.zodiacSign ?: "Cosmic")
        } else {
            // Requirements Tracker
            Text(
                text = "Critères de Certification",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            // Followers Progress
            ProgressItem(
                label = "Abonnés",
                current = status.followersCount.toFloat(),
                target = status.criteria.minFollowers.toFloat()
            )
            Spacer(modifier = Modifier.height(16.dp))

            // Views Progress
            ProgressItem(
                label = "Vues cumulées",
                current = status.totalViews.toFloat(),
                target = status.criteria.minTotalViews.toFloat()
            )
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(text = "Âge minimum: ${status.criteria.minAge} ans", style = MaterialTheme.typography.bodyLarge)

            Spacer(modifier = Modifier.height(32.dp))
            
            Text(text = "Entrez votre date de naissance (YYYY-MM-DD):", style = MaterialTheme.typography.bodyMedium)
            OutlinedTextField(
                value = birthDate,
                onValueChange = { birthDate = it },
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = { viewModel.requestVerification(birthDate) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Demander le badge")
            }
        }
    }
}

@Composable
fun ProgressItem(label: String, current: Float, target: Float) {
    val progress = (current / target).coerceIn(0f, 1f)
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, fontWeight = FontWeight.SemiBold)
            Text("${current.toInt()} / ${target.toInt()}")
        }
        Spacer(modifier = Modifier.height(8.dp))
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier.fillMaxWidth().height(8.dp),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
    }
}

@Composable
fun AnimatedZodiacBadge(zodiacSign: String) {
    val infiniteTransition = rememberInfiniteTransition()
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        )
    )
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(10000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(200.dp)) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val radius = size.minDimension / 2
                drawCircle(
                    color = Color(0xC2EFC832),
                    radius = radius * scale,
                    style = Stroke(width = 8f)
                )
                // Draw some decorative stars
                for (i in 0..5) {
                    val angle = (i * 60 + rotation) * (Math.PI / 180)
                    val cx = center.x + (radius * 0.7f) * cos(angle).toFloat()
                    val cy = center.y + (radius * 0.7f) * sin(angle).toFloat()
                    drawCircle(color = Color.White, radius = 6f * scale, center = androidx.compose.ui.geometry.Offset(cx, cy))
                }
            }
            Icon(
                imageVector = Icons.Default.Star,
                contentDescription = null,
                tint = Color(0xFFFFD700),
                modifier = Modifier.size(100.dp).scale(scale)
            )
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        Text(
            text = "Félicitations, vous êtes certifié !",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedCard(
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(24.dp).fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Signe Cosmique",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = zodiacSign,
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
