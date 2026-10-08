package com.footnombo

import android.app.Activity
import android.os.Bundle
import android.graphics.*
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.view.Choreographer
import android.view.SurfaceView
import android.util.Log
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.nio.ByteBuffer
import com.google.android.filament.utils.ModelViewer
import com.google.android.filament.Colors
import com.google.android.filament.utils.Utils
import com.google.android.filament.EntityManager
import com.google.android.filament.LightManager
import com.google.android.filament.utils.Manipulator
import com.google.android.filament.gltfio.AssetLoader
import com.google.android.filament.gltfio.FilamentAsset
import com.google.android.filament.gltfio.ResourceLoader
import com.google.android.filament.gltfio.UbershaderProvider
import android.widget.*
import kotlin.math.roundToInt
import kotlin.random.Random

data class Player(
    var name: String = "Алекс Морозов",
    var position: String = "ЦАП",
    var number: Int = 10,
    var height: Int = 180,
    var weight: Int = 72,
    var age: Int = 18,
    var country: String = "Россия",
    var skin: String = "Средняя",
    var hair: String = "Тёмная",
    var attack: Int = 72,
    var pass: Int = 75,
    var speed: Int = 68,
    var physical: Int = 61,
    var morale: Int = 78,
    var energy: Int = 82,
    var trust: Int = 55,
    var matches: Int = 0,
    var goals: Int = 0,
    var assists: Int = 0,
    var season: Int = 1,
    var week: Int = 1,
    var money: Int = 1200,
    var contractWeeks: Int = 24,
    var club: String = "FC North City",
    var academy: String = "FC North City",
    var injuryWeeks: Int = 0,
    var reputation: Int = 20
) { val overall get() = ((attack + pass + speed + physical) / 4.0).roundToInt() }

class PlayerPreviewView(context: Activity, private var player: Player) : FrameLayout(context) {
    companion object {
        private const val MODEL_URL = "https://raw.githubusercontent.com/kendrekaran/striker-3d/main/assets/player.glb"
        private const val CACHE_NAME = "footnombo_male_soccer_player.glb"
        private const val HAIR_URL = "https://raw.githubusercontent.com/Quizball-trivia/web/c1d268834751bcc405054176b45e784eda6b6379/public/assets/demos/score/player-hair.glb"
        private const val HAIR_CACHE_NAME = "footnombo_real_hair_styles.glb"
        private const val TAG = "Footnombo3D"
        init { Utils.init() }
    }

    private val surface = SurfaceView(context)
    private lateinit var modelViewer: ModelViewer
    private var hairAsset: FilamentAsset? = null
    private var hairLoader: AssetLoader? = null
    private var hairResourceLoader: ResourceLoader? = null
    private val choreographer = Choreographer.getInstance()
    private var framePosted = false

    init {
        setBackgroundColor(Color.rgb(8, 18, 14))
        addView(surface, LayoutParams(-1, -1))
        surface.setOnTouchListener { _, event ->
            if (::modelViewer.isInitialized) modelViewer.onTouchEvent(event)
            true
        }
        post { initViewer() }
    }

    fun update(height: Int, weight: Int, skin: String, hair: String, position: String) {
        player.height = height
        player.weight = weight
        player.skin = skin
        player.hair = hair
        player.position = position
        if (::modelViewer.isInitialized) { applySkinTone(skin); selectHair(hair) }
    }

