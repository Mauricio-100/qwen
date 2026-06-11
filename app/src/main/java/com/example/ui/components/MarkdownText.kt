package com.example.ui.components

import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight

@Composable
fun MarkdownText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = LocalTextStyle.current
) {
    val annotatedString = buildAnnotatedString {
        append(text)
        // Very basic mock Markdown text styles
        // Since we are adding styles after appending, we can just use simple string matching
        
        val boldMatches = Regex("\\*\\*(.*?)\\*\\*").findAll(text)
        for (match in boldMatches) {
            addStyle(SpanStyle(fontWeight = FontWeight.Bold), match.range.first, match.range.last + 1)
        }
    }
    
    Text(text = annotatedString, modifier = modifier, style = style)
}
