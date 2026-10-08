package com.footnombo

import android.app.Activity
import android.os.Bundle
import android.graphics.*
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
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
    var injuryWeeks: Int = 0,
    var reputation: Int = 20
) { val overall get() = ((attack + pass + speed + physical) / 4.0).roundToInt() }

class PlayerPreviewView(context: Activity, private var player: Player) : android.opengl.GLSurfaceView(context) {
    private val renderer = Player3DRenderer()
    private var touchX = 0f

    init {
        setEGLContextClientVersion(2)
        setRenderer(renderer)
        renderMode = android.opengl.GLSurfaceView.RENDERMODE_CONTINUOUSLY
        setBackgroundColor(Color.rgb(12, 25, 20))
    }

    fun update(height: Int, weight: Int, skin: String, hair: String, position: String) {
        queueEvent {
            renderer.height = height
            renderer.weight = weight
            renderer.skin = skin
            renderer.hair = hair
            renderer.position = position
        }
    }

    override fun onTouchEvent(event: android.view.MotionEvent): Boolean {
        when (event.actionMasked) {
            android.view.MotionEvent.ACTION_DOWN -> {
                touchX = event.x
                return true
            }
            android.view.MotionEvent.ACTION_MOVE -> {
                val dx = event.x - touchX
                touchX = event.x
                queueEvent { renderer.rotation += dx * 0.65f }
                return true
            }
        }
        return true
    }

    private class Mesh(
        private val vertices: FloatArray,
        private val normals: FloatArray,
        private val indices: ShortArray
    ) {
        private val vb = java.nio.ByteBuffer.allocateDirect(vertices.size * 4)
            .order(java.nio.ByteOrder.nativeOrder()).asFloatBuffer()
        private val nb = java.nio.ByteBuffer.allocateDirect(normals.size * 4)
            .order(java.nio.ByteOrder.nativeOrder()).asFloatBuffer()
        private val ib = java.nio.ByteBuffer.allocateDirect(indices.size * 2)
            .order(java.nio.ByteOrder.nativeOrder()).asShortBuffer()

        init {
            vb.put(vertices).position(0)
            nb.put(normals).position(0)
            ib.put(indices).position(0)
        }

        fun draw(program: Int, posHandle: Int, normalHandle: Int) {
            vb.position(0)
            android.opengl.GLES20.glEnableVertexAttribArray(posHandle)
            android.opengl.GLES20.glVertexAttribPointer(posHandle, 3, android.opengl.GLES20.GL_FLOAT, false, 0, vb)
            nb.position(0)
            android.opengl.GLES20.glEnableVertexAttribArray(normalHandle)
            android.opengl.GLES20.glVertexAttribPointer(normalHandle, 3, android.opengl.GLES20.GL_FLOAT, false, 0, nb)
            ib.position(0)
            android.opengl.GLES20.glDrawElements(android.opengl.GLES20.GL_TRIANGLES, indices.size, android.opengl.GLES20.GL_UNSIGNED_SHORT, ib)
            android.opengl.GLES20.glDisableVertexAttribArray(posHandle)
            android.opengl.GLES20.glDisableVertexAttribArray(normalHandle)
        }
    }

    private class Player3DRenderer : android.opengl.GLSurfaceView.Renderer {
        var height = 180
        var weight = 72
        var skin = "Средняя"
        var hair = "Тёмная"
        var position = "ЦАП"
        var rotation = 0f

        private var program = 0
        private lateinit var sphere: Mesh
        private lateinit var cylinder: Mesh
        private var width = 1
        private var viewHeight = 1
        private val projection = FloatArray(16)
        private val view = FloatArray(16)
        private val model = FloatArray(16)
        private val vp = FloatArray(16)
        private val mvp = FloatArray(16)
        private val normalMatrix = FloatArray(9)
        private var colorHandle = 0
        private var mvpHandle = 0
        private var normalHandle = 0
        private var lightHandle = 0
        private var normalMatrixHandle = 0
        private var positionHandle = 0
        private var shaderReady = false

        private val vertexShader = """
            uniform mat4 uMvp;
            uniform mat3 uNormal;
            attribute vec3 aPosition;
            attribute vec3 aNormal;
            varying vec3 vNormal;
            varying vec3 vPosition;
            void main() {
                vNormal = normalize(uNormal * aNormal);
                vPosition = aPosition;
                gl_Position = uMvp * vec4(aPosition, 1.0);
            }
        """.trimIndent()