    private fun initViewer() {
        if (::modelViewer.isInitialized) return
        val manipulator = Manipulator.Builder()
            .targetPosition(0.0f, 0.0f, -4.0f)
            .orbitHomePosition(0.0f, 0.05f, -1.65f)
            .viewport(width.coerceAtLeast(1), height.coerceAtLeast(1))
            .zoomSpeed(0.025f)
            .orbitSpeed(0.008f, 0.008f)
            .build(Manipulator.Mode.ORBIT)
        modelViewer = ModelViewer(surface, manipulator = manipulator)
        modelViewer.camera.setExposure(16.0f, 1.0f / 125.0f, 180.0f)
        modelViewer.view.renderQuality = modelViewer.view.renderQuality.apply {
            hdrColorBuffer = com.google.android.filament.View.QualityLevel.HIGH
        }
        modelViewer.view.multiSampleAntiAliasingOptions =
            modelViewer.view.multiSampleAntiAliasingOptions.apply { enabled = true }
        modelViewer.view.antiAliasing = com.google.android.filament.View.AntiAliasing.FXAA
        modelViewer.view.ambientOcclusionOptions =
            modelViewer.view.ambientOcclusionOptions.apply { enabled = true }
        val clear = modelViewer.renderer.clearOptions
        clear.clear = true
        clear.clearColor = doubleArrayOf(0.16, 0.23, 0.20, 1.0)
        modelViewer.renderer.clearOptions = clear
        val light = EntityManager.get().create()
        LightManager.Builder(LightManager.Type.DIRECTIONAL)
            .color(1.0f, 0.95f, 0.90f)
            .intensity(120000.0f)
            .direction(0.35f, -1.0f, -0.55f)
            .castShadows(true)
            .build(modelViewer.engine, light)
        modelViewer.scene.addEntity(light)
        val fill = EntityManager.get().create()
        LightManager.Builder(LightManager.Type.DIRECTIONAL)
            .color(0.72f, 0.82f, 1.0f)
            .intensity(65000.0f)
            .direction(-0.55f, -0.35f, 0.65f)
            .build(modelViewer.engine, fill)
        modelViewer.scene.addEntity(fill)
        startFrames()
        loadModel()
    }

    private fun startFrames() {
        if (framePosted) return
        framePosted = true
        choreographer.postFrameCallback(frameCallback)
    }

    private val frameCallback = object : Choreographer.FrameCallback {
        override fun doFrame(frameTimeNanos: Long) {
            if (!isAttachedToWindow) {
                framePosted = false
                return
            }
            if (::modelViewer.isInitialized) modelViewer.render(frameTimeNanos)
            choreographer.postFrameCallback(this)
        }
    }

