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
import com.google.android.filament.utils.Utils
import com.google.android.filament.EntityManager
import com.google.android.filament.LightManager
import com.google.android.filament.RenderableManager
import com.google.android.filament.VertexBuffer
import com.google.android.filament.IndexBuffer
import com.google.android.filament.Material
import com.google.android.filament.filamat.MaterialBuilder
import com.google.android.filament.utils.Manipulator
import java.nio.ByteOrder
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.PI
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
        private const val MODEL_URL = "https://www.innerscene.com/api/library/soccer-player-standing-3d-person-team-sports-f55b1b4f/download"
        private const val CACHE_NAME = "footnombo_soccer_player.glb"
        private const val TAG = "Footnombo3D"
        init { Utils.init() }
    }

    private val surface = SurfaceView(context)
    private lateinit var modelViewer: ModelViewer
    private var hairEntity = 0
    private var hairVertexBuffer: VertexBuffer? = null
    private var hairIndexBuffer: IndexBuffer? = null
    private var hairMaterial: Material? = null
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
        if (::modelViewer.isInitialized) rebuildHair(hair)
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

    private fun ensureHairMaterial() {
        if (hairMaterial != null) return
        MaterialBuilder.init()
        try {
            val pkg = MaterialBuilder()
                .name("FootnomboHair")
                .platform(MaterialBuilder.Platform.MOBILE)
                .shading(MaterialBuilder.Shading.LIT)
                .doubleSided(true)
                .material("""
                    void material(inout MaterialInputs material) {
                        prepareMaterial(material);
                        material.baseColor.rgb = float3(0.035, 0.022, 0.015);
                        material.metallic = 0.0;
                        material.roughness = 0.82;
                    }
                """.trimIndent())
                .optimization(MaterialBuilder.Optimization.NONE)
                .build(modelViewer.engine)
            hairMaterial = Material.Builder().payload(pkg.getBuffer(), pkg.getBuffer().remaining()).build(modelViewer.engine)
        } finally {
            MaterialBuilder.shutdown()
        }
    }

    private fun rebuildHair(style: String) {
        if (!::modelViewer.isInitialized || modelViewer.asset == null) return
        try {
            if (hairEntity != 0) {
                modelViewer.scene.removeEntity(hairEntity)
                modelViewer.engine.renderableManager.destroy(hairEntity)
                modelViewer.engine.transformManager.destroy(hairEntity)
                EntityManager.get().destroy(hairEntity)
                hairEntity = 0
            }
            hairVertexBuffer?.let { modelViewer.engine.destroyVertexBuffer(it) }
            hairIndexBuffer?.let { modelViewer.engine.destroyIndexBuffer(it) }
            hairVertexBuffer = null
            hairIndexBuffer = null
            ensureHairMaterial()

            val positions = ArrayList<Float>()
            val indices = ArrayList<Int>()

            fun addEllipsoid(cx: Float, cy: Float, cz: Float,
                             rx: Float, ry: Float, rz: Float,
                             rings: Int = 8, segs: Int = 24,
                             topOnly: Boolean = false) {
                val base = positions.size / 3
                val startPhi = if (topOnly) 0f else -PI.toFloat() / 2f
                val endPhi = PI.toFloat() / 2f
                for (r in 0..rings) {
                    val phi = startPhi + (endPhi - startPhi) * r / rings
                    val cp = cos(phi)
                    val sp = sin(phi)
                    for (seg in 0 until segs) {
                        val a = seg.toFloat() / segs * 2f * PI.toFloat()
                        positions += cx + cos(a) * rx * cp
                        positions += cy + sp * ry
                        positions += cz + sin(a) * rz * cp
                    }
                }
                for (r in 0 until rings) for (seg in 0 until segs) {
                    val n = (seg + 1) % segs
                    val a = base + r * segs + seg
                    val b = base + r * segs + n
                    val c = base + (r + 1) * segs + seg
                    val d = base + (r + 1) * segs + n
                    indices += a; indices += c; indices += b
                    indices += b; indices += c; indices += d
                }
            }

            fun addHairClump(cx: Float, cy: Float, cz: Float,
                             rx: Float, ry: Float, rz: Float,
                             tilt: Float = 0f) {
                val base = positions.size / 3
                val rings = 5
                val segs = 12
                for (r in 0..rings) {
                    val t = r.toFloat() / rings
                    val y = cy - t * ry
                    val scale = 1f - 0.28f * t
                    val z = cz + tilt * t
                    for (seg in 0 until segs) {
                        val a = seg.toFloat() / segs * 2f * PI.toFloat()
                        positions += cx + cos(a) * rx * scale
                        positions += y
                        positions += z + sin(a) * rz * scale
                    }
                }
                for (r in 0 until rings) for (seg in 0 until segs) {
                    val n = (seg + 1) % segs
                    val a = base + r * segs + seg
                    val b = base + r * segs + n
                    val c = base + (r + 1) * segs + seg
                    val d = base + (r + 1) * segs + n
                    indices += a; indices += c; indices += b
                    indices += b; indices += c; indices += d
                }
            }

            // The player GLB is normalized by ModelViewer to a ~2m tall reference.
            // Keep every hairstyle around the same head anchor so it follows the male model.
            val headY = 1.48f
            val headZ = -0.01f

            when (style) {
                "Короткая классика" -> {
                    addEllipsoid(0f, headY, headZ, 0.145f, 0.16f, 0.155f, topOnly = true)
                    addHairClump(-0.12f, 1.50f, -0.055f, 0.055f, 0.13f, 0.075f, -0.015f)
                    addHairClump(0.12f, 1.50f, -0.055f, 0.055f, 0.13f, 0.075f, -0.015f)
                }
                "Фейд" -> {
                    addEllipsoid(0f, 1.49f, headZ, 0.135f, 0.145f, 0.145f, topOnly = true)
                    addEllipsoid(0f, 1.43f, 0f, 0.142f, 0.055f, 0.145f, topOnly = true)
                    addHairClump(0f, 1.56f, -0.11f, 0.09f, 0.09f, 0.05f, -0.03f)
                }
                "Высокий фейд" -> {
                    addEllipsoid(0f, 1.50f, headZ, 0.13f, 0.14f, 0.14f, topOnly = true)
                    addEllipsoid(0f, 1.40f, 0f, 0.137f, 0.035f, 0.14f, topOnly = true)
                    addHairClump(0f, 1.56f, -0.105f, 0.085f, 0.075f, 0.045f, -0.035f)
                }
                "Андеркат" -> {
                    addEllipsoid(0f, 1.50f, headZ, 0.155f, 0.17f, 0.16f, topOnly = true)
                    addEllipsoid(0f, 1.42f, 0.005f, 0.155f, 0.045f, 0.16f, topOnly = true)
                    for (i in -2..2) addHairClump(i * 0.045f, 1.57f, -0.095f, 0.042f, 0.095f, 0.055f, -0.025f)
                }
                "Короткий ёжик" -> {
                    addEllipsoid(0f, 1.50f, headZ, 0.145f, 0.16f, 0.15f, topOnly = true)
                    for (i in -3..3) {
                        val x = i * 0.038f
                        addHairClump(x, 1.60f, -0.015f, 0.032f, 0.09f, 0.032f, 0f)
                    }
                }
                "Текстурный кроп" -> {
                    addEllipsoid(0f, 1.49f, headZ, 0.15f, 0.16f, 0.15f, topOnly = true)
                    for (i in -3..3) {
                        val x = i * 0.035f
                        addHairClump(x, 1.57f, -0.105f, 0.035f, 0.10f, 0.05f, -0.055f)
                    }
                }
                "Кудри" -> {
                    addEllipsoid(0f, 1.49f, headZ, 0.17f, 0.18f, 0.17f, topOnly = true)
                    for (x in -2..2) for (z in -1..1) {
                        addEllipsoid(x * 0.055f, 1.58f + (1 - kotlin.math.abs(x)) * 0.01f,
                            z * 0.055f - 0.01f, 0.052f, 0.052f, 0.052f, 5, 12, true)
                    }
                }
                "Объёмные кудри" -> {
                    addEllipsoid(0f, 1.50f, headZ, 0.19f, 0.20f, 0.19f, topOnly = true)
                    for (x in -3..3) for (z in -2..2) {
                        if ((x*x + z*z) <= 10) {
                            addEllipsoid(x * 0.055f, 1.61f, z * 0.055f - 0.01f,
                                0.055f, 0.055f, 0.055f, 5, 12, true)
                        }
                    }
                }
                "Ирокез" -> {
                    for (i in -2..2) {
                        addHairClump(i * 0.035f, 1.57f, 0f, 0.032f, 0.19f, 0.055f, -0.02f)
                    }
                    addEllipsoid(0f, 1.47f, 0f, 0.13f, 0.07f, 0.14f, topOnly = true)
                }
                "Длинные назад" -> {
                    addEllipsoid(0f, 1.50f, 0.015f, 0.17f, 0.18f, 0.18f, topOnly = true)
                    for (x in -2..2) {
                        addHairClump(x * 0.055f, 1.48f, 0.095f, 0.035f, 0.24f, 0.065f, 0.10f)
                    }
                }
                "Дреды" -> {
                    addEllipsoid(0f, 1.49f, 0f, 0.16f, 0.17f, 0.16f, topOnly = true)
                    for (x in -3..3) {
                        addHairClump(x * 0.042f, 1.54f, 0.025f, 0.022f, 0.27f, 0.025f, 0.02f)
                    }
                }
                "Косички" -> {
                    addEllipsoid(0f, 1.49f, 0f, 0.16f, 0.17f, 0.16f, topOnly = true)
                    for (x in -3..3) {
                        addHairClump(x * 0.045f, 1.53f, 0.0f, 0.019f, 0.29f, 0.022f, 0.015f)
                    }
                }
                else -> addEllipsoid(0f, headY, headZ, 0.145f, 0.16f, 0.155f, topOnly = true)
            }

            // Build smooth vertex normals so the hair is lit like real 3D geometry.
            val normals = FloatArray(positions.size)
            for (i in indices.indices step 3) {
                val ia = indices[i] * 3
                val ib = indices[i + 1] * 3
                val ic = indices[i + 2] * 3
                val ax = positions[ib] - positions[ia]
                val ay = positions[ib + 1] - positions[ia + 1]
                val az = positions[ib + 2] - positions[ia + 2]
                val bx = positions[ic] - positions[ia]
                val by = positions[ic + 1] - positions[ia + 1]
                val bz = positions[ic + 2] - positions[ia + 2]
                val nx = ay * bz - az * by
                val ny = az * bx - ax * bz
                val nz = ax * by - ay * bx
                for (v in intArrayOf(ia, ib, ic)) {
                    normals[v] += nx
                    normals[v + 1] += ny
                    normals[v + 2] += nz
                }
            }
            for (i in normals.indices step 3) {
                val len = kotlin.math.sqrt(normals[i] * normals[i] + normals[i+1] * normals[i+1] + normals[i+2] * normals[i+2]).coerceAtLeast(0.0001f)
                normals[i] /= len; normals[i+1] /= len; normals[i+2] /= len
            }

            val vbData = ByteBuffer.allocateDirect(positions.size * 4 + normals.size * 4).order(ByteOrder.nativeOrder())
            for (i in positions.indices step 3) {
                vbData.putFloat(positions[i]); vbData.putFloat(positions[i+1]); vbData.putFloat(positions[i+2])
                vbData.putFloat(normals[i]); vbData.putFloat(normals[i+1]); vbData.putFloat(normals[i+2])
            }
            vbData.flip()

            val ibData = ByteBuffer.allocateDirect(indices.size * 2).order(ByteOrder.nativeOrder())
            indices.forEach { ibData.putShort(it.toShort()) }
            ibData.flip()

            hairVertexBuffer = VertexBuffer.Builder()
                .bufferCount(1)
                .vertexCount(positions.size / 3)
                .attribute(VertexBuffer.VertexAttribute.POSITION, 0, VertexBuffer.AttributeType.FLOAT3, 0, 24)
                .attribute(VertexBuffer.VertexAttribute.NORMAL, 0, VertexBuffer.AttributeType.FLOAT3, 12, 24)
                .build(modelViewer.engine)
            hairVertexBuffer!!.setBufferAt(modelViewer.engine, 0, vbData)

            hairIndexBuffer = IndexBuffer.Builder()
                .indexCount(indices.size)
                .bufferType(IndexBuffer.Builder.IndexType.USHORT)
                .build(modelViewer.engine)
            hairIndexBuffer!!.setBuffer(modelViewer.engine, ibData)

            hairEntity = EntityManager.get().create()
            RenderableManager.Builder(1)
                .boundingBox(com.google.android.filament.Box(0f, 1.5f, 0f, 0.30f, 0.38f, 0.30f))
                .geometry(0, RenderableManager.PrimitiveType.TRIANGLES, hairVertexBuffer!!, hairIndexBuffer!!)
                .material(0, hairMaterial!!.defaultInstance)
                .culling(false)
                .castShadows(true)
                .receiveShadows(true)
                .build(modelViewer.engine, hairEntity)
            modelViewer.scene.addEntity(hairEntity)

            val tm = modelViewer.engine.transformManager
            tm.create(hairEntity)
            tm.setParent(tm.getInstance(hairEntity), tm.getInstance(modelViewer.asset!!.root))
        } catch (e: Exception) {
            Log.e(TAG, "3D hair build failed", e)
        }
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
                    rebuildHair(player.hair)
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
                if (hairEntity != 0) {
                    modelViewer.engine.renderableManager.destroy(hairEntity)
                    modelViewer.engine.transformManager.destroy(hairEntity)
                    EntityManager.get().destroy(hairEntity)
                }
                hairVertexBuffer?.let { modelViewer.engine.destroyVertexBuffer(it) }
                hairIndexBuffer?.let { modelViewer.engine.destroyIndexBuffer(it) }
                hairMaterial?.let { modelViewer.engine.destroyMaterial(it) }
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
                "Короткая классика","Фейд","Высокий фейд","Андеркат",
                "Короткий ёжик","Текстурный кроп","Кудри","Объёмные кудри",
                "Ирокез","Длинные назад","Дреды","Косички"
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