        private val fragmentShader = """
            precision mediump float;
            uniform vec4 uColor;
            uniform vec3 uLight;
            varying vec3 vNormal;
            void main() {
                float diffuse = max(dot(normalize(vNormal), normalize(uLight)), 0.0);
                float rim = pow(1.0 - max(dot(normalize(vNormal), vec3(0.0,0.0,1.0)), 0.0), 2.0);
                float light = 0.30 + diffuse * 0.62 + rim * 0.08;
                gl_FragColor = vec4(uColor.rgb * light, uColor.a);
            }
        """.trimIndent()

        override fun onSurfaceCreated(gl: javax.microedition.khronos.opengles.GL10?, config: javax.microedition.khronos.egl.EGLConfig?) {
            program = makeProgram(vertexShader, fragmentShader)
            shaderReady = program != 0
            sphere = makeSphere(1f, 28, 18)
            cylinder = makeCylinder(1f, 1f, 24)
            mvpHandle = android.opengl.GLES20.glGetUniformLocation(program, "uMvp")
            normalHandle = 1
            colorHandle = android.opengl.GLES20.glGetUniformLocation(program, "uColor")
            lightHandle = android.opengl.GLES20.glGetUniformLocation(program, "uLight")
            normalMatrixHandle = android.opengl.GLES20.glGetUniformLocation(program, "uNormal")
            positionHandle = 0
            android.opengl.GLES20.glDisable(android.opengl.GLES20.GL_CULL_FACE)
            android.opengl.GLES20.glEnable(android.opengl.GLES20.GL_DEPTH_TEST)
            android.opengl.GLES20.glClearColor(0.055f, 0.10f, 0.08f, 1f)
        }

        override fun onSurfaceChanged(gl: javax.microedition.khronos.opengles.GL10?, w: Int, h: Int) {
            width = w
            viewHeight = h
            android.opengl.GLES20.glViewport(0, 0, w, h)
            val ratio = w.toFloat() / h.coerceAtLeast(1)
            android.opengl.Matrix.perspectiveM(projection, 0, 43f, ratio, 0.1f, 30f)
        }

        override fun onDrawFrame(gl: javax.microedition.khronos.opengles.GL10?) {
            android.opengl.GLES20.glClear(android.opengl.GLES20.GL_COLOR_BUFFER_BIT or android.opengl.GLES20.GL_DEPTH_BUFFER_BIT)
            if (!shaderReady) return
            android.opengl.GLES20.glUseProgram(program)
            android.opengl.GLES20.glUniform3f(lightHandle, -0.45f, 0.85f, 1.0f)

            android.opengl.Matrix.setLookAtM(view, 0, 0f, 1.25f, 7.4f, 0f, 1.25f, 0f, 0f, 1f, 0f)
            android.opengl.Matrix.setIdentityM(model, 0)
            android.opengl.Matrix.rotateM(model, 0, rotation, 0f, 1f, 0f)
            android.opengl.Matrix.multiplyMM(vp, 0, view, 0, model, 0)

            drawModel()
        }

        private fun drawModel() {
            val hScale = (height / 180f).coerceIn(0.86f, 1.14f)
            val wScale = (weight / 72f).coerceIn(0.82f, 1.22f)
            val torsoW = 0.72f * wScale
            val shoulderW = 0.88f * wScale
            val legW = 0.23f * wScale
            val skinColor = skinColor()
            val shirt = shirtColor()
            val shorts = floatArrayOf(shirt[0] * .52f, shirt[1] * .52f, shirt[2] * .52f, 1f)
            val skin = floatArrayOf(skinColor[0], skinColor[1], skinColor[2], 1f)
            val jersey = floatArrayOf(shirt[0], shirt[1], shirt[2], 1f)
            val boot = floatArrayOf(.035f, .04f, .045f, 1f)
            val sock = floatArrayOf(.88f, .89f, .88f, 1f)
            val hairCol = hairColor()

            part(sphere, 0f, 1.86f*hScale, 0f, .32f, .32f, .32f, hairCol)
            part(sphere, 0f, 1.85f*hScale, 0f, .295f, .295f, .295f, skin)
            part(cylinder, 0f, 1.48f*hScale, 0f, .18f, .22f, .18f, skin)
            part(sphere, 0f, 1.20f*hScale, 0f, shoulderW*.88f, .62f, torsoW*.78f, jersey)
            part(sphere, 0f, .66f*hScale, 0f, shoulderW*.72f, .30f, torsoW*.70f, shorts)

            // Rounded shoulders, arms and hands
            arm(-1f, shoulderW, hScale, jersey, skin)
            arm(1f, shoulderW, hScale, jersey, skin)

            // Natural hips and legs
            leg(-1f, legW, hScale, shorts, sock, boot)
            leg(1f, legW, hScale, shorts, sock, boot)
        }

