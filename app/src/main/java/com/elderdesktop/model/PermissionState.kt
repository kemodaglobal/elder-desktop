package com.elderdesktop.model

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

object PermissionRationaleProvider {
    var rationaleMessage by mutableStateOf<String?>(null)
}
