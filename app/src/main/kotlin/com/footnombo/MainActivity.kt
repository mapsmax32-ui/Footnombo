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

class PlayerPreviewView(context: Activity, private var player: Player) : ImageView(context) {
    private var hCm = 180
    private var wKg = 72
    private var skinName = "Средняя"
    private var hairName = "Тёмная"
    private var positionName = "ЦАП"
    private var rotation = 0f
    private var downX = 0f

    init {
        scaleType = ScaleType.CENTER_CROP
        setBackgroundColor(Color.rgb(10, 32, 23))
        isClickable = true
        render()
    }

    fun update(height: Int, weight: Int, skin: String, hair: String, position: String) {
        hCm = height
        wKg = weight
        skinName = skin
        hairName = hair
        positionName = position
        render()
    }

    override fun onTouchEvent(event: android.view.MotionEvent): Boolean {
        when (event.actionMasked) {
            android.view.MotionEvent.ACTION_DOWN -> {
                downX = event.x
                return true
            }
            android.view.MotionEvent.ACTION_MOVE -> {
                rotation += (event.x - downX) * 0.7f
                downX = event.x
                render()
                return true
            }
        }
        return true
    }

    private fun render() {
        val density = resources.displayMetrics.density
        val bw = (320f * density).roundToInt().coerceAtLeast(320)
        val bh = (285f * density).roundToInt().coerceAtLeast(285)
        val bmp = Bitmap.createBitmap(bw, bh, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        val w = bw.toFloat()
        val h = bh.toFloat()
        val cx = w / 2f
        val scale = density
        val lean = kotlin.math.sin(Math.toRadians(rotation.toDouble())).toFloat()
        val turn = kotlin.math.cos(Math.toRadians(rotation.toDouble())).toFloat()
        val bodyW = (wKg / 72f).coerceIn(.82f, 1.22f)
        val bodyH = (hCm / 180f).coerceIn(.86f, 1.14f)

        val bg = Paint(Paint.ANTI_ALIAS_FLAG)
        bg.shader = LinearGradient(0f, 0f, 0f, h, Color.rgb(24, 78, 52), Color.rgb(5, 22, 15), Shader.TileMode.CLAMP)
        c.drawRect(0f, 0f, w, h, bg)

        val glow = Paint(Paint.ANTI_ALIAS_FLAG)
        glow.color = Color.argb(55, 100, 255, 170)
        c.drawCircle(cx, h * .40f, w * .34f, glow)

        val floor = Paint(Paint.ANTI_ALIAS_FLAG)
        floor.color = Color.argb(120, 0, 0, 0)
        c.drawOval(cx - 58f*scale, h*.88f, cx + 58f*scale, h*.94f, floor)

        fun skinColor() = when (skinName) {
            "Светлая" -> Color.rgb(242, 190, 153)
            "Смуглая" -> Color.rgb(181, 112, 72)
            "Тёмная" -> Color.rgb(105, 60, 39)
            else -> Color.rgb(211, 151, 106)
        }
        fun hairColor() = when (hairName) {
            "Светлая" -> Color.rgb(220, 180, 82)
            "Каштановая" -> Color.rgb(112, 60, 30)
            "Короткая" -> Color.rgb(28, 25, 24)
            else -> Color.rgb(12, 12, 12)
        }
        fun shirtColor() = when (positionName) {
            "ЦФ" -> Color.rgb(220, 55, 50)
            "ЦЗ" -> Color.rgb(55, 105, 225)
            "ВР" -> Color.rgb(235, 185, 45)
            else -> Color.rgb(35, 190, 98)
        }

        val skin = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = skinColor() }
        val hair = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = hairColor() }
        val shirt = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = shirtColor() }
        val shorts = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = darken(shirt.color, .58f) }
        val white = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
        val boot = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(18, 20, 24) }

        c.save()
        c.translate(cx + lean * 18f*scale, 0f)

        val headY = h*.25f
        val torsoTop = h*.37f
        val torsoBottom = h*.59f
        val hipY = h*.60f
        val legBottom = h*.86f
        val shoulder = 47f*bodyW*scale
        val leg = 15f*bodyW*scale
        val legHeight = (legBottom-hipY)*bodyH.coerceIn(.9f,1.1f)

        rounded(c, -leg*1.15f, hipY, leg*2.3f, legHeight, 9f*scale, shorts)
        rounded(c, 0f, hipY, leg*2.3f, legHeight, 9f*scale, shorts)
        rounded(c, -leg*1.05f, h*.69f, leg*2.1f, h*.13f, 7f*scale, white)
        rounded(c, 0f, h*.69f, leg*2.1f, h*.13f, 7f*scale, white)
        rounded(c, -leg*1.2f, h*.80f, leg*2.55f, h*.08f, 7f*scale, boot)
        rounded(c, -leg*.9f, h*.86f, leg*3.0f, 11f*scale, 5f*scale, boot)
        rounded(c, -2f*scale, h*.80f, leg*2.55f, h*.08f, 7f*scale, boot)
        rounded(c, 4f*scale, h*.86f, leg*3.0f, 11f*scale, 5f*scale, boot)

        c.drawRoundRect(RectF(-shoulder, torsoTop, shoulder, torsoBottom), 18f*scale, 18f*scale, shirt)
        rounded(c, -shoulder*.76f, hipY-5f*scale, shoulder*1.52f, 34f*scale, 10f*scale, shorts)

        val armW = 18f*scale
        rounded(c, -shoulder-15f*scale, torsoTop+8f*scale, armW, 82f*scale, 9f*scale, shirt)
        c.drawCircle(-shoulder-6f*scale, torsoTop+94f*scale, 13f*scale, skin)
        rounded(c, shoulder-3f*scale, torsoTop+8f*scale, armW, 82f*scale, 9f*scale, shirt)
        c.drawCircle(shoulder+6f*scale, torsoTop+94f*scale, 13f*scale, skin)

        rounded(c, -14f*scale, torsoTop-22f*scale, 28f*scale, 28f*scale, 8f*scale, skin)
        c.drawOval(RectF(-31f*scale, headY-34f*scale, 31f*scale, headY+34f*scale), skin)
        c.drawArc(RectF(-31f*scale, headY-37f*scale, 31f*scale, headY+26f*scale), 180f, 180f, true, hair)
        c.drawOval(RectF(-29f*scale, headY-37f*scale, 29f*scale, headY-9f*scale), hair)

        val eye = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(30, 24, 20) }
        c.drawCircle(-10f*turn*scale, headY-2f*scale, 2.4f*scale, eye)
        c.drawCircle(10f*turn*scale, headY-2f*scale, 2.4f*scale, eye)

        val number = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textAlign = Paint.Align.CENTER
            textSize = 25f*scale
            typeface = Typeface.DEFAULT_BOLD
        }
        c.drawText(player.number.toString(), 0f, torsoTop+75f*scale, number)

        val title = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textAlign = Paint.Align.CENTER
            textSize = 13f*scale
            typeface = Typeface.DEFAULT_BOLD
        }
        c.drawText("ПРЕДПРОСМОТР ИГРОКА", cx, h*.96f, title)
        c.restore()

        setImageBitmap(bmp)
    }

    private fun rounded(c: Canvas, x: Float, y: Float, w: Float, h: Float, r: Float, p: Paint) {
        c.drawRoundRect(RectF(x, y, x+w, y+h), r, r, p)
    }

    private fun darken(color: Int, factor: Float): Int =
        Color.rgb((Color.red(color)*factor).toInt(), (Color.green(color)*factor).toInt(), (Color.blue(color)*factor).toInt())
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
