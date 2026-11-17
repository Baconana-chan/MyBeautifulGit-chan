package org.baconana.mybeautifulgit_chan

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import org.baconana.mybeautifulgit_chan.ui.mockup.MockupApp
import org.jetbrains.compose.reload.DevelopmentEntryPoint

@Composable
@Preview
@DevelopmentEntryPoint
fun App() {
    MaterialTheme {
        MockupApp()
    }
}