    private fun applySkinTone(skin: String = player.skin) {
        if (!::modelViewer.isInitialized || modelViewer.asset == null) return
        val rgb = when (skin) {
            "Очень светлая" -> floatArrayOf(0.96f, 0.78f, 0.66f)
            "Светлая" -> floatArrayOf(0.86f, 0.64f, 0.48f)
            "Средняя" -> floatArrayOf(0.68f, 0.45f, 0.30f)
            "Смуглая" -> floatArrayOf(0.48f, 0.29f, 0.18f)
            "Тёмная" -> floatArrayOf(0.25f, 0.13f, 0.075f)
            else -> floatArrayOf(0.68f, 0.45f, 0.30f)
        }
        try {
            val rm = modelViewer.engine.renderableManager
            val asset = modelViewer.asset!!
            for (entity in asset.entities) {
                if (!rm.hasComponent(entity)) continue
                val instance = rm.getInstance(entity)
                val primitiveCount = rm.getPrimitiveCount(instance)
                for (i in 0 until primitiveCount) {
                    val materialInstance = rm.getMaterialInstanceAt(instance, i) ?: continue
                    val materialName = materialInstance.material.name
                    if (materialName.equals("Skin", ignoreCase = true) ||
                        materialName.contains("skin", ignoreCase = true)) {
                        materialInstance.setParameter(
                            "baseColor",
                            Colors.RgbType.SRGB,
                            rgb[0], rgb[1], rgb[2]
                        )
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Skin material update failed", e)
        }
    }

    private fun selectHair(style: String) {
        val asset = hairAsset ?: return
        val wanted = when (style) {
            "Короткая классика" -> "Hair_SimpleParted"
            "Фейд", "Высокий фейд", "Короткий ёжик" -> "Hair_Buzzed"
            "Длинные волосы" -> "Hair_Long"
            "Пучки" -> "Hair_Buns"
            else -> "Hair_SimpleParted"
        }
        val rm = modelViewer.engine.renderableManager
        for (entity in asset.entities) {
            if (!rm.hasComponent(entity)) continue
            val name = asset.getName(entity) ?: ""
            rm.setLayerMask(rm.getInstance(entity), 0x1, if (name == wanted) 0x1 else 0x0)
        }
    }

    private fun loadHairStyles() {
        Thread {
            try {
                val file = File(context.cacheDir, HAIR_CACHE_NAME)
                if (!file.exists() || file.length() < 100_000L) {
                    val connection = (URL(HAIR_URL).openConnection() as HttpURLConnection).apply {
                        connectTimeout = 20_000
                        readTimeout = 45_000
                        requestMethod = "GET"
                        instanceFollowRedirects = true
                    }
                    connection.connect()
                    if (connection.responseCode !in 200..299) throw IllegalStateException("Hair HTTP " + connection.responseCode)
                    connection.inputStream.use { input -> file.outputStream().use { output -> input.copyTo(output, 64 * 1024) } }
                    connection.disconnect()
                }
                val bytes = file.readBytes()
                post {
                    if (!::modelViewer.isInitialized || !isAttachedToWindow) return@post
                    try {
                        val engine = modelViewer.engine
                        val loader = AssetLoader(engine, UbershaderProvider(engine), EntityManager.get())
                        val resources = ResourceLoader(engine)
                        val asset = loader.createAsset(ByteBuffer.wrap(bytes)) ?: throw IllegalStateException("Hair asset parse failed")
                        hairLoader = loader
                        hairResourceLoader = resources
                        hairAsset = asset
                        modelViewer.scene.addEntities(asset.entities)
                        resources.asyncBeginLoad(asset)
                        asset.releaseSourceData()
                        modelViewer.asset?.let { body ->
                            val tm = engine.transformManager
                            tm.setTransform(tm.getInstance(asset.root), tm.getTransform(tm.getInstance(body.root)))
                        }
                        selectHair(player.hair)
                    } catch (e: Exception) { Log.e(TAG, "Hair asset load failed", e) }
                }
            } catch (e: Exception) { Log.e(TAG, "Hair download failed", e) }
        }.start()
    }

    private fun loadModel() {
        Thread {
            try {
                val file = File(context.cacheDir, CACHE_NAME)
                if (!file.exists() || file.length() < 100_000L) {
                    val connection = (URL(MODEL_URL).openConnection() as HttpURLConnection).apply {
                        connectTimeout = 20_000
                        readTimeout = 45_000
                        requestMethod = "GET"
                        instanceFollowRedirects = true
                    }
                    connection.connect()
                    if (connection.responseCode !in 200..299) {
                        throw IllegalStateException("HTTP " + connection.responseCode)
                    }
                    connection.inputStream.use { input ->
                        file.outputStream().use { output -> input.copyTo(output, 64 * 1024) }
                    }
                    connection.disconnect()
                }
                val bytes = file.readBytes()
                post {
                    if (!::modelViewer.isInitialized || !isAttachedToWindow) return@post
                    modelViewer.loadModelGlb(ByteBuffer.wrap(bytes))
                    modelViewer.transformToUnitCube()
                    applySkinTone(player.skin)
                    loadHairStyles()
                }
            } catch (e: Exception) {
                Log.e(TAG, "3D player load failed", e)
                post {
                    Toast.makeText(context, "Не удалось загрузить 3D-модель футболиста", Toast.LENGTH_LONG).show()
                }
            }
        }.start()
    }

    override fun onDetachedFromWindow() {
        choreographer.removeFrameCallback(frameCallback)
        framePosted = false
        if (::modelViewer.isInitialized) {
            try {
                hairAsset?.let { asset ->
                    modelViewer.scene.removeEntities(asset.entities)
                    hairResourceLoader?.asyncCancelLoad()
                    hairResourceLoader?.evictResourceData()
                    hairLoader?.destroyAsset(asset)
                }
                hairAsset = null
                hairResourceLoader = null
                hairLoader = null
                modelViewer.destroy()
            } catch (_: Exception) {}
        }
        super.onDetachedFromWindow()
    }
}

class MainActivity : Activity() {
    private val p = Player()
    private lateinit var root: LinearLayout
    private lateinit var content: LinearLayout
    private var trained = false
    private var played = false
    private var eventShown = false
    private var eventTitle = ""
    private var eventText = ""
    private var preview: PlayerPreviewView? = null

    private fun dp(v:Int)= (v*resources.displayMetrics.density).roundToInt()
    private fun tv(t:String,size:Float=16f,bold:Boolean=false)=TextView(this).apply{
        text=t;textSize=size;setTextColor(Color.WHITE);if(bold)setTypeface(typeface,1)
        setPadding(dp(4),dp(5),dp(4),dp(5))
    }
    private fun field(hint:String,value:String,input:Int=0)=EditText(this).apply{
        this.hint=hint
        setText(value)
        setSelection(text.length)
        inputType=if(input==0) android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_FLAG_CAP_SENTENCES else input
        setTextColor(Color.WHITE)
        setHintTextColor(Color.LTGRAY)
        isFocusable=true
        isFocusableInTouchMode=true
        isEnabled=true
        setSingleLine(true)
        setPadding(dp(10),dp(7),dp(10),dp(7))
        background=GradientDrawable().apply{cornerRadius=dp(10).toFloat();setColor(Color.rgb(35,45,41))}
    }
    private fun button(t:String,a:()->Unit)=Button(this).apply{
        text=t;isAllCaps=false;setTextColor(Color.WHITE);textSize=14f;minHeight=dp(46)
        background=GradientDrawable().apply{cornerRadius=dp(12).toFloat();setColor(Color.rgb(24,165,88))}
        setOnClickListener{a()}
    }
    private fun card()=LinearLayout(this).apply{
        orientation=LinearLayout.VERTICAL;setPadding(dp(12),dp(10),dp(12),dp(10))
        background=GradientDrawable().apply{cornerRadius=dp(16).toFloat();setColor(Color.rgb(28,38,34));setStroke(dp(1),Color.rgb(55,70,63))}
        layoutParams=LinearLayout.LayoutParams(-1,-2).apply{setMargins(0,dp(5),0,dp(5))}
    }
    private fun base(t:String){
        root=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(dp(10),dp(8),dp(10),dp(8));setBackgroundColor(Color.rgb(16,22,20))}
        root.addView(tv(t,22f,true))
        content=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL}
        val scroll=ScrollView(this).apply{isFillViewport=true;addView(content)}
        root.addView(scroll,LinearLayout.LayoutParams(-1,0,1f))
        setContentView(root)
    }
    override fun onCreate(b:Bundle?){super.onCreate(b);window.statusBarColor=Color.rgb(16,22,20);window.navigationBarColor=Color.rgb(16,22,20);showCreate()}

    private fun showCreate(){
        base("Создание игрока")

        val editor = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        val previewCard = card()
        preview = PlayerPreviewView(this, p).apply { minimumHeight = dp(330) }
        previewCard.addView(preview, LinearLayout.LayoutParams(-1, dp(330)))
        editor.addView(previewCard)

        val tabs = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, dp(4), 0, dp(4))
        }
        val infoTab = button("ИНФОРМАЦИЯ") {}
        val bodyTab = button("ТЕЛО И КОЖА") {}
        val hairTab = button("ПРИЧЕСКИ") {}
        tabs.addView(infoTab, LinearLayout.LayoutParams(0, dp(48), 1f))
        tabs.addView(bodyTab, LinearLayout.LayoutParams(0, dp(48), 1f))
        tabs.addView(hairTab, LinearLayout.LayoutParams(0, dp(48), 1f))
        editor.addView(tabs)

