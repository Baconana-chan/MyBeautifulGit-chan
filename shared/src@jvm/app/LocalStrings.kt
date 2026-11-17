package org.baconana.mybeautifulgit_chan.app

import androidx.compose.runtime.staticCompositionLocalOf
import org.baconana.mybeautifulgit_chan.i18n.EnglishStrings
import org.baconana.mybeautifulgit_chan.i18n.Strings

/**
 * Текущие тексты интерфейса. Меняется вместе с языком в DesktopApp.
 */
val LocalStrings = staticCompositionLocalOf<Strings> { EnglishStrings }