        private fun arm(side: Float, shoulderW: Float, hs: Float, jersey: FloatArray, skin: FloatArray) {
            part(sphere, side*shoulderW*.83f, 1.34f*hs, 0f, .20f, .20f, .20f, jersey)
            part(cylinder, side*shoulderW*1.00f, 1.14f*hs, 0f, .17f, .43f, .17f, jersey, side*8f)
            part(sphere, side*shoulderW*1.02f, .84f*hs, 0f, .17f, .17f, .17f, skin)
            part(cylinder, side*shoulderW*1.04f, .66f*hs, 0f, .14f, .30f, .14f, skin, side*5f)
            part(sphere, side*shoulderW*1.06f, .49f*hs, 0f, .15f, .18f, .15f, skin)
        }

        private fun leg(side: Float, legW: Float, hs: Float, shorts: FloatArray, sock: FloatArray, boot: FloatArray) {
            part(sphere, side*legW*1.25f, .54f*hs, 0f, .27f, .27f, .27f, shorts)
            part(cylinder, side*legW*1.25f, .24f*hs, 0f, legW, .52f, legW, shorts)
            part(sphere, side*legW*1.25f, -.05f*hs, 0f, legW*1.03f, legW*1.03f, legW*1.03f, sock)
            part(cylinder, side*legW*1.25f, -.34f*hs, 0f, legW*.88f, .48f, legW*.88f, sock)
            part(sphere, side*legW*1.25f, -.59f*hs, .09f, .18f, .11f, .30f, boot)
        }

        private fun part(mesh: Mesh, x: Float, y: Float, z: Float, sx: Float, sy: Float, sz: Float, color: FloatArray, rotationZ: Float = 0f) {
            android.opengl.Matrix.setIdentityM(model, 0)
            android.opengl.Matrix.rotateM(model, 0, rotation, 0f, 1f, 0f)
            android.opengl.Matrix.translateM(model, 0, x, y, z)
            if (rotationZ != 0f) android.opengl.Matrix.rotateM(model, 0, rotationZ, 0f, 0f, 1f)
            android.opengl.Matrix.scaleM(model, 0, sx, sy, sz)
            android.opengl.Matrix.multiplyMM(mvp, 0, view, 0, model, 0)
            android.opengl.Matrix.multiplyMM(mvp, 0, projection, 0, mvp, 0)

            val nm = FloatArray(16)
            android.opengl.Matrix.invertM(nm, 0, model, 0)
            android.opengl.Matrix.transposeM(nm, 0, nm, 0)
            val n3 = floatArrayOf(nm[0],nm[1],nm[2],nm[4],nm[5],nm[6],nm[8],nm[9],nm[10])

            android.opengl.GLES20.glUniformMatrix4fv(mvpHandle, 1, false, mvp, 0)
            android.opengl.GLES20.glUniformMatrix3fv(normalMatrixHandle, 1, false, n3, 0)
            android.opengl.GLES20.glUniform4fv(colorHandle, 1, color, 0)
            mesh.draw(program, positionHandle, normalHandle)
        }

        private fun skinColor(): FloatArray = when (skin) {
            "Светлая" -> floatArrayOf(0.90f, .69f, .54f)
            "Смуглая" -> floatArrayOf(.67f, .42f, .26f)
            "Тёмная" -> floatArrayOf(.38f, .22f, .14f)
            else -> floatArrayOf(.78f, .55f, .38f)
        }

        private fun hairColor(): FloatArray = when (hair) {
            "Светлая" -> floatArrayOf(.72f, .58f, .31f, 1f)
            "Каштановая" -> floatArrayOf(.34f, .18f, .09f, 1f)
            "Короткая" -> floatArrayOf(.08f, .055f, .045f, 1f)
            else -> floatArrayOf(.025f, .025f, .025f, 1f)
        }

        private fun shirtColor(): FloatArray = when (position) {
            "ЦФ" -> floatArrayOf(.80f, .10f, .08f)
            "ЦЗ" -> floatArrayOf(.08f, .28f, .72f)
            "ВР" -> floatArrayOf(.82f, .58f, .08f)
            else -> floatArrayOf(.05f, .62f, .30f)
        }