        val panel = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        editor.addView(panel)

        val first = field("Имя", "Алекс")
        val last = field("Фамилия", "Морозов")
        val country = Spinner(this).apply {
            adapter = ArrayAdapter(this@MainActivity, android.R.layout.simple_spinner_dropdown_item,
                arrayOf("Россия","Нидерланды","Германия","Бразилия","Аргентина","Франция","Испания","Англия"))
        }
        val pos = Spinner(this).apply {
            adapter = ArrayAdapter(this@MainActivity, android.R.layout.simple_spinner_dropdown_item,
                arrayOf("ЦАП","ПВ","ЛВ","ЦФ","ЦП","ЦЗ","ВР"))
        }
        val age = field("Возраст, 16-35", "18", 2)
        val num = field("Номер, 1-99", "10", 2)
        val height = field("Рост, 150-210 см", "180", 2)
        val weight = field("Вес, 45-120 кг", "72", 2)
        val skin = Spinner(this).apply {
            adapter = ArrayAdapter(this@MainActivity, android.R.layout.simple_spinner_dropdown_item,
                arrayOf("Очень светлая","Светлая","Средняя","Смуглая","Тёмная"))
        }

        fun refreshModel() {
            val hh = (height.text.toString().toIntOrNull() ?: 180).coerceIn(150, 210)
            val ww = (weight.text.toString().toIntOrNull() ?: 72).coerceIn(45, 120)
            preview?.update(hh, ww, skin.selectedItem?.toString() ?: "Средняя",
                p.hair, pos.selectedItem?.toString() ?: "ЦАП")
        }

