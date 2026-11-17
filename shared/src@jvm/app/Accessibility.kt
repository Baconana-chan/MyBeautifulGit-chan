package org.baconana.mybeautifulgit_chan.app

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import org.baconana.mybeautifulgit_chan.ui.mockup.a11yButton as sharedButton
import org.baconana.mybeautifulgit_chan.ui.mockup.a11yFocusGroup as sharedFocusGroup
import org.baconana.mybeautifulgit_chan.ui.mockup.a11yFocusRing as sharedFocusRing
import org.baconana.mybeautifulgit_chan.ui.mockup.a11yHeading as sharedHeading
import org.baconana.mybeautifulgit_chan.ui.mockup.a11yLiveRegion as sharedLiveRegion
import org.baconana.mybeautifulgit_chan.ui.mockup.a11yState as sharedState

@Composable
fun Modifier.a11yFocusRing(enabled: Boolean = true): Modifier = sharedFocusRing(enabled)

fun Modifier.a11yButton(
    label: String? = null,
    selected: Boolean? = null,
    role: Role = Role.Button,
    enabled: Boolean = true,
): Modifier = sharedButton(label, selected, role, enabled)

fun Modifier.a11yState(label: String): Modifier = sharedState(label)

fun Modifier.a11yLiveRegion(assertive: Boolean = false): Modifier = sharedLiveRegion(assertive)

fun Modifier.a11yHeading(): Modifier = sharedHeading()

fun Modifier.a11yFocusGroup(): Modifier = sharedFocusGroup()