        private fun makeSphere(radius: Float, slices: Int, stacks: Int): Mesh {
            val v = ArrayList<Float>()
            val n = ArrayList<Float>()
            val ind = ArrayList<Short>()
            for (j in 0..stacks) {
                val phi = Math.PI * j / stacks
                val sp = kotlin.math.sin(phi).toFloat()
                val cp = kotlin.math.cos(phi).toFloat()
                for (i in 0..slices) {
                    val th = 2.0 * Math.PI * i / slices
                    val st = kotlin.math.sin(th).toFloat()
                    val ct = kotlin.math.cos(th).toFloat()
                    v += radius * sp * ct
                    v += radius * cp
                    v += radius * sp * st
                    n += sp * ct
                    n += cp
                    n += sp * st
                }
            }
            for (j in 0 until stacks) for (i in 0 until slices) {
                val a = (j*(slices+1)+i).toShort()
                val b = (a.toInt()+slices+1).toShort()
                ind += a; ind += b; ind += (a.toInt()+1).toShort()
                ind += (a.toInt()+1).toShort(); ind += b; ind += (b.toInt()+1).toShort()
            }
            return Mesh(v.toFloatArray(), n.toFloatArray(), ind.toShortArray())
        }

        private fun makeCylinder(radiusTop: Float, radiusBottom: Float, slices: Int): Mesh {
            val v = ArrayList<Float>()
            val n = ArrayList<Float>()
            val ind = ArrayList<Short>()
            val height = 2f
            for (y in 0..1) {
                val yy = if (y == 0) -1f else 1f
                val r = if (y == 0) radiusBottom else radiusTop
                for (i in 0..slices) {
                    val th = 2f * Math.PI.toFloat() * i / slices
                    val c = kotlin.math.cos(th)
                    val s = kotlin.math.sin(th)
                    v += r*c; v += yy; v += r*s
                    n += c; n += 0f; n += s
                }
            }
            for (i in 0 until slices) {
                val a = i.toShort()
                val b = (i+slices+1).toShort()
                ind += a; ind += (a.toInt()+1).toShort(); ind += b
                ind += (a.toInt()+1).toShort(); ind += (b.toInt()+1).toShort(); ind += b
            }
            return Mesh(v.toFloatArray(), n.toFloatArray(), ind.toShortArray())
        }

        private fun makeProgram(vs: String, fs: String): Int {
            fun compile(type: Int, source: String): Int {
                val shader = android.opengl.GLES20.glCreateShader(type)
                if (shader == 0) return 0
                android.opengl.GLES20.glShaderSource(shader, source)
                android.opengl.GLES20.glCompileShader(shader)
                val ok = IntArray(1)
                android.opengl.GLES20.glGetShaderiv(shader, android.opengl.GLES20.GL_COMPILE_STATUS, ok, 0)
                if (ok[0] == 0) {
                    android.util.Log.e("FootnomboGL", android.opengl.GLES20.glGetShaderInfoLog(shader))
                    android.opengl.GLES20.glDeleteShader(shader)
                    return 0
                }
                return shader
            }
            val v = compile(android.opengl.GLES20.GL_VERTEX_SHADER, vs)
            val f = compile(android.opengl.GLES20.GL_FRAGMENT_SHADER, fs)
            if (v == 0 || f == 0) return 0
            val p = android.opengl.GLES20.glCreateProgram()
            if (p == 0) return 0
            android.opengl.GLES20.glAttachShader(p, v)
            android.opengl.GLES20.glAttachShader(p, f)
            android.opengl.GLES20.glBindAttribLocation(p, 0, "aPosition")
            android.opengl.GLES20.glBindAttribLocation(p, 1, "aNormal")
            android.opengl.GLES20.glLinkProgram(p)
            val linked = IntArray(1)
            android.opengl.GLES20.glGetProgramiv(p, android.opengl.GLES20.GL_LINK_STATUS, linked, 0)
            if (linked[0] == 0) {
                android.util.Log.e("FootnomboGL", android.opengl.GLES20.glGetProgramInfoLog(p))
                android.opengl.GLES20.glDeleteProgram(p)
                return 0
            }
            android.opengl.GLES20.glDeleteShader(v)
            android.opengl.GLES20.glDeleteShader(f)
            return p
        }
        }
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
        content.addView(tv("Настрой футболиста перед первым контрактом.",14f))

        val previewCard=card()
        preview=PlayerPreviewView(this,p).apply{minimumHeight=dp(270)}
        previewCard.addView(preview,LinearLayout.LayoutParams(-1,dp(285)))
        content.addView(previewCard)

        val c=card()
        val first=field("Имя","Алекс")
        val last=field("Фамилия","Морозов")
        c.addView(first);c.addView(last)