        val infoPanel = card().apply {
            addView(tv("Основная информация", 18f, true))
            addView(first); addView(last)
            addView(tv("Страна", 13f, true)); addView(country)
            addView(tv("Позиция", 13f, true)); addView(pos)
            addView(age); addView(num)
        }
        val bodyPanel = card().apply {
            addView(tv("Параметры тела", 18f, true))
            addView(tv("Рост и вес сразу обновляют параметры персонажа.", 13f))
            addView(height); addView(weight)
            addView(tv("Тон кожи", 13f, true)); addView(skin)
            addView(button("Сбросить тело") {
                height.setText("180")
                weight.setText("72")
                skin.setSelection(2)
                refreshModel()
            })
        }
        val hairPanel = card().apply {
            addView(tv("Выбери причёску", 18f, true))
            addView(tv("Выбранная причёска сразу отображается на превью.", 13f))
            val hairstyles = arrayOf(
                "Короткая классика",
                "Фейд",
                "Длинные волосы",
                "Пучки"
            )
            val grid = GridLayout(this@MainActivity).apply {
                columnCount = 3
                useDefaultMargins = true
            }
            hairstyles.forEach { style ->
                val b = button(style) {
                    p.hair = style
                    preview?.update(
                        (height.text.toString().toIntOrNull() ?: 180).coerceIn(150,210),
                        (weight.text.toString().toIntOrNull() ?: 72).coerceIn(45,120),
                        skin.selectedItem?.toString() ?: "Средняя",
                        style,
                        pos.selectedItem?.toString() ?: "ЦАП"
                    )
                    Toast.makeText(this@MainActivity, "Причёска: $style", Toast.LENGTH_SHORT).show()
                }
                grid.addView(b, GridLayout.LayoutParams().apply {
                    width = 0
                    this.height = dp(58)
                    columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                })
            }
            addView(grid)
        }

        panel.addView(infoPanel)
        panel.addView(bodyPanel)
        panel.addView(hairPanel)

        fun showPanel(which: Int) {
            infoPanel.visibility = if (which == 0) View.VISIBLE else View.GONE
            bodyPanel.visibility = if (which == 1) View.VISIBLE else View.GONE
            hairPanel.visibility = if (which == 2) View.VISIBLE else View.GONE
            refreshModel()
        }

        infoTab.setOnClickListener { showPanel(0) }
        bodyTab.setOnClickListener { showPanel(1) }
        hairTab.setOnClickListener { showPanel(2) }

