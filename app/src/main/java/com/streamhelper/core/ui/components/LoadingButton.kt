package com.streamhelper.core.ui.components

import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** RN `AppButton` with `isLoading`: shows a spinner and ignores taps while [isLoading]. */
@Composable
fun LoadingButton(
    text: String,
    isLoading: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Button(onClick = onClick, enabled = !isLoading, modifier = modifier) {
        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
        } else {
            Text(text)
        }
    }
}
