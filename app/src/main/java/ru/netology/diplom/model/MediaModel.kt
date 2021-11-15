package ru.netology.diplom.model

import android.net.Uri
import ru.netology.diplom.dto.AttachmentType
import java.io.File

data class MediaModel(
    val uri: Uri? = null,
    val file: File? = null,
    val type: AttachmentType? = null

)
