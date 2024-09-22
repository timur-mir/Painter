package home.howework.painter.hostplace


import android.Manifest
import android.app.Activity
import android.app.Dialog
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import home.howework.painter.DoodleView
import home.howework.painter.R
import java.io.IOException
import java.util.concurrent.atomic.AtomicBoolean


class PainterActivity : AppCompatActivity() {
    private var doodleView // создание View
            : DoodleView? = null
    private var sensorManager // отслеживание акселерометра
            : SensorManager? = null
    private var acceleration // ускорение
            = 0f
    private var currentAcceleration // текущее ускорение
            = 0f
    private var lastAcceleration // последнее ускорение
            = 0f
    private val dialogIsDisplayed = AtomicBoolean()

    // ложь
    private val RESULT_LOAD_IMAGE = 1

    // создание идентификаторов для каждого элемента меню
    private val COLOR_MENU_ID = Menu.FIRST
    private val WIDTH_MENU_ID = Menu.FIRST + 1
    private val ERASE_MENU_ID = Menu.FIRST + 2
    private val CLEAR_MENU_ID = Menu.FIRST + 3
    private val SAVE_MENU_ID = Menu.FIRST + 4
    private val OPEN_MENU_ID = Menu.FIRST + 5
    private val UNDO_MENU_ID = Menu.FIRST + 6

    // значение, используемое для идентификации удара устройства
    private val ACCELERATION_THRESHOLD = 15000

    // переменная, которая ссылается на диалоговые окна Choose Color
    // либо Choose Line Width
    private var currentDialog: Dialog? = null

