package com.example.model

enum class FileTypeCategory(
    val displayName: String,
    val description: String,
    val iconName: String,
    val colorHex: Long
) {
    DOCUMENTS("Documents", "PDFs, Word docs, spreadsheets & text", "Description", 0xFF2563EB),
    IMAGES("Images", "Photos, vectors, artwork & scans", "Image", 0xFF059669),
    VIDEOS("Videos", "Recordings, movies & clips", "Movie", 0xFFD97706),
    AUDIO("Audio", "Voice notes, music & audio files", "Audiotrack", 0xFF7C3AED),
    ARCHIVES("Archives", "ZIP, TAR, GZ & compressed backups", "Archive", 0xFFDC2626),
    CODE_DATA("Code & Data", "JSON, CSV, SQL, code & databases", "Terminal", 0xFF0891B2),
    BACKUPS("Backups", "System snapshots & encrypted archives", "CloudSync", 0xFF4F46E5),
    OTHER("Other", "Miscellaneous binary & unknown files", "FolderZip", 0xFF64748B);

    companion object {
        fun fromMimeOrFilename(mime: String, filename: String): FileTypeCategory {
            val lower = filename.lowercase()
            val lowerMime = mime.lowercase()

            return when {
                lowerMime.startsWith("image/") || lower.endsWith(".jpg") || lower.endsWith(".jpeg") ||
                        lower.endsWith(".png") || lower.endsWith(".webp") || lower.endsWith(".svg") -> IMAGES

                lowerMime.startsWith("video/") || lower.endsWith(".mp4") || lower.endsWith(".mkv") ||
                        lower.endsWith(".mov") || lower.endsWith(".avi") -> VIDEOS

                lowerMime.startsWith("audio/") || lower.endsWith(".mp3") || lower.endsWith(".wav") ||
                        lower.endsWith(".m4a") || lower.endsWith(".flac") -> AUDIO

                lower.endsWith(".zip") || lower.endsWith(".tar") || lower.endsWith(".gz") ||
                        lower.endsWith(".7z") || lower.endsWith(".rar") -> ARCHIVES

                lower.endsWith(".json") || lower.endsWith(".csv") || lower.endsWith(".sql") ||
                        lower.endsWith(".kt") || lower.endsWith(".py") || lower.endsWith(".js") ||
                        lower.endsWith(".html") || lower.endsWith(".xml") -> CODE_DATA

                lower.endsWith(".bak") || lower.endsWith(".dump") || lower.contains("backup") -> BACKUPS

                lowerMime.startsWith("text/") || lower.endsWith(".pdf") || lower.endsWith(".doc") ||
                        lower.endsWith(".docx") || lower.endsWith(".txt") || lower.endsWith(".md") ||
                        lower.endsWith(".xlsx") || lower.endsWith(".pptx") -> DOCUMENTS

                else -> OTHER
            }
        }
    }
}
