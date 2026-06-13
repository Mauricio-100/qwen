package com.example.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

data class OpenGraphData(
    val title: String,
    val image: String?,
    val description: String?,
    val url: String
)

@Composable
fun OpenGraphPreview(url: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var previewData by remember(url) { mutableStateOf<OpenGraphData?>(null) }

    LaunchedEffect(url) {
        val data = withContext(Dispatchers.IO) {
            try {
                val connection = URL(url).openConnection() as HttpURLConnection
                connection.connectTimeout = 4000
                connection.readTimeout = 4000
                connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36")
                connection.inputStream.use { stream ->
                    val buf = ByteArray(128 * 1024) // Read 128KB to fully cover head
                    val bytesRead = stream.read(buf)
                    if (bytesRead > 0) {
                        val html = String(buf, 0, bytesRead, Charsets.UTF_8)
                        val title = extractMetaTag(html, "og:title") 
                            ?: extractMetaTag(html, "title") 
                            ?: extractHTMLTitle(html) 
                            ?: URL(url).host
                        val image = extractMetaTag(html, "og:image")
                        val description = extractMetaTag(html, "og:description") 
                            ?: extractMetaTag(html, "description")
                        OpenGraphData(
                            title = title,
                            image = image,
                            description = description,
                            url = url
                        )
                    } else null
                }
            } catch (e: Exception) {
                // Return fallback based on host name
                try {
                    OpenGraphData(
                        title = URL(url).host,
                        image = null,
                        description = "Cliquez pour ouvrir le lien : $url",
                        url = url
                    )
                } catch (ex: Exception) {
                    null
                }
            }
        }
        previewData = data
    }

    previewData?.let { data ->
        Card(
            modifier = modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
                .clickable {
                    try {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(data.url))
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                },
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            ) {
                if (!data.image.isNullOrBlank()) {
                    AsyncImage(
                        model = data.image,
                        contentDescription = null,
                        modifier = Modifier
                            .size(72.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .clickable {
                                try {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(data.url))
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            },
                        contentScale = ContentScale.Crop
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = data.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (!data.description.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = data.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = Uri.parse(data.url).host ?: data.url,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

private fun extractMetaTag(html: String, property: String): String? {
    try {
        val metaRegex1 = Regex("<meta\\s+[^>]*property=[\"']$property[\"']\\s+[^>]*content=[\"']([^\"']*)[\"']", RegexOption.IGNORE_CASE)
        val metaRegex2 = Regex("<meta\\s+[^>]*content=[\"']([^\"']*)[\"']\\s+[^>]*property=[\"']$property[\"']", RegexOption.IGNORE_CASE)
        val nameRegex1 = Regex("<meta\\s+[^>]*name=[\"']$property[\"']\\s+[^>]*content=[\"']([^\"']*)[\"']", RegexOption.IGNORE_CASE)
        val nameRegex2 = Regex("<meta\\s+[^>]*content=[\"']([^\"']*)[\"']\\s+[^>]*name=[\"']$property[\"']", RegexOption.IGNORE_CASE)
        
        return metaRegex1.find(html)?.groupValues?.get(1)
            ?: metaRegex2.find(html)?.groupValues?.get(1)
            ?: nameRegex1.find(html)?.groupValues?.get(1)
            ?: nameRegex2.find(html)?.groupValues?.get(1)
    } catch (e: Exception) {
        return null
    }
}

private fun extractHTMLTitle(html: String): String? {
    try {
        val titleRegex = Regex("<title>([^<]*)</title>", RegexOption.IGNORE_CASE)
        return titleRegex.find(html)?.groupValues?.get(1)?.trim()
    } catch (e: Exception) {
        return null
    }
}
