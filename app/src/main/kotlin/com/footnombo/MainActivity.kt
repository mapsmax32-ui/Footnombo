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
        private const val MODEL_ASSET = "footnombo_reference_player.glb"
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
        surface.setZOrderOnTop(false)
        surface.setZOrderMediaOverlay(false)
        surface.isClickable = false
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

    private fun attachHairToHead(body: FilamentAsset, hair: FilamentAsset) {
        val tm = modelViewer.engine.transformManager
        val headNames = setOf(
            "Head", "head", "HEAD",
            "mixamorig:Head", "mixamorig_Head",
            "Armature_Head", "head_joint", "Head_JNT"
        )
        var headEntity = 0
        for (entity in body.entities) {
            val name = body.getName(entity) ?: continue
            if (headNames.any { name.equals(it, ignoreCase = true) || name.contains(it, ignoreCase = true) }) {
                headEntity = entity
                break
            }
        }

        val target = if (headEntity != 0) headEntity else body.root
        val targetTransform = FloatArray(16)
        tm.getTransform(tm.getInstance(target), targetTransform)

        // Hair GLB is authored around its own root. Put that root on the player's head,
        // then compensate for the head-node origin so the mesh sits on the scalp.
        tm.setTransform(tm.getInstance(hair.root), targetTransform)

        // Keep the hair asset from inheriting any body-root offset that caused the
        // previous floating-at-the-neck behaviour.
        Log.d(TAG, "Hair attached to " + (body.getName(target) ?: "body-root"))
    }

    private fun selectHair(style: String) {
        val asset = modelViewer.asset ?: return
        val wanted = when (style) {
            "Короткая классика" -> "Hair_SimpleParted"
            "Фейд" -> "Hair_Buzzed"
            "Длинные волосы" -> "Hair_Long"
            "Пучки" -> "Hair_Buns"
            else -> "Hair_SimpleParted"
        }
        val rm = modelViewer.engine.renderableManager
        for (entity in asset.entities) {
            if (!rm.hasComponent(entity)) continue
            val name = asset.getName(entity) ?: ""
            if (name.startsWith("Hair_")) {
                rm.setLayerMask(rm.getInstance(entity), 0x1, if (name.startsWith(wanted)) 0x1 else 0x0)
            }
        }
    }

    private fun loadModel() {
        Thread {
            try {
                val bytes = context.assets.open(MODEL_ASSET).use { it.readBytes() }
                post {
                    if (!::modelViewer.isInitialized || !isAttachedToWindow) return@post
                    modelViewer.loadModelGlb(ByteBuffer.wrap(bytes))
                    modelViewer.transformToUnitCube()
                    selectHair(player.hair)
                    applySkinTone(player.skin)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Bundled 3D player load failed", e)
                post {
                    Toast.makeText(context, "Не удалось загрузить 3D-модель игрока", Toast.LENGTH_LONG).show()
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
        base("ПРОФИЛЬ ИГРОКА")
        val panel = Color.rgb(22,30,44)
        val muted = Color.rgb(155,169,190)
        val blue = Color.rgb(68,128,255)
        val form = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18),dp(12),dp(18),dp(18))
            background = GradientDrawable().apply {
                cornerRadius = dp(20).toFloat()
                setColor(Color.rgb(14,20,31))
                setStroke(dp(1),Color.rgb(42,54,75))
            }
        }
        form.addView(tv("СОЗДАНИЕ ФУТБОЛИСТА",22f,true))
        form.addView(tv("Заполни данные, чтобы начать карьеру.",14f).apply { setTextColor(muted) })
        fun section(t:String) {
            form.addView(tv(t,14f,true).apply {
                setTextColor(Color.rgb(220,229,243))
                setPadding(0,dp(14),0,dp(5))
            })
        }
        fun input(h:String,v:String,numeric:Boolean=false)=EditText(this).apply {
            hint=h; setText(v); textSize=16f; setSingleLine(true)
            setTextColor(Color.WHITE); setHintTextColor(muted)
            inputType=if(numeric) android.text.InputType.TYPE_CLASS_NUMBER else android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
            setPadding(dp(12),0,dp(12),0)
            background=GradientDrawable().apply {
                cornerRadius=dp(11).toFloat();setColor(panel)
                setStroke(dp(1),Color.rgb(54,68,92))
            }
        }
        fun cell(label:String,v:View)=LinearLayout(this).apply {
            orientation=LinearLayout.VERTICAL
            addView(tv(label,11f,true).apply { setTextColor(muted);setPadding(dp(2),0,0,dp(5)) })
            addView(v,LinearLayout.LayoutParams(-1,dp(48)))
        }
        fun pair(left:LinearLayout,right:LinearLayout) {
            form.addView(LinearLayout(this).apply {
                orientation=LinearLayout.HORIZONTAL
                addView(left,LinearLayout.LayoutParams(0,-2,1f))
                addView(right,LinearLayout.LayoutParams(0,-2,1f).apply { leftMargin=dp(10) })
            })
            form.addView(View(this),LinearLayout.LayoutParams(1,dp(7)))
        }
        section("01  ЛИЧНЫЕ ДАННЫЕ")
        val first=input("Имя","Алекс")
        val last=input("Фамилия","Морозов")
        pair(cell("ИМЯ",first),cell("ФАМИЛИЯ",last))
        val age=input("18","18",true)
        val birth=input("Город, страна","Москва, Россия")
        pair(cell("ВОЗРАСТ",age),cell("МЕСТО РОЖДЕНИЯ",birth))
        section("02  ИГРОВЫЕ ДАННЫЕ")
        val number=input("10","10",true)
        val pos=Spinner(this).apply {
            adapter=ArrayAdapter(this@MainActivity,android.R.layout.simple_spinner_dropdown_item,
                arrayOf("Центральный атакующий полузащитник","Центральный полузащитник","Правый вингер","Левый вингер","Центральный нападающий","Центральный защитник","Правый защитник","Левый защитник","Вратарь"))
            background=GradientDrawable().apply { cornerRadius=dp(11).toFloat();setColor(panel);setStroke(dp(1),Color.rgb(54,68,92)) }
        }
        pair(cell("НОМЕР ФУТБОЛКИ",number),cell("ПОЗИЦИЯ",pos))
        section("03  ФИЗИЧЕСКИЕ ДАННЫЕ")
        val height=input("180","180",true)
        val weight=input("72","72",true)
        pair(cell("РОСТ, СМ",height),cell("ВЕС, КГ",weight))
        val submit=Button(this).apply {
            text="СОЗДАТЬ ИГРОКА   →";isAllCaps=false;textSize=16f;setTextColor(Color.WHITE)
            background=GradientDrawable().apply { cornerRadius=dp(12).toFloat();setColor(blue) }
            minHeight=dp(54)
            setOnClickListener {
                val a=age.text.toString().toIntOrNull()
                val n=number.text.toString().toIntOrNull()
                val h=height.text.toString().toIntOrNull()
                val w=weight.text.toString().toIntOrNull()
                val bad=when {
                    first.text.toString().isBlank()->first
                    last.text.toString().isBlank()->last
                    birth.text.toString().isBlank()->birth
                    a==null || a !in 16..45->age
                    n==null || n !in 1..99->number
                    h==null || h !in 140..220->height
                    w==null || w !in 40..150->weight
                    else->null
                }
                if(bad!=null) {
                    bad.error="Проверь значение"
                    bad.requestFocus()
                    Toast.makeText(this@MainActivity,"Проверь заполненные данные",Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                p.name=first.text.toString().trim()+" "+last.text.toString().trim()
                p.age=a!!;p.number=n!!;p.height=h!!;p.weight=w!!
                val birthplace = birth.text.toString().trim()
                p.country = when {
                    birthplace.endsWith("Россия", true) -> "Россия"
                    birthplace.endsWith("Нидерланды", true) -> "Нидерланды"
                    birthplace.endsWith("Германия", true) -> "Германия"
                    birthplace.endsWith("Бразилия", true) -> "Бразилия"
                    birthplace.endsWith("Аргентина", true) -> "Аргентина"
                    birthplace.endsWith("Франция", true) -> "Франция"
                    birthplace.endsWith("Испания", true) -> "Испания"
                    birthplace.endsWith("Англия", true) || birthplace.endsWith("Великобритания", true) -> "Англия"
                    else -> "Россия"
                }
                p.position=when(pos.selectedItemPosition) {
                    0->"ЦАП";1->"ЦП";2->"ПВ";3->"ЛВ";4->"ЦФ";5->"ЦЗ";6->"ПЗ";7->"ЛЗ";else->"ВР"
                }
                showAcademySelection()
            }
        }
        form.addView(submit,LinearLayout.LayoutParams(-1,dp(54)).apply { topMargin=dp(12) })
        val scroll=ScrollView(this).apply { isFillViewport=true;addView(form) }
        content.addView(scroll,LinearLayout.LayoutParams(-1,-1))
    }

    private fun nav(){
        val bar=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL;gravity=Gravity.CENTER}
        bar.addView(button("👤 Профиль"){showProfile()},LinearLayout.LayoutParams(0,dp(48),1f))
        bar.addView(button("👥 Состав"){showSquad()},LinearLayout.LayoutParams(0,dp(48),1f))
        bar.addView(button("🏆 Лига"){showLeague()},LinearLayout.LayoutParams(0,dp(48),1f))
        bar.addView(button("🏋 Тренировка"){showTraining()},LinearLayout.LayoutParams(0,dp(48),1f))
        bar.addView(button("⚽ Матч"){showMatch()},LinearLayout.LayoutParams(0,dp(48),1f))
        root.addView(bar)
    }

    private fun leagueTeams(): List<String> = when (p.country) {
        "Россия" -> listOf("Зенит", "Краснодар", "Динамо Москва", "Локомотив Москва", "Спартак Москва", "ЦСКА Москва", "Рубин", "Ростов", "Крылья Советов", "Ахмат", "Оренбург", "Пари НН", "Урал", "Факел", "Балтика", "Сочи")
        "Нидерланды" -> listOf("ПСВ", "Фейеноорд", "Аякс", "АЗ Алкмар", "Твенте", "Утрехт", "Гоу Эхед Иглс", "Спарта Роттердам", "Херенвен", "Фортуна Ситтард", "НЕК", "Зволле", "Алмере Сити", "Виллем II", "Гронинген", "Валвейк")
        "Германия" -> listOf("Бавария", "Боруссия Дортмунд", "Байер 04", "РБ Лейпциг", "Штутгарт", "Айнтрахт Франкфурт", "Хоффенхайм", "Фрайбург", "Вольфсбург", "Майнц 05", "Вердер", "Аугсбург", "Боруссия Мёнхенгладбах", "Унион Берлин", "Санкт-Паули", "Хайденхайм")
        "Бразилия" -> listOf("Фламенго", "Палмейрас", "Ботафого", "Атлетико Минейро", "Сан-Паулу", "Флуминенсе", "Гремио", "Интернасьонал", "Коринтианс", "Сантос", "Васко да Гама", "Баия", "Крузейро", "Форталеза", "Ред Булл Брагантино", "Атлетико Паранаэнсе")
        "Аргентина" -> listOf("Ривер Плейт", "Бока Хуниорс", "Расинг", "Индепендьенте", "Сан-Лоренсо", "Эстудиантес", "Ланус", "Велес Сарсфилд", "Тальерес", "Росарио Сентраль", "Ньюэллс Олд Бойз", "Аргентинос Хуниорс", "Банфилд", "Уракан", "Платенсе", "Бельграно")
        "Франция" -> listOf("Пари Сен-Жермен", "Монако", "Марсель", "Лион", "Лилль", "Ренн", "Ланс", "Ницца", "Страсбур", "Тулуза", "Нант", "Монпелье", "Осер", "Анже", "Гавр", "Реймс")
        "Испания" -> listOf("Реал Мадрид", "Барселона", "Атлетико Мадрид", "Атлетик Бильбао", "Реал Сосьедад", "Вильярреал", "Бетис", "Севилья", "Валенсия", "Жирона", "Осасуна", "Хетафе", "Сельта", "Мальорка", "Эспаньол", "Леганес")
        "Англия" -> listOf("Арсенал", "Астон Вилла", "Борнмут", "Брентфорд", "Брайтон", "Бернли", "Челси", "Кристал Пэлас", "Эвертон", "Фулхэм", "Лидс Юнайтед", "Ливерпуль", "Манчестер Сити", "Манчестер Юнайтед", "Ньюкасл", "Ноттингем Форест", "Сандерленд", "Тоттенхэм", "Вест Хэм", "Вулверхэмптон")
        else -> listOf("Аякс", "Бавария", "Барселона", "Арсенал", "Пари Сен-Жермен", "Ривер Плейт", "Зенит", "Фламенго")
    }

    private fun nextOpponentClub(): String {
        val opponents = leagueTeams().filter { it != p.club }
        return if (opponents.isEmpty()) "Соперник не найден" else opponents[Random.nextInt(opponents.size)]
    }

    private fun squadPlayers(club: String): List<String> = when (club) {
"Астон Вилла" -> listOf("Джон Макгинн • ЦП", "Бубакар Камара • ОПЗ", "Эмилиано Буэндия • АПЗ", "Николас Джексон • ЦФ", "Алехандро Гарначо • ЛВ", "Тэмми Абрахам • ЦФ", "Пау Торрес • ЦЗ", "Эмилиано Мартинес • ВР")
        "Брентфорд" -> listOf("Каоимин Келлехер • ВР", "Аарон Хики • ПЗ", "Рико Генри • ЛЗ", "Сепп ван ден Берг • ЦЗ", "Этан Пиннок • ЦЗ", "Йегор Ярмолюк • ЦП", "Кевин Шаде • ЛВ", "Матиас Йенсен • ЦП", "Игор Тьяго • ЦФ", "Данго Уаттара • ПВ", "Каллум Уилсон • ЦФ")
        "Реал Мадрид" -> listOf("Тибо Куртуа • ВР", "Антонио Рюдигер • ЦЗ", "Дани Карвахаль • ПЗ", "Федерико Вальверде • ЦП", "Джуд Беллингем • АПЗ", "Винисиус Жуниор • ЛВ", "Килиан Мбаппе • ЦФ", "Родриго • ПВ")
        "Барселона" -> listOf("Марк-Андре тер Штеген • ВР", "Рональд Араухо • ЦЗ", "Жюль Кунде • ПЗ", "Педри • ЦП", "Гави • ЦП", "Ламин Ямаль • ПВ", "Рафинья • ЛВ", "Роберт Левандовски • ЦФ")
        "Манчестер Сити" -> listOf("Джанлуиджи Доннарумма • ВР", "Рубен Диаш • ЦЗ", "Йошко Гвардиол • ЦЗ", "Матеуш Нунес • ПЗ", "Родри • ОПЗ", "Фил Фоден • АПЗ", "Эрлинг Холанд • ЦФ", "Омар Мармуш • НП", "Савио • ПВ", "Райан Шерки • АПЗ")
        "Арсенал" -> listOf("Давид Райя • ВР", "Уильям Салиба • ЦЗ", "Крис Мосquera • ЦЗ", "Бен Уайт • ПЗ", "Пьеро Инкапье • ЛЗ", "Габриэл Магальяйнс • ЦЗ", "Деклан Райс • ОПЗ", "Мартин Эдегор • АПЗ", "Эберечи Эзе • АПЗ", "Букайо Сака • ПВ", "Виктор Дьёкереш • ЦФ", "Кай Хаверц • ЦФ")
        "Ливерпуль" -> listOf("Алисон • ВР", "Вирджил ван Дейк • ЦЗ", "Трент Александер-Арнольд • ПЗ", "Алексис Мак Аллистер • ЦП", "Доминик Собослаи • ЦП", "Мохамед Салах • ПВ", "Коди Гакпо • ЛВ", "Диогу Жота • ЦФ")
        "Бавария" -> listOf("Мануэль Нойер • ВР", "Ким Мин Джэ • ЦЗ", "Дайо Упамекано • ЦЗ", "Йозуа Киммих • ЦП", "Джамал Мусиала • АПЗ", "Лерой Сане • ПВ", "Харри Кейн • ЦФ", "Серж Гнабри • ЛВ")
        "Боруссия Дортмунд" -> listOf("Грегор Кобель • ВР", "Нико Шлоттербек • ЦЗ", "Эмре Джан • ОПЗ", "Юлиан Брандт • АПЗ", "Карим Адейеми • ЛВ", "Серу Гирасси • ЦФ", "Дониэлл Мален • ПВ")
        "Пари Сен-Жермен" -> listOf("Джанлуиджи Доннарумма • ВР", "Маркиньос • ЦЗ", "Ашраф Хакими • ПЗ", "Витинья • ЦП", "Уоррен Заир-Эмери • ЦП", "Усман Дембеле • ПВ", "Брэдли Баркола • ЛВ", "Гонсалу Рамуш • ЦФ")
        "Аякс" -> listOf("Ремко Пасвеер • ВР", "Йоррел Хато • ЦЗ", "Девейн Ренсх • ПЗ", "Джордан Хендерсон • ЦП", "Кеннет Тейлор • ЦП", "Брайан Бробби • ЦФ", "Бертран Траоре • ПВ", "Стивен Бергхёйс • АПЗ")
        "Зенит" -> listOf("Евгений Латышонок • ВР", "Нино • ЦЗ", "Вячеслав Караваев • ПЗ", "Вильмар Барриос • ОПЗ", "Вендел • ЦП", "Максим Глушенков • АПЗ", "Педро • ПВ", "Александр Соболев • ЦФ")
        "Спартак Москва" -> listOf("Александр Максименко • ВР", "Срджан Бабич • ЦЗ", "Даниил Денисов • ПЗ", "Кристофер Мартинс • ЦП", "Роман Зобнин • ЦП", "Эсекьель Барко • АПЗ", "Манфред Угальде • ЦФ", "Тео Бонгонда • ПВ")
        "Ривер Плейт" -> listOf("Франко Армани • ВР", "Пауло Диас • ЦЗ", "Гонсало Монтиэль • ПЗ", "Энцо Перес • ОПЗ", "Игнасио Фернандес • ЦП", "Факундо Колидио • ЦФ", "Мигель Борха • ЦФ", "Франко Мастантуоно • АПЗ")
        "Фламенго" -> listOf("Агустин Росси • ВР", "Лео Перейра • ЦЗ", "Данило • ЦЗ", "Эрик Пульгар • ОПЗ", "Де Арраскаэта • АПЗ", "Жерсон • ЦП", "Педро • ЦФ", "Бруно Энрике • ЛВ")
        else -> listOf("Основной вратарь", "Центральный защитник", "Правый защитник", "Опорный полузащитник", "Центральный полузащитник", "Атакующий полузащитник", "Правый вингер", "Центральный нападающий")
    }

    private fun showSquad() {
        base("Состав клуба")
        content.addView(tv(p.club, 24f, true))
        content.addView(tv("Состав сезона 2026/27 • данные на 10.10.2026", 14f))
        squadPlayers(p.club).forEachIndexed { i, player ->
            val c = card()
            c.orientation = LinearLayout.HORIZONTAL
            c.gravity = Gravity.CENTER_VERTICAL
            c.addView(tv((i + 1).toString().padStart(2, '0'), 16f, true).apply { setTextColor(Color.rgb(112,170,255)) }, LinearLayout.LayoutParams(dp(34), -2))
            c.addView(tv(player, 16f, true), LinearLayout.LayoutParams(0, -2, 1f))
            content.addView(c)
        }
        content.addView(tv("Составы сверяются с опубликованными заявками сезона 2026/27; список в игре охватывает ключевых футболистов, а не всю заявку.", 12f))
        content.addView(button("← Вернуться в карьеру") { showCareer() })
        nav()
    }

    private fun showLeague() {
        base("Таблица лиги")
        val teams = leagueTeams().distinct()
        content.addView(tv("Лига • " + p.country, 22f, true))
        content.addView(tv("Начало сезона " + p.season + " • у всех команд 0 очков", 13f))
        val header = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; setPadding(dp(6),dp(8),dp(6),dp(8)) }
        header.addView(tv("#", 11f, true), LinearLayout.LayoutParams(dp(24), -2))
        header.addView(tv("КЛУБ", 11f, true), LinearLayout.LayoutParams(0, -2, 1f))
        listOf("И","В","Н","П","О").forEach { h -> header.addView(tv(h, 11f, true).apply { gravity = Gravity.CENTER }, LinearLayout.LayoutParams(dp(25), -2)) }
        content.addView(header)
        teams.forEachIndexed { index, team ->
            val c = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(dp(6),dp(7),dp(6),dp(7))
                background = GradientDrawable().apply {
                    cornerRadius = dp(8).toFloat()
                    setColor(if (team == p.club) Color.rgb(30,58,87) else if (index % 2 == 0) Color.rgb(28,38,34) else Color.rgb(21,29,26))
                }
                layoutParams = LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(3) }
            }
            c.addView(tv((index + 1).toString(), 13f, true), LinearLayout.LayoutParams(dp(24), -2))
            c.addView(tv(team, 12f, team == p.club), LinearLayout.LayoutParams(0, -2, 1f))
            repeat(5) { c.addView(tv("0", 12f).apply { gravity = Gravity.CENTER }, LinearLayout.LayoutParams(dp(25), -2)) }
            content.addView(c)
        }
        content.addView(button("← Вернуться в карьеру") { showCareer() })
        nav()
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
        val opponent = nextOpponentClub(); val e=card();e.addView(tv("Следующий матч",17f,true));e.addView(tv(p.club+" — "+opponent));content.addView(e)
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
        base("Матч");val c=card();c.addView(tv(p.club+" — "+nextOpponentClub(),20f,true));c.addView(tv("Домашний матч • "+p.country+" • сезон 2026/27"))
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
