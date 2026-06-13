package com.example.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.foundation.text.ClickableText

@Composable
fun FormattedText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.bodyMedium,
    color: Color = Color.Unspecified,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip,
    onHashtagClick: ((String) -> Unit)? = null,
    onMentionClick: ((String) -> Unit)? = null
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.tertiary

    val annotatedString = buildAnnotatedString {
        var lastIndex = 0
        val regex = Regex("(@[\\\\w.]+)|(#[\\\\w.]+)")
        val matches = regex.findAll(text)
        for (match in matches) {
            append(text.substring(lastIndex, match.range.first))
            if (match.value.startsWith("@")) {
                withStyle(style = SpanStyle(color = primaryColor, fontWeight = FontWeight.Bold)) {
                    pushStringAnnotation(tag = "mention", annotation = match.value)
                    append(match.value)
                    pop()
                }
            } else {
                withStyle(style = SpanStyle(color = secondaryColor, fontWeight = FontWeight.Bold)) {
                    pushStringAnnotation(tag = "hashtag", annotation = match.value)
                    append(match.value)
                    pop()
                }
            }
            lastIndex = match.range.last + 1
        }
        append(text.substring(lastIndex))
    }

    ClickableText(
        text = annotatedString,
        modifier = modifier,
        style = style.copy(color = color),
        maxLines = maxLines,
        overflow = overflow,
        onClick = { offset ->
            annotatedString.getStringAnnotations(tag = "hashtag", start = offset, end = offset)
                .firstOrNull()?.let { annotation ->
                    onHashtagClick?.invoke(annotation.item)
                }
            annotatedString.getStringAnnotations(tag = "mention", start = offset, end = offset)
                .firstOrNull()?.let { annotation ->
                    onMentionClick?.invoke(annotation.item)
                }
        }
    )
}
