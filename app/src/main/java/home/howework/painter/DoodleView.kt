package home.howework.painter

import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Point
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.AttributeSet
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.widget.Toast
import home.howework.painter.hostplace.PainterActivity.PainterHelper.bitmapChange
import java.io.File
import java.util.Date
import java.util.Objects


class DoodleView : View {
    // определяется, переместил ли пользователь палец на расстояние,
    // достаточное для повторного рисования
    private val TOUCH_TOLERANCE = 10f

    //    private var bitmap // область для отображения или сохранения
//            : Bitmap? = null
    private lateinit var bitmap: Bitmap
    private var mBitmap // область для отображения или сохранения
            : Bitmap? = null
    lateinit var bitmapCopy: Bitmap
    private var bitmapCanvas // рисование на растре
            : Canvas? = null
    private var paintScreen // рисование растра на экране
            : Paint? = null
    private var paintLine // рисование линий на растре
            : Paint? = null
    private var pathMap // рисование текущего Paths
            : HashMap<Int, Path>? = null
    private var previousPointMap // текущие Points
            : HashMap<Int, Point>? = null
            companion object{
                var lastLineId=0
                private var pathMapCopy
                        :ArrayList< HashMap<Int, Path> > =ArrayList<HashMap<Int, Path>>()
            }
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs) {
        paintScreen = Paint() // применяется для отображения

        // растра на экране

        // настройка начальных настроек отображения
        // для нарисованной линии
        // растра на экране

        // настройка начальных настроек отображения
        // для нарисованной линии
        paintLine = Paint()
        // paintLine.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        paintLine!!.setAntiAlias(true) // сглаживание краев

        // нарисованной линии
        // нарисованной линии
        paintLine!!.setColor(Color.BLACK) // по умолчанию выбран черный

        paintLine!!.setStyle(Paint.Style.STROKE) // сплошная линия

        paintLine!!.setStrokeWidth(5f) // настраивается заданная

        // по умолчанию ширина линии
        // по умолчанию ширина линии
        paintLine!!.setStrokeCap(Paint.Cap.ROUND) // скругленные концы

        // линии
        // линии
        pathMap = HashMap()
        previousPointMap = HashMap()
    }

    // Метод onSizeChanged создает BitMap и Canvas
    // после отображения приложения
    override fun onSizeChanged(w: Int, h: Int, oldW: Int, oldH: Int) {
bitmap = Bitmap.createBitmap(
            width, height,
            Bitmap.Config.ARGB_8888
        )
        bitmapCanvas = Canvas(bitmap)
        bitmap.eraseColor(Color.WHITE)


    } // конец описания метода onSizeChanged

    fun clear() {
        pathMap!!.clear() // удаление всех контуров
        previousPointMap!!.clear() // удаление всех предыдущих точек
        bitmap.eraseColor(Color.WHITE)
        bitmapChange=false
        bitmap = Bitmap.createBitmap(
            width, height,
            Bitmap.Config.ARGB_8888
        )
        bitmapCanvas = Canvas(bitmap)
        bitmapCanvas!!.drawBitmap(bitmap, 0f, 0f, null)
        paintLine!!.color = Color.BLACK
        invalidate() // обновление экрана
    } // завершение описания метода clear

    // настройка цвета нарисованной линии

    fun setDrawingColor(color: Int) {
        paintLine!!.color = color
    } // завершение описания метода setDrawingColor


    // возврат цвета нарисованной линии
    fun getDrawingColor(): Int {
        return paintLine!!.color
    } // завершение описания метода getDrawingColor


    // выбор толщины нарисованной линии
    fun setLineWidth(width: Int) {
        paintLine!!.strokeWidth = width.toFloat()
    } // завершение описания метода setLineWidth


    // возврат толщины нарисованной линии
    fun getLineWidth(): Int {
        return paintLine!!.strokeWidth.toInt()
    } // завершение описания метода getLineWidth

    override fun onDraw(canvas: Canvas) {
        if (::bitmapCopy.isInitialized && bitmapChange) {
            canvas.drawBitmap(bitmapCopy!!, 0f, 0f, null)
        } else {
            canvas.drawBitmap(bitmap, 0f, 0f, null)
        }
        // для каждого только что нарисованного контура
        for (key in pathMap!!.keys)
            canvas.drawPath(
            pathMap!![key]!!,
            paintLine!!
        ) // рисование линии
    } // завершение описания метода onDraw

    fun setImage(bitmapImage: Bitmap) {
        bitmapChange=true
        mBitmap = bitmapImage
        if (mBitmap != null) {
            bitmapCopy = mBitmap!!.copy(Bitmap.Config.ARGB_8888, true)
            bitmapCanvas = Canvas(bitmapCopy)
            bitmapCanvas!!.drawBitmap(bitmapCopy, 0f, 0f, null)
            invalidate() // обновление экрана
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        // получение типа события и идентификатора указателя,
        // который вызвал событие
        val action = event.actionMasked // тип события
        val actionIndex = event.actionIndex // указатель (палец)
        // определение типа действия для данного MotionEvent
        // представляет, затем вызывает соответствующий метод обработки
        if (action == MotionEvent.ACTION_DOWN ||
            action == MotionEvent.ACTION_POINTER_DOWN
        ) {
            touchStarted(
                event.getX(actionIndex),
                event.getY(actionIndex),
                event.getPointerId(actionIndex)
            )

        } // завершение описания блока if
        else if (action == MotionEvent.ACTION_UP ||
            action == MotionEvent.ACTION_POINTER_UP
        ) {
            touchEnded(event.getPointerId(actionIndex))
        } // конец описания блока else if
        else {
            touchMoved(event)
        } // конец описания блока else
        invalidate() // перерисовывание
        return true // использование события касания
    } // конец описания метода onTouchEvent

    // вызывается после касания пользователем экрана
    private fun touchStarted(x: Float, y: Float, lineID: Int) {
        val path: Path? // применяется для хранения контура для
        // идентификатора данного прикосновения
        val point: Point? // применяется для хранения последней точки контура
        // если уже есть контур для идентификатора lineID
        if (pathMap!!.containsKey(lineID)) {
            path = pathMap!![lineID] // получение контура
            path!!.reset() // переустановка контура из-за нового
            // прикосновения
            point = previousPointMap!![lineID] // последняя точка контура
        } // конец блока f
        else {
            path = Path() // создание нового контура
            pathMap!![lineID] = path // добавление контура в карту
            pathMapCopy.add(pathMap!!)
            lastLineId=  lineID
            point = Point() // создание новой точки
            previousPointMap!![lineID] = point // добавление точки
            // на карту
        } // конец блока else

        // перемещение координат прикосновения
        path!!.moveTo(x, y)
        point!!.x = x.toInt()
        point.y = y.toInt()
    } // конец описания метода touchStarted

    // вызывается при выполнении перетаскивания в области экрана
    private fun touchMoved(event: MotionEvent) {
        // для каждого из указателей в данном MotionEvent
        for (i in 0 until event.pointerCount) {
            // получение идентификатора и индекса указателя
            val pointerID = event.getPointerId(i)
            val pointerIndex = event.findPointerIndex(pointerID)
            // если имеется контур, связанный с указателем
            if (pathMap!!.containsKey(pointerID)) {
                // получение новых координат указателя
                val newX = event.getX(pointerIndex)
                val newY = event.getY(pointerIndex)

                // получение контура и предыдущей точки, связанных
                // с этим указателем
                val path = pathMap!![pointerID]
                val point = previousPointMap!![pointerID]

                // вычисление перемещения от точки последнего обновления
                val deltaX = Math.abs(newX - point!!.x)
                val deltaY = Math.abs(newY - point.y)

                // если расстояние достаточно велико
                if (deltaX >= TOUCH_TOLERANCE || deltaY >= TOUCH_TOLERANCE) {
                    // перемещение контура в новое место
                    path!!.quadTo(
                        point.x.toFloat(), point.y.toFloat(), (newX + point.x) / 2,
                        (newY + point.y) / 2
                    )

                    // хранение новых координат
                    point.x = newX.toInt()
                    point.y = newY.toInt()
                } // конец определения блока if
            } // конец блока if
        } // конец блока for
    } // конец определения метода touchMoved

    private fun touchEnded(lineID: Int) {
        val path = pathMap!![lineID] // получение соответствующего сегмента

        bitmapCanvas!!.drawPath(path!!, paintLine!!) // рисует bitmapCanvas
       path.reset() // переустановка контура
    }
    fun clearLastPath(){
        pathMap?.remove(lastLineId)
                if(!bitmapChange) {
                    bitmap = Bitmap.createBitmap(
                        width, height,
                        Bitmap.Config.ARGB_8888
                    )
                    bitmapCanvas = Canvas(bitmap)
                    bitmapCanvas!!.drawBitmap(bitmap, 0f, 0f, null)
                    for (key in pathMap!!.keys) {
                        bitmapCanvas!!.drawPath(
                            pathMap!![key]!!,
                            paintLine!!

                        )
                    }
                        invalidate()

                    }

        else
                {
                    bitmapCopy = mBitmap!!.copy(Bitmap.Config.ARGB_8888, true)
                    bitmapCanvas = Canvas(bitmapCopy)
                    bitmapCanvas!!.drawBitmap(bitmapCopy, 0f, 0f, null)
                    for (key in pathMap!!.keys) {
                        bitmapCanvas!!.drawPath(pathMap!![key]!!, paintLine!!)
                    }

                   invalidate()
                }

    }


    // save the current image to the Gallery
    fun saveImage() {

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            var date: Date = Date()
            val name = "Рисунок_"+"$date" + ".jpg"

            // insert the image on the device
            if (::bitmapCopy.isInitialized) {
                val location = MediaStore.Images.Media.insertImage(
                    context.contentResolver, bitmapCopy, name,
                    "Painter Drawing"
                )
                if (location != null) {
                    // display a message indicating that the image was saved
                    val message: Toast = Toast.makeText(
                        context,
                        R.string.message_saved,
                        Toast.LENGTH_SHORT
                    )
                    message.setGravity(
                        Gravity.CENTER, message.xOffset / 2,
                        message.yOffset / 2
                    )
                    message.show()
                } else {
                    // display a message indicating that there was an error saving
                    val message: Toast = Toast.makeText(
                        context,
                        R.string.message_error_saving, Toast.LENGTH_SHORT
                    )
                    message.setGravity(
                        Gravity.CENTER, message.xOffset / 2,
                        message.yOffset / 2
                    )
                    message.show()
                }
            } else {
                val location = MediaStore.Images.Media.insertImage(
                    context.contentResolver, bitmap, name,
                    "Painter Drawing"
                )
                if (location != null) {
                    // display a message indicating that the image was saved
                    val message: Toast = Toast.makeText(
                        context,
                        R.string.message_saved,
                        Toast.LENGTH_SHORT
                    )
                    message.setGravity(
                        Gravity.CENTER, message.xOffset / 2,
                        message.yOffset / 2
                    )
                    message.show()
                } else {
                    // display a message indicating that there was an error saving
                    val message: Toast = Toast.makeText(
                        context,
                        R.string.message_error_saving, Toast.LENGTH_SHORT
                    )
                    message.setGravity(
                        Gravity.CENTER, message.xOffset / 2,
                        message.yOffset / 2
                    )
                    message.show()
                }
            }
        } else {
            val resolver: ContentResolver? = context?.contentResolver
            var date: Date = Date()
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {

                    val contentValues = ContentValues()
                    contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, "Рисунок_$date.jpg")
                    contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                    contentValues.put(
                        MediaStore.MediaColumns.RELATIVE_PATH,
                        Environment.DIRECTORY_PICTURES + File.separator + "PainterFolder"
                    )
                    val imageUri =
                        resolver?.insert(
                            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                            contentValues
                        )

                    val fos =
                        Objects.requireNonNull(imageUri)?.let { resolver?.openOutputStream(it) }
                    if (::bitmapCopy.isInitialized) {
                        bitmapCopy.compress(Bitmap.CompressFormat.JPEG, 100, fos)
                    }
                    else
                    {
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos)
                    }
                    Objects.requireNonNull(fos)
                    Toast.makeText(context, "Изображение сохранено в папке художника)", Toast.LENGTH_SHORT).show()

                }
            } catch (e: Exception) {
                Toast.makeText(
                    context,
                    " Не удалось сохранить изображение по причине:\n$e",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

    }

}