    private val launcher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { map ->
            if (map.values.all { it }) {
                Toast.makeText(
                  this,
                    "Теперь разрешения на запись есть",
                    Toast.LENGTH_LONG
                ).show()

            } else {
                Toast.makeText(this, "Нет разрешения на запись", Toast.LENGTH_LONG)
                    .show()
            }

        }
    private val changeImage =
        registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) {
            if (it.resultCode == Activity.RESULT_OK) {
                val data = it.data
                if (data != null
                    && data.getData() != null
                ) {
                    val selectedImageUri = data.getData();

                    val selectedImageBitmap: Bitmap
                    try {
                        selectedImageBitmap = MediaStore.Images.Media.getBitmap(
                            this.getContentResolver(),
                            selectedImageUri
                        )

                        doodleView!!.setImage(selectedImageBitmap)
                    } catch (e: IOException) {
                        e.printStackTrace();
                    }
                }
            }
        }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        checkPermission()
        doodleView = findViewById(R.id.doodleView);
        acceleration = 0.00f;
        currentAcceleration = SensorManager.GRAVITY_EARTH;
        lastAcceleration = SensorManager.GRAVITY_EARTH;
        enableAccelerometerListening(); // прослушивания тряски
    }
    fun OpenAndSetImageFromGallery(){
        val pickImg = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.INTERNAL_CONTENT_URI)
        changeImage.launch(pickImg)
    }
    private fun checkPermission() {
        val isAllGranted = REQUEST_PERMISSIONS.all { permission ->
            ContextCompat.checkSelfPermission(
                this,
                permission
            ) == PackageManager.PERMISSION_GRANTED
        }
        if (isAllGranted) {
            Toast.makeText(this, "Есть разрешения на запись", Toast.LENGTH_LONG).show()

        } else {
            launcher.launch(REQUEST_PERMISSIONS)
        }


    }
    override fun onPause() {
        super.onPause()
        disableAccelerometerListening() // не прослушивать тряску
    } // конец метода onPause

    private fun enableAccelerometerListening() {
        // инициализация SensorManager
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        sensorManager!!.registerListener(
            sensorEventListener,
            sensorManager!!.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
            SensorManager.SENSOR_DELAY_NORMAL
        )
    } // конец метода enableAccelerometerListening

    // отключение прослушивания событий акселерометра
    private fun disableAccelerometerListening() {
        // прекращение прослушивания событий сенсора
        if (sensorManager != null) {
            sensorManager!!.unregisterListener(
                sensorEventListener,
                sensorManager!!.getDefaultSensor(
                    SensorManager.SENSOR_ACCELEROMETER
                )
            )
            sensorManager = null
        } // конец блока if
    } // конец описания метода disableAccelerometerListening

   val sensorEventListener = object:SensorEventListener{
       override fun onSensorChanged(event: SensorEvent?) {
           // предотвращение отображения других диалоговых окон
           // предотвращение отображения других диалоговых окон
           if (!dialogIsDisplayed.get()) {
               // получение значений x, y и z для SensorEvent
               val x = event!!.values[0]
               val y = event.values[1]
               val z = event.values[2]

               // сохранение предыдущего значения ускорения
               lastAcceleration = currentAcceleration

               // вычисление текущего ускорения
               currentAcceleration = x * x + y * y + z * z

               // вычисление изменения ускорения
               acceleration = currentAcceleration *
                       (currentAcceleration - lastAcceleration)

               // если ускорение превышает определенный уровень
               if (acceleration > ACCELERATION_THRESHOLD) {
                   // создание нового AlertDialog Builder
                   val builder: AlertDialog.Builder = object :AlertDialog.Builder(this@PainterActivity){}

                   // создание сообщения AlertDialog
                   builder.setMessage(R.string.message_erase)
                   builder.setCancelable(true)

                   // добавление кнопки Erase
                   builder.setPositiveButton(
                       R.string.button_erase,
                       DialogInterface.OnClickListener { dialog, id ->
                           dialogIsDisplayed.set(false)
                           doodleView!!.clear() // очистка экрана
                       } // конец метода onClick
                       // конец анонимного внутреннего класса
                   ) // завершение вызова setPositiveButton

                   // добавление кнопки Cancel
                   builder.setNegativeButton(
                       R.string.button_cancel,
                       DialogInterface.OnClickListener { dialog, id ->
                           dialogIsDisplayed.set(false)
                           dialog.cancel() // скрытие диалогового окна
                       } // конец метода onClick
                       // конец анонимного внутреннего класса
                   ) // завершение вызова setNegativeButton
                   dialogIsDisplayed.set(true) // диалоговое окно,
                   // отображаемое на экране
                   builder.show() // отображение диалогового окна
               } // конец блока if
           } // конец блока if

       }

       override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
           TODO("Not yet implemented")
       }

   }
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        super.onCreateOptionsMenu(menu) // вызов метода суперкласса

        // добавление параметров в меню
        menu.add(
            Menu.NONE, COLOR_MENU_ID, Menu.NONE,
            R.string.menuitem_color
        )
        menu.add(
            Menu.NONE, WIDTH_MENU_ID, Menu.NONE,
            R.string.menuitem_line_width
        )
        menu.add(
            Menu.NONE, ERASE_MENU_ID, Menu.NONE,
            R.string.menuitem_erase
        )
        menu.add(
            Menu.NONE, CLEAR_MENU_ID, Menu.NONE,
            R.string.menuitem_clear
        )
        menu.add(
            Menu.NONE, SAVE_MENU_ID, Menu.NONE,
            R.string.menuitem_save_image
        )
        menu.add(
            Menu.NONE, OPEN_MENU_ID, Menu.NONE,
            R.string.menuitem_open_image
        )
        menu.add(
            Menu.NONE, UNDO_MENU_ID, Menu.NONE,
            R.string.menuitem_undo
        )
        return true // обработано создание параметров меню
    } // завершение метода onCreateOptionsMenu

    // обработка выбранных параметров меню
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // оператор switch, использующий MenuItem id
        when (item.itemId) {
            COLOR_MENU_ID -> {
                showColorDialog() // диалоговое окно выбора цвета
                return true // результат обработки события меню
            }

            WIDTH_MENU_ID -> {
                showLineWidthDialog() // диалоговое окно выбора
                // толщины линии
                return true // результат обработки события меню
            }

            ERASE_MENU_ID -> {
                doodleView!!.setDrawingColor(Color.WHITE) // белый цвет линии
                return true // результат обработки события меню
            }

            CLEAR_MENU_ID -> {
                doodleView!!.clear() // очистка doodleView
                return true // результат обработки события меню
            }

            SAVE_MENU_ID -> {
                doodleView!!.saveImage() // сохранение текущих изображений
                return true // результат обработки события меню
            }

            OPEN_MENU_ID -> {
             OpenAndSetImageFromGallery()
                return true // результат обработки события меню
            }
          UNDO_MENU_ID -> {
              doodleView?.clearLastPath()
              doodleView?.postInvalidate()
                return true // результат обработки события меню
            }
        }
        return super.onOptionsItemSelected(item) // вызов метода
        // суперкласса
    } // конец метода onOptionsItemSelected

    // отображает диалоговое окно выбора цвета
    private fun showColorDialog() {
        // создание диалогового окна и «раздувание» его содержимого
        currentDialog = Dialog(this)
        currentDialog!!.setContentView(R.layout.color_dialog)
        currentDialog!!.setTitle(R.string.title_color_dialog)
        currentDialog!!.setCancelable(true)

        // получение ползунков SeekBar цвета и настройка
        // их слушателей onChange
        val alphaSeekBar = currentDialog!!.findViewById<View>(R.id.alphaSeekBar) as SeekBar
        val redSeekBar = currentDialog!!.findViewById<View>(R.id.redSeekBar) as SeekBar
        val greenSeekBar = currentDialog!!.findViewById<View>(R.id.greenSeekBar) as SeekBar
        val blueSeekBar = currentDialog!!.findViewById<View>(R.id.blueSeekBar) as SeekBar

        // регистрация слушателей событий SeekBar
        alphaSeekBar.setOnSeekBarChangeListener(colorSeekBarChanged)
        redSeekBar.setOnSeekBarChangeListener(colorSeekBarChanged)
        greenSeekBar.setOnSeekBarChangeListener(colorSeekBarChanged)
        blueSeekBar.setOnSeekBarChangeListener(colorSeekBarChanged)

        // использование текущего цвета рисунка для выбора значений SeekBar
        val color = doodleView!!.getDrawingColor()
        alphaSeekBar.progress = Color.alpha(color)
        redSeekBar.progress = Color.red(color)
        greenSeekBar.progress = Color.green(color)
        blueSeekBar.progress = Color.blue(color)

        // настройка слушателей кнопки onClickListeneset для класса Color
        val setColorButton = currentDialog!!.findViewById<View>(R.id.setColorButton) as Button
        setColorButton.setOnClickListener(setColorButtonListener)
        dialogIsDisplayed.set(true) // диалоговое окна на экране
        currentDialog!!.show() // отображение диалогового окна
    } // конец метода showColorDialog
    private val colorSeekBarChanged: OnSeekBarChangeListener = object : OnSeekBarChangeListener {
        override fun onProgressChanged(
            seekBar: SeekBar, progress: Int,
            fromUser: Boolean
        ) {
            // получение компонентов SeekBar и colorView LinearLayout
            val alphaSeekBar = currentDialog!!.findViewById<View>(R.id.alphaSeekBar) as SeekBar
            val redSeekBar = currentDialog!!.findViewById<View>(R.id.redSeekBar) as SeekBar
            val greenSeekBar = currentDialog!!.findViewById<View>(R.id.greenSeekBar) as SeekBar
            val blueSeekBar = currentDialog!!.findViewById<View>(R.id.blueSeekBar) as SeekBar
            val colorView = currentDialog!!.findViewById<View>(R.id.colorView) as View

            // отображение текущего цвета
            colorView.setBackgroundColor(
                Color.argb(
                    alphaSeekBar.progress, redSeekBar.progress,
                    greenSeekBar.progress, blueSeekBar.progress
                )
            )
        } // конец метода onProgressChanged

        // требуется указать метод для интерфейса OnSeekBarChangeListener
        override fun onStartTrackingTouch(seekBar: SeekBar) {} // конец метода onStartTrackingTouch

        // метод, требуемый для интерфейса OnSeekBarChangeListener
        override fun onStopTrackingTouch(seekBar: SeekBar) {} // конец метода onStopTrackingTouch
    } // конец colorSeekBarChanged

    // Интерфейс OnClickListener, используемый для выбора цвета
    // после выбора кнопки Set Color в диалоговом окне
    private val setColorButtonListener = View.OnClickListener {
        // получение цвета SeekBar
        val alphaSeekBar = currentDialog!!.findViewById<View>(R.id.alphaSeekBar) as SeekBar
        val redSeekBar = currentDialog!!.findViewById<View>(R.id.redSeekBar) as SeekBar
        val greenSeekBar = currentDialog!!.findViewById<View>(R.id.greenSeekBar) as SeekBar
        val blueSeekBar = currentDialog!!.findViewById<View>(R.id.blueSeekBar) as SeekBar

        // выбор цвета линии
        doodleView!!.setDrawingColor(
            Color.argb(
                alphaSeekBar.progress, redSeekBar.progress,
                greenSeekBar.progress, blueSeekBar.progress
            )
        )
        dialogIsDisplayed.set(false) // диалоговое окно не на экране
        currentDialog!!.dismiss() // скрытие диалогового окна
        currentDialog = null // диалоговое окно не нужно
    } // конец метода onClick
    // конец определения интерфейса setColorButtonListener
    // отображение диалогового окна, в котором выбирается толщина линии
    private  fun showLineWidthDialog() {
        // создание диалогового окна и «раздувание» его содержимого
        currentDialog = Dialog(this)
        currentDialog!!.setContentView(R.layout.width_dialog)
        currentDialog!!.setTitle(R.string.title_line_width_dialog)
        currentDialog!!.setCancelable(true)
        // получение widthSeekBar и его конфигурирование
        val widthSeekBar = currentDialog!!.findViewById<View>(R.id.widthSeekBar) as SeekBar
        widthSeekBar.setOnSeekBarChangeListener(widthSeekBarChanged)
        widthSeekBar.progress = doodleView!!.getLineWidth()

        // Настройка onClickListener для кнопки Set Line Width
        val setLineWidthButton =
            currentDialog!!.findViewById<View>(R.id.widthDialogDoneButton) as Button
        setLineWidthButton.setOnClickListener(setLineWidthButtonListener)
        dialogIsDisplayed.set(true) // диалоговое окно отображается
        // на экране
        currentDialog!!.show() // отображение диалогового окна
    } // конец метода showLineWidthDialog

    // Интерфейс OnSeekBarChangeListener для компонента
    // SeekBar в диалоговом окне width
    private val widthSeekBarChanged: OnSeekBarChangeListener = object : OnSeekBarChangeListener {
        var bitmap = Bitmap.createBitmap(400, 100, Bitmap.Config.ARGB_8888)
        var canvas = Canvas(bitmap) // связывание

        // с объектом Canvas
        override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
            // получение ImageView
            val widthImageView =
                currentDialog!!.findViewById<View>(R.id.widthImageView) as ImageView

            // конфигурирование объекта Paint для текущего значения SeekBar
            val p = Paint()
            p.color = doodleView!!.getDrawingColor()
            p.strokeCap = Paint.Cap.ROUND
            p.strokeWidth = progress.toFloat()

            // очистка растра и перерисовывание линии
            bitmap.eraseColor(Color.WHITE)
            canvas.drawLine(30f, 50f, 370f, 50f, p)
            widthImageView.setImageBitmap(bitmap)
        } // конец определения метода onProgressChanged

        // метод, требуемый для интерфейса OnSeekBarChangeListener
        override fun onStartTrackingTouch(seekBar: SeekBar) {} // завершение определения метода onStartTrackingTouch

        // метод, требуемый для интерфейса OnSeekBarChangeListener
        override fun onStopTrackingTouch(seekBar: SeekBar) {} // конец определения метода onStopTrackingTouch
    }

    // Интерфейс OnClickListener, выполняющий настройку ширины линии
    // после щелчка на кнопке Set Line Width
    private val setLineWidthButtonListener = View.OnClickListener {
        // получение цвета с помощью SeekBar
        val widthSeekBar = currentDialog!!.findViewById<View>(R.id.widthSeekBar) as SeekBar

        // настройка цвета линии
        doodleView!!.setLineWidth(widthSeekBar.progress)
        dialogIsDisplayed.set(false) // диалоговое окно не на экране
        currentDialog!!.dismiss() // скрытие диалогового окна
        currentDialog = null // диалоговое окно не нужно
    } // конец описания метода onClick
    // конец описания интерфейса setColorButtonListener


    companion object {

        private val REQUEST_PERMISSIONS: Array<String> = buildList {

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                add(Manifest.permission.ACCESS_MEDIA_LOCATION)
            }
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.S_V2) {
                add(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.READ_MEDIA_IMAGES)
            }

        }.toTypedArray()


    }
    object PainterHelper {
            var bitmapChange=false
    }
}