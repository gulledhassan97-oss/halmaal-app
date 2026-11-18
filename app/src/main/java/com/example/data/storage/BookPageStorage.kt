package com.example.data.storage

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.Build
import android.os.ParcelFileDescriptor
import android.provider.OpenableColumns
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.core.content.res.ResourcesCompat
import com.example.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class BookPageStorage(private val context: Context) {

    private val storageRoot: File by lazy {
        File(context.filesDir, "storage").apply { if (!exists()) mkdirs() }
    }
    private val rawPdfDir: File by lazy {
        File(storageRoot, "raw_pdfs").apply { if (!exists()) mkdirs() }
    }
    private val pagesRootDir: File by lazy {
        File(storageRoot, "book_pages").apply { if (!exists()) mkdirs() }
    }

    fun getPageFile(bookId: Long, pageNum: Int): File {
        return File(File(pagesRootDir, bookId.toString()), "page_$pageNum.webp")
    }

    fun deleteBookFiles(bookId: Long) {
        File(pagesRootDir, bookId.toString()).deleteRecursively()
        File(rawPdfDir, "book_$bookId.pdf").delete()
    }

    fun resolveDisplayName(uri: Uri): String {
        runCatching {
            context.contentResolver.query(
                uri,
                arrayOf(OpenableColumns.DISPLAY_NAME),
                null,
                null,
                null
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (idx >= 0) {
                        val name = cursor.getString(idx)
                        if (!name.isNullOrBlank()) return name
                    }
                }
            }
        }
        return uri.lastPathSegment?.substringAfterLast('/')?.takeIf { it.isNotBlank() } ?: "uploaded_book.pdf"
    }

    suspend fun rasterizePdfFromUri(
        bookId: Long,
        uri: Uri,
        onProgress: (current: Int, total: Int) -> Unit = { _, _ -> }
    ): Result<Int> = withContext(Dispatchers.IO) {
        val tempPdfFile = File(rawPdfDir, "book_${bookId}_tmp.pdf")
        val tempPagesDir = File(pagesRootDir, "${bookId}_tmp")
        runCatching {
            if (tempPagesDir.exists()) tempPagesDir.deleteRecursively()
            tempPagesDir.mkdirs()

            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(tempPdfFile).use { output ->
                    input.copyTo(output)
                }
            } ?: error("Unable to open selected PDF file.")

            val totalPages = ParcelFileDescriptor.open(tempPdfFile, ParcelFileDescriptor.MODE_READ_ONLY).use { descriptor ->
                PdfRenderer(descriptor).use { renderer ->
                    val total = renderer.pageCount
                    require(total > 0) { "The selected PDF contains 0 pages." }
                    val targetWidth = 1080
                    for (index in 0 until total) {
                        renderer.openPage(index).use { page ->
                            val aspectRatio = page.height.toFloat() / page.width.coerceAtLeast(1).toFloat()
                            val targetHeight = (targetWidth * aspectRatio).toInt().coerceIn(720, 1920)
                            val bitmap = Bitmap.createBitmap(
                                targetWidth,
                                targetHeight,
                                Bitmap.Config.ARGB_8888
                            )
                            val canvas = Canvas(bitmap)
                            canvas.drawColor(Color.WHITE)
                            page.render(
                                bitmap,
                                null,
                                null,
                                PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY
                            )
                            val pageFile = File(tempPagesDir, "page_${index + 1}.webp")
                            compressBitmapToWebp(bitmap, pageFile)
                            bitmap.recycle()
                        }
                        onProgress(index + 1, total)
                    }
                    total
                }
            }

            // Atomically promote staged PDF & pages into live storage
            val finalPdfFile = File(rawPdfDir, "book_$bookId.pdf")
            if (finalPdfFile.exists()) finalPdfFile.delete()
            if (!tempPdfFile.renameTo(finalPdfFile)) {
                tempPdfFile.copyTo(finalPdfFile, overwrite = true)
                tempPdfFile.delete()
            }

            val finalPagesDir = File(pagesRootDir, bookId.toString())
            if (finalPagesDir.exists()) finalPagesDir.deleteRecursively()
            if (!tempPagesDir.renameTo(finalPagesDir)) {
                tempPagesDir.copyRecursively(finalPagesDir, overwrite = true)
                tempPagesDir.deleteRecursively()
            }
            totalPages
        }.onFailure {
            tempPdfFile.delete()
            tempPagesDir.deleteRecursively()
        }
    }

    suspend fun refreshEditorialCover(
        bookId: Long,
        title: String,
        author: String,
        category: String,
        accentColorHex: Long
    ) = withContext(Dispatchers.IO) {
        val bookPagesDir = File(pagesRootDir, bookId.toString()).apply {
            if (!exists()) mkdirs()
        }
        val serifTypeface = ResourcesCompat.getFont(context, R.font.playfair_display) ?: Typeface.SERIF
        val sansTypeface = ResourcesCompat.getFont(context, R.font.plus_jakarta_sans) ?: Typeface.SANS_SERIF
        val width = 900
        val height = 1260
        val coverFile = File(bookPagesDir, "page_1.webp")
        val coverBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(coverBitmap)
        drawEditorialCover(
            canvas = canvas,
            width = width,
            height = height,
            title = title,
            author = author,
            category = category,
            accentColor = accentColorHex.toInt(),
            serifTypeface = serifTypeface,
            sansTypeface = sansTypeface
        )
        compressBitmapToWebp(coverBitmap, coverFile)
        coverBitmap.recycle()
    }

    suspend fun renderManuscriptBookPages(
        bookId: Long,
        title: String,
        author: String,
        description: String,
        category: String,
        accentColorHex: Long,
        manuscriptText: String
    ): Int = withContext(Dispatchers.IO) {
        val paragraphs = manuscriptText
            .split(Regex("\\n\\s*\\n"))
            .map { it.trim() }
            .filter { it.isNotEmpty() }

        val chapters = if (paragraphs.isEmpty()) {
            listOf(
                "Introduction" to description.ifBlank { "Published with Hal-maal Digital Publishing." }
            )
        } else {
            paragraphs.chunked(2).mapIndexed { idx, chunk ->
                val heading = if (idx == 0) "Chapter 1" else "Section ${idx + 1}"
                heading to chunk.joinToString("\n\n")
            }
        }

        val bookPagesDir = File(pagesRootDir, bookId.toString()).apply {
            if (exists()) deleteRecursively()
            mkdirs()
        }
        renderStarterBookPages(
            bookId = bookId,
            title = title,
            author = author,
            description = description,
            category = category,
            accentColorHex = accentColorHex,
            chapters = chapters
        )
    }

    suspend fun renderStarterBookPages(
        bookId: Long,
        title: String,
        author: String,
        description: String,
        category: String,
        accentColorHex: Long,
        chapters: List<Pair<String, String>>
    ): Int = withContext(Dispatchers.IO) {
        val bookPagesDir = File(pagesRootDir, bookId.toString()).apply {
            if (!exists()) mkdirs()
        }

        val serifTypeface = ResourcesCompat.getFont(context, R.font.playfair_display) ?: Typeface.SERIF
        val sansTypeface = ResourcesCompat.getFont(context, R.font.plus_jakarta_sans) ?: Typeface.SANS_SERIF

        val width = 900
        val height = 1260

        // Page 1: Public Cover Preview
        val coverFile = File(bookPagesDir, "page_1.webp")
        if (!coverFile.exists()) {
            val coverBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(coverBitmap)
            drawEditorialCover(
                canvas = canvas,
                width = width,
                height = height,
                title = title,
                author = author,
                category = category,
                accentColor = accentColorHex.toInt(),
                serifTypeface = serifTypeface,
                sansTypeface = sansTypeface
            )
            compressBitmapToWebp(coverBitmap, coverFile)
            coverBitmap.recycle()
        }

        // Pages 2..N: Interior Reading Pages
        chapters.forEachIndexed { idx, (chapterTitle, chapterBody) ->
            val pageNum = idx + 2
            val pageFile = File(bookPagesDir, "page_$pageNum.webp")
            if (!pageFile.exists()) {
                val pageBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(pageBitmap)
                drawEditorialPage(
                    canvas = canvas,
                    width = width,
                    height = height,
                    bookTitle = title,
                    chapterTitle = chapterTitle,
                    bodyText = chapterBody,
                    pageNumber = pageNum,
                    totalPages = chapters.size + 1,
                    accentColor = accentColorHex.toInt(),
                    serifTypeface = serifTypeface,
                    sansTypeface = sansTypeface
                )
                compressBitmapToWebp(pageBitmap, pageFile)
                pageBitmap.recycle()
            }
        }

        chapters.size + 1
    }

    private fun drawEditorialCover(
        canvas: Canvas,
        width: Int,
        height: Int,
        title: String,
        author: String,
        category: String,
        accentColor: Int,
        serifTypeface: Typeface,
        sansTypeface: Typeface
    ) {
        val darkTint = darkenColor(accentColor, 0.55f)
        val bgPaint = Paint().apply {
            shader = LinearGradient(
                0f, 0f, width.toFloat(), height.toFloat(),
                accentColor, darkTint, Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

        // Book spine highlight strip on left
        val spinePaint = Paint().apply {
            color = Color.argb(45, 0, 0, 0)
        }
        canvas.drawRect(0f, 0f, 34f, height.toFloat(), spinePaint)

        val spineLine = Paint().apply {
            color = Color.argb(60, 255, 255, 255)
            strokeWidth = 2f
        }
        canvas.drawLine(36f, 0f, 36f, height.toFloat(), spineLine)

        // Geometric horizon & sun emblem
        val motifPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(38, 255, 255, 255)
            style = Paint.Style.STROKE
            strokeWidth = 3f
        }
        val cx = width * 0.54f
        val cy = height * 0.62f
        canvas.drawCircle(cx, cy, 240f, motifPaint)
        canvas.drawCircle(cx, cy, 175f, motifPaint)

        val goldAccentPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#F59E0B")
            style = Paint.Style.FILL
        }
        canvas.drawCircle(cx, cy, 38f, goldAccentPaint)

        // Frame border
        val framePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(85, 253, 248, 240)
            style = Paint.Style.STROKE
            strokeWidth = 2.5f
        }
        canvas.drawRoundRect(
            RectF(64f, 64f, width - 52f, height - 52f),
            16f, 16f, framePaint
        )

        // Category pill
        val catPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#FDE68A")
            typeface = Typeface.create(sansTypeface, Typeface.BOLD)
            textSize = 26f
            letterSpacing = 0.14f
        }
        canvas.drawText(category.uppercase(), 96f, 145f, catPaint)

        // Title block
        val titlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#FDF8F0")
            typeface = Typeface.create(serifTypeface, Typeface.BOLD)
            textSize = 68f
        }
        val titleLayout = StaticLayout.Builder
            .obtain(title, 0, title.length, titlePaint, width - 190)
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .setLineSpacing(8f, 1.05f)
            .build()
        canvas.save()
        canvas.translate(96f, 200f)
        titleLayout.draw(canvas)
        canvas.restore()

        // Divider
        val rulePaint = Paint().apply {
            color = Color.parseColor("#F59E0B")
            strokeWidth = 5f
        }
        val ruleY = 230f + titleLayout.height
        canvas.drawLine(96f, ruleY, 220f, ruleY, rulePaint)

        // Author
        val authorPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(230, 253, 248, 240)
            typeface = Typeface.create(sansTypeface, Typeface.NORMAL)
            textSize = 34f
        }
        canvas.drawText("By $author", 96f, ruleY + 60f, authorPaint)

        // Hal-maal Imprint Footer
        val imprintPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(200, 255, 255, 255)
            typeface = Typeface.create(serifTypeface, Typeface.BOLD)
            textSize = 28f
        }
        canvas.drawText("HAL-MAAL DIGITAL PUBLISHING", 96f, height - 104f, imprintPaint)
    }

    private fun drawEditorialPage(
        canvas: Canvas,
        width: Int,
        height: Int,
        bookTitle: String,
        chapterTitle: String,
        bodyText: String,
        pageNumber: Int,
        totalPages: Int,
        accentColor: Int,
        serifTypeface: Typeface,
        sansTypeface: Typeface
    ) {
        // Warm ivory reading paper
        canvas.drawColor(Color.parseColor("#FDFBF7"))

        // Subtle top running header
        val headerPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#71717A")
            typeface = Typeface.create(sansTypeface, Typeface.NORMAL)
            textSize = 22f
            letterSpacing = 0.08f
        }
        canvas.drawText(bookTitle.uppercase(), 86f, 88f, headerPaint)

        val dividerPaint = Paint().apply {
            color = Color.parseColor("#E4E0D8")
            strokeWidth = 2f
        }
        canvas.drawLine(86f, 112f, width - 86f, 112f, dividerPaint)

        // Chapter heading
        val chapterPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = accentColor
            typeface = Typeface.create(serifTypeface, Typeface.BOLD)
            textSize = 44f
        }
        val chapterLayout = StaticLayout.Builder
            .obtain(chapterTitle, 0, chapterTitle.length, chapterPaint, width - 172)
            .setLineSpacing(6f, 1.05f)
            .build()
        canvas.save()
        canvas.translate(86f, 165f)
        chapterLayout.draw(canvas)
        canvas.restore()

        val accentRule = Paint().apply {
            color = Color.parseColor("#D97706")
            strokeWidth = 4f
        }
        val bodyTop = 195f + chapterLayout.height
        canvas.drawLine(86f, bodyTop, 170f, bodyTop, accentRule)

        // Body text
        val bodyPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#18181B")
            typeface = Typeface.create(sansTypeface, Typeface.NORMAL)
            textSize = 30f
        }
        val bodyLayout = StaticLayout.Builder
            .obtain(bodyText, 0, bodyText.length, bodyPaint, width - 172)
            .setLineSpacing(14f, 1.15f)
            .build()
        canvas.save()
        canvas.translate(86f, bodyTop + 42f)
        bodyLayout.draw(canvas)
        canvas.restore()

        // Footer page number
        canvas.drawLine(86f, height - 110f, width - 86f, height - 110f, dividerPaint)
        val pageNumPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#52525B")
            typeface = Typeface.create(sansTypeface, Typeface.BOLD)
            textSize = 24f
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(
            "Page $pageNumber of $totalPages",
            width / 2f,
            height - 64f,
            pageNumPaint
        )
    }

    private fun darkenColor(color: Int, factor: Float): Int {
        val r = (Color.red(color) * factor).toInt().coerceIn(0, 255)
        val g = (Color.green(color) * factor).toInt().coerceIn(0, 255)
        val b = (Color.blue(color) * factor).toInt().coerceIn(0, 255)
        return Color.rgb(r, g, b)
    }

    private fun compressBitmapToWebp(bitmap: Bitmap, destination: File) {
        FileOutputStream(destination).use { out ->
            @Suppress("DEPRECATION")
            val format = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                Bitmap.CompressFormat.WEBP_LOSSY
            } else {
                Bitmap.CompressFormat.WEBP
            }
            bitmap.compress(format, 88, out)
        }
    }
}