        c.addView(tv("Страна",13f,true))
        val country=Spinner(this).apply{adapter=ArrayAdapter(this@MainActivity,android.R.layout.simple_spinner_dropdown_item,
            arrayOf("Россия","Нидерланды","Германия","Бразилия","Аргентина","Франция","Испания","Англия"))}
        c.addView(country)

        c.addView(tv("Позиция",13f,true))
        val pos=Spinner(this).apply{adapter=ArrayAdapter(this@MainActivity,android.R.layout.simple_spinner_dropdown_item,
            arrayOf("ЦАП","ПВ","ЛВ","ЦФ","ЦП","ЦЗ","ВР"))}
        c.addView(pos)

        val age=field("Возраст, 16-35","18",2)
        val height=field("Рост, 150-210 см","180",2)
        val weight=field("Вес, 45-120 кг","72",2)
        val num=field("Номер, 1-99","10",2)
        c.addView(age);c.addView(height);c.addView(weight);c.addView(num)

        c.addView(tv("Цвет кожи",13f,true))
        val skin=Spinner(this).apply{adapter=ArrayAdapter(this@MainActivity,android.R.layout.simple_spinner_dropdown_item,
            arrayOf("Светлая","Средняя","Смуглая","Тёмная"))}
        c.addView(skin)
        c.addView(tv("Волосы",13f,true))
        val hair=Spinner(this).apply{adapter=ArrayAdapter(this@MainActivity,android.R.layout.simple_spinner_dropdown_item,
            arrayOf("Короткая","Каштановая","Светлая","Тёмная"))}
        c.addView(hair)
        content.addView(c)

        fun refresh(){
            val hh=(height.text.toString().toIntOrNull()?:180).coerceIn(150,210)
            val ww=(weight.text.toString().toIntOrNull()?:72).coerceIn(45,120)
            preview?.update(hh,ww,skin.selectedItem?.toString()?:"Средняя",hair.selectedItem?.toString()?:"Тёмная",pos.selectedItem?.toString()?:"ЦАП")
        }
        listOf(height,weight).forEach{it.setOnFocusChangeListener{_,_->refresh()}}
        skin.onItemSelectedListener=object: AdapterView.OnItemSelectedListener{
            override fun onNothingSelected(parent:AdapterView<*>?) {}
            override fun onItemSelected(parent:AdapterView<*>?,view:View?,position:Int,id:Long){refresh()}
        }
        hair.onItemSelectedListener=object: AdapterView.OnItemSelectedListener{
            override fun onNothingSelected(parent:AdapterView<*>?) {}
            override fun onItemSelected(parent:AdapterView<*>?,view:View?,position:Int,id:Long){refresh()}
        }
        pos.onItemSelectedListener=object: AdapterView.OnItemSelectedListener{
            override fun onNothingSelected(parent:AdapterView<*>?) {}
            override fun onItemSelected(parent:AdapterView<*>?,view:View?,position:Int,id:Long){refresh()}
        }

        content.addView(button("Начать карьеру →"){
            p.name=(first.text.toString().trim()+" "+last.text.toString().trim()).trim().ifBlank{"Алекс Морозов"}
            p.position=pos.selectedItem.toString()
            p.country=country.selectedItem.toString()
            p.age=(age.text.toString().toIntOrNull()?:18).coerceIn(16,35)
            p.number=(num.text.toString().toIntOrNull()?:10).coerceIn(1,99)
            p.height=(height.text.toString().toIntOrNull()?:180).coerceIn(150,210)
            p.weight=(weight.text.toString().toIntOrNull()?:72).coerceIn(45,120)
            p.skin=skin.selectedItem.toString()
            p.hair=hair.selectedItem.toString()
            showCareer()
        })
    }

    private fun nav(){
        val bar=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL;gravity=Gravity.CENTER}
        bar.addView(button("👤 Профиль"){showProfile()},LinearLayout.LayoutParams(0,dp(50),1f))
        bar.addView(button("🏋 Тренировка"){showTraining()},LinearLayout.LayoutParams(0,dp(50),1f))
        bar.addView(button("⚽ Матч"){showMatch()},LinearLayout.LayoutParams(0,dp(50),1f))
        root.addView(bar)
    }
    private fun showCareer(){
        base("Моя карьера")
        val c=card();c.addView(tv(p.name,22f,true));c.addView(tv(p.position+" • №"+p.number+" • "+p.age+" лет",14f));c.addView(tv(p.country+" • FC North City"));c.addView(tv("Общий рейтинг: "+p.overall,20f,true));content.addView(c)
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
        base("Матч");val c=card();c.addView(tv("FC North City — Red Falcons",20f,true));c.addView(tv("Домашний матч • Лига"))
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