        listOf(height, weight).forEach {
            it.setOnFocusChangeListener { _, _ -> refreshModel() }
        }
        skin.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) {}
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) { refreshModel() }
        }
        pos.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) {}
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) { refreshModel() }
        }

        content.addView(editor)
        content.addView(button("Начать карьеру →") {
            p.name = (first.text.toString().trim() + " " + last.text.toString().trim()).trim().ifBlank { "Алекс Морозов" }
            p.position = pos.selectedItem.toString()
            p.country = country.selectedItem.toString()
            p.age = (age.text.toString().toIntOrNull() ?: 18).coerceIn(16, 35)
            p.number = (num.text.toString().toIntOrNull() ?: 10).coerceIn(1, 99)
            p.height = (height.text.toString().toIntOrNull() ?: 180).coerceIn(150, 210)
            p.weight = (weight.text.toString().toIntOrNull() ?: 72).coerceIn(45, 120)
            p.skin = skin.selectedItem.toString()
            showAcademySelection()
        })
        showPanel(0)
    }

    private fun nav(){
        val bar=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL;gravity=Gravity.CENTER}
        bar.addView(button("👤 Профиль"){showProfile()},LinearLayout.LayoutParams(0,dp(50),1f))
        bar.addView(button("🏋 Тренировка"){showTraining()},LinearLayout.LayoutParams(0,dp(50),1f))
        bar.addView(button("⚽ Матч"){showMatch()},LinearLayout.LayoutParams(0,dp(50),1f))
        root.addView(bar)
    }
    private fun academyOptions(country: String): List<String> = when (country) {
        "Россия" -> listOf("ЦСКА Москва", "Спартак Москва", "Зенит", "Динамо Москва", "Локомотив Москва", "Краснодар")
        "Нидерланды" -> listOf("Аякс", "ПСВ", "Фейеноорд", "АЗ Алкмар", "Утрехт", "Витесс")
        "Германия" -> listOf("Бавария", "Боруссия Дортмунд", "Шальке 04", "Байер 04", "РБ Лейпциг", "Штутгарт")
        "Бразилия" -> listOf("Фламенго", "Палмейрас", "Сантос", "Сан-Паулу", "Гремио", "Флуминенсе")
        "Аргентина" -> listOf("Ривер Плейт", "Бока Хуниорс", "Расинг", "Индепендьенте", "Сан-Лоренсо", "Ньюэллс Олд Бойз")
        "Франция" -> listOf("Пари Сен-Жермен", "Олимпик Лион", "Олимпик Марсель", "Монако", "Лилль", "Ренн")
        "Испания" -> listOf("Барселона", "Реал Мадрид", "Атлетик Бильбао", "Атлетико Мадрид", "Валенсия", "Севилья")
        "Англия" -> listOf("Арсенал", "Челси", "Манчестер Сити", "Ливерпуль", "Манчестер Юнайтед", "Тоттенхэм")
        else -> listOf("Аякс", "Бавария", "Барселона", "Арсенал", "ПСЖ", "Ривер Плейт")
    }

    private fun showAcademySelection() {
        base("Молодёжная академия")
        val choices = academyOptions(p.country).shuffled().distinct().take(3)

        content.addView(tv("Добро пожаловать в молодёжную карьеру", 24f, true))
        content.addView(tv("Ты выбрал страну: " + p.country + ". Скаутская система предлагает три реальные академии. Выбери клуб, с которого начнётся твоя карьера.", 15f))

        choices.forEachIndexed { index, academy ->
            val c = card()
            c.addView(tv((index + 1).toString() + ". " + academy, 21f, true))
            c.addView(tv("Молодёжная академия • " + p.country))
            c.addView(tv("Стартовый контракт • U19/U21"))
            c.addView(button("Выбрать академию") {
                p.club = academy
                p.academy = academy
                showCareer()
                Toast.makeText(this, "Карьера началась в академии " + academy, Toast.LENGTH_LONG).show()
            })
            content.addView(c)
        }

        content.addView(tv("Каждый новый старт генерирует новую тройку клубов.", 13f))
        content.addView(button("🎲 Перегенерировать три академии") { showAcademySelection() })
    }

    private fun showCareer(){
        base("Моя карьера")
        val c=card();c.addView(tv(p.name,22f,true));c.addView(tv(p.position+" • №"+p.number+" • "+p.age+" лет",14f));c.addView(tv(p.country+" • Молодёжная академия: "+p.academy));c.addView(tv("Общий рейтинг: "+p.overall,20f,true));content.addView(c)
        val s=card();s.addView(tv("Сезон "+p.season+" • Неделя "+p.week,18f,true));s.addView(tv("Матчи: "+p.matches+"   Голы: "+p.goals+"   Голевые: "+p.assists));s.addView(tv("Доверие: "+p.trust+"%   Мораль: "+p.morale+"%   Репутация: "+p.reputation));s.addView(tv("Баланс: €"+p.money+"   Контракт: "+p.contractWeeks+" нед."));content.addView(s)
        if(!eventShown) content.addView(button("📖 Событие недели"){showEvent()})
        val e=card();e.addView(tv("Следующий матч",17f,true));e.addView(tv(p.club+" — Red Falcons"));content.addView(e)
        content.addView(button("⏭ Следующая неделя"){advanceWeek()});nav()
    }
    private fun showEvent(){
        eventShown=true
        val events=listOf("Тренер вызывает тебя" to "Тренер предлагает остаться после занятия и отработать удары.","Интерес скаутов" to "Клуб из соседней лиги прислал запрос о твоём прогрессе.","Разговор с капитаном" to "Капитан советует чаще играть на команду.")
        val e=events[Random.nextInt(events.size)];eventTitle=e.first;eventText=e.second
        base("Событие");content.addView(tv(eventTitle,22f,true));content.addView(tv(eventText,16f))
        content.addView(button("💪 Принять вызов"){p.attack+=2;p.energy=(p.energy-10).coerceAtLeast(0);p.trust=(p.trust+4).coerceAtMost(100);showCareer()})
        content.addView(button("🤝 Играть командно"){p.pass+=2;p.morale=(p.morale+5).coerceAtMost(100);p.reputation++;showCareer()})
        content.addView(button("😴 Отказаться"){p.energy=(p.energy+5).coerceAtMost(100);p.trust=(p.trust-3).coerceAtLeast(0);showCareer()})
    }
    private fun advanceWeek(){p.week++;p.energy=(p.energy+25).coerceAtMost(100);p.contractWeeks--;trained=false;played=false;eventShown=false;if(p.contractWeeks<=0){p.club="Без клуба";p.trust=0};if(p.week>38){p.week=1;p.season++};if(p.injuryWeeks>0)p.injuryWeeks--;showCareer()}
    private fun showProfile(){
        base("Профиль игрока")
        val c=card();c.addView(tv(p.name,24f,true));c.addView(tv(p.position+" • №"+p.number+" • "+p.age+" лет"));c.addView(tv(p.country));c.addView(tv("Рост "+p.height+" см • Вес "+p.weight+" кг"));c.addView(tv("Внешность: "+p.skin+" тон кожи • "+p.hair+" волосы"));c.addView(tv("Рейтинг "+p.overall));c.addView(tv("Атака "+p.attack+" • Пас "+p.pass+" • Скорость "+p.speed+" • Физика "+p.physical));content.addView(c)
        val s=card();s.addView(tv("Карьерная статистика",18f,true));s.addView(tv("Матчи "+p.matches+" • Голы "+p.goals+" • Ассисты "+p.assists));content.addView(s);nav()
    }
    private fun showTraining(){
        base("Тренировки");content.addView(tv("Выбери одну основную тренировку на неделю.",15f))
        listOf("⚽ Завершение" to "attack","🎯 Передачи" to "pass","🏃 Физическая подготовка" to "physical").forEach{pair->
            val c=card();c.addView(tv(pair.first,18f,true));c.addView(tv("Навык +2 • расход энергии 15"))
            c.addView(button("Тренироваться"){
                if(trained)Toast.makeText(this,"На этой неделе тренировка уже выполнена.",Toast.LENGTH_SHORT).show()
                else if(p.energy<15)Toast.makeText(this,"Недостаточно энергии.",Toast.LENGTH_SHORT).show()
                else{when(pair.second){"attack"->p.attack+=2;"pass"->p.pass+=2;"physical"->p.physical+=2};p.energy-=15;trained=true;Toast.makeText(this,"Тренировка завершена! Рейтинг "+p.overall,Toast.LENGTH_SHORT).show();showTraining()}
            });content.addView(c)
        };nav()
    }
    private fun showMatch(){
        base("Матч");val c=card();c.addView(tv(p.club+" — Red Falcons",20f,true));c.addView(tv("Домашний матч • Лига"))
        if(!played){c.addView(tv("Как будешь играть?",17f,true));c.addView(button("🎨 Рисковать"){play("creative")});c.addView(button("⚖ Сбалансированно"){play("balanced")});c.addView(button("🤝 На команду"){play("team")})}
        else c.addView(tv("Матч уже сыгран. Открой профиль для статистики."))
        content.addView(c);nav()
    }
    private fun play(choice:String){
        played=true;p.matches++;var home=Random.nextInt(0,3);var away=Random.nextInt(0,3)
        if(choice=="creative"&&p.attack>70)home++
        if(choice=="team"){p.morale=(p.morale+7).coerceAtMost(100);p.trust=(p.trust+4).coerceAtMost(100);home++}
        if(choice=="balanced"&&p.physical>65)away=(away-1).coerceAtLeast(0)
        if(home>away){p.goals++;p.morale=(p.morale+5).coerceAtMost(100)}else p.morale=(p.morale-2).coerceAtLeast(0)
        p.energy=(p.energy-20).coerceAtLeast(0);showMatch();Toast.makeText(this,"Матч "+home+":"+away+" • "+if(home>away)"Победа!"else if(home==away)"Ничья"else"Поражение",Toast.LENGTH_LONG).show()
    }
}
