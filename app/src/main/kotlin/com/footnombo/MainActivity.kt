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

class PlayerPreviewView(context: Activity, private var player: Player) : View(context) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var previewHeight = 180
    private var previewWeight = 72
    private var previewSkin = "Средняя"
    private var previewHair = "Тёмная"
    private var previewPosition = "ЦАП"

    fun update(height: Int, weight: Int, skin: String, hair: String, position: String) {
        previewHeight = height
        previewWeight = weight
        previewSkin = skin
        previewHair = hair
        previewPosition = position
        invalidate()
    }

    private fun skinColor(): Int = when (previewSkin) {
        "Светлая" -> Color.rgb(244, 204, 172)
        "Смуглая" -> Color.rgb(180, 125, 82)
        "Тёмная" -> Color.rgb(105, 68, 45)
        else -> Color.rgb(211, 157, 111)
    }

    private fun shirtColor(): Int = when (previewPosition) {
        "ЦФ" -> Color.rgb(205, 64, 58)
        "ЦЗ" -> Color.rgb(55, 96, 180)
        "ВР" -> Color.rgb(226, 174, 45)
        else -> Color.rgb(28, 154, 88)
    }

    private fun hairColor(): Int = when (previewHair) {
        "Светлая" -> Color.rgb(205, 178, 105)
        "Каштановая" -> Color.rgb(105, 67, 40)
        "Короткая" -> Color.rgb(48, 36, 31)
        else -> Color.rgb(24, 24, 25)
    }

    private fun shade(base: Int, amount: Int): Int =
        Color.rgb((Color.red(base) + amount).coerceIn(0,255),
                  (Color.green(base) + amount).coerceIn(0,255),
                  (Color.blue(base) + amount).coerceIn(0,255))

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        paint.style = Paint.Style.FILL

        val bg = LinearGradient(0f, 0f, 0f, h, Color.rgb(22, 43, 35), Color.rgb(9, 22, 18), Shader.TileMode.CLAMP)
        paint.shader = bg
        canvas.drawRoundRect(0f, 0f, w, h, 28f, 28f, paint)
        paint.shader = null

        val heightScale = (previewHeight / 180f).coerceIn(.9f, 1.1f)
        val widthScale = (previewWeight / 72f).coerceIn(.84f, 1.18f)
        val cx = w / 2f
        val ground = h - 42f
        val figureH = (h * .76f * heightScale).coerceIn(h * .62f, h * .80f)
        val headR = (figureH * .075f).coerceIn(18f, 25f)
        val headY = ground - figureH + headR
        val neckY = headY + headR * .82f
        val shoulderW = figureH * .145f * widthScale
        val waistW = shoulderW * .58f
        val hipY = ground - figureH * .39f
        val torsoBottom = ground - figureH * .36f
        val torsoTop = neckY + headR * .45f
        val upperLegLen = figureH * .25f
        val lowerLegLen = figureH * .22f
        val legGap = figureH * .035f
        val skin = skinColor()
        val shirt = shirtColor()
        val darkShirt = shade(shirt, -32)
        val lightShirt = shade(shirt, 28)

        // Soft ground shadow
        paint.color = Color.argb(100, 0, 0, 0)
        canvas.drawOval(cx - figureH*.18f, ground-5f, cx + figureH*.18f, ground+11f, paint)

        // Legs: tapered paths, not rectangles
        fun legPath(left: Boolean): Path {
            val s = if (left) -1f else 1f
            val x = cx + s * legGap
            return Path().apply {
                moveTo(x - s*figureH*.045f, torsoBottom + figureH*.03f)
                lineTo(x + s*figureH*.07f, torsoBottom + figureH*.03f)
                lineTo(x + s*figureH*.065f, torsoBottom + upperLegLen)
                lineTo(x + s*figureH*.05f, torsoBottom + upperLegLen + lowerLegLen)
                lineTo(x - s*figureH*.055f, torsoBottom + upperLegLen + lowerLegLen)
                lineTo(x - s*figureH*.075f, torsoBottom + upperLegLen)
                close()
            }
        }
        paint.color = shade(shirt, -12)
        canvas.drawPath(legPath(true), paint)
        canvas.drawPath(legPath(false), paint)

        // Socks and rounded boots
        paint.color = Color.rgb(235, 235, 235)
        canvas.drawRoundRect(cx-legGap-figureH*.065f, ground-figureH*.055f, cx-legGap+figureH*.035f, ground-figureH*.005f, 7f, 7f, paint)
        canvas.drawRoundRect(cx+legGap-figureH*.035f, ground-figureH*.055f, cx+legGap+figureH*.065f, ground-figureH*.005f, 7f, 7f, paint)
        paint.color = Color.rgb(27, 29, 34)
        canvas.drawOval(cx-legGap-figureH*.09f, ground-figureH*.035f, cx-legGap+figureH*.07f, ground+2f, paint)
        canvas.drawOval(cx+legGap-figureH*.07f, ground-figureH*.035f, cx+legGap+figureH*.09f, ground+2f, paint)

        // Torso with natural shoulders and waist
        val torso = Path().apply {
            moveTo(cx-shoulderW, torsoTop)
            cubicTo(cx-shoulderW*.78f, torsoTop+figureH*.04f, cx-waistW*.95f, hipY-figureH*.03f, cx-waistW, torsoBottom)
            lineTo(cx+waistW, torsoBottom)
            cubicTo(cx+waistW*.95f, hipY-figureH*.03f, cx+shoulderW*.78f, torsoTop+figureH*.04f, cx+shoulderW, torsoTop)
            close()
        }
        paint.shader = LinearGradient(cx-shoulderW, torsoTop, cx+shoulderW, torsoBottom, lightShirt, darkShirt, Shader.TileMode.CLAMP)
        canvas.drawPath(torso, paint)
        paint.shader = null

        // Shorts
        paint.color = shade(shirt, -45)
        canvas.drawOval(cx-shoulderW*.72f, torsoBottom-figureH*.015f, cx, torsoBottom+figureH*.08f, paint)
        canvas.drawOval(cx, torsoBottom-figureH*.015f, cx+shoulderW*.72f, torsoBottom+figureH*.08f, paint)

        // Neck
        paint.color = skin
        canvas.drawRoundRect(cx-headR*.38f, neckY-headR*.05f, cx+headR*.38f, torsoTop+headR*.12f, 7f, 7f, paint)

        // Arms with shoulders, elbows and hands
        val armW = figureH * .045f * widthScale
        fun drawArm(left: Boolean) {
            val s = if (left) -1f else 1f
            val shoulderX = cx + s*shoulderW*.83f
            val elbowX = cx + s*shoulderW*1.10f
            val handX = cx + s*shoulderW*1.13f
            val elbowY = torsoTop + figureH*.19f
            val handY = torsoTop + figureH*.37f
            paint.color = shade(shirt, -18)
            canvas.drawRoundRect(shoulderX-s*armW*.55f, torsoTop+figureH*.03f, elbowX+s*armW*.45f, elbowY, armW, armW, paint)
            paint.color = skin
            canvas.drawRoundRect(elbowX-s*armW*.45f, elbowY-armW*.15f, handX+s*armW*.45f, handY, armW, armW, paint)
            canvas.drawCircle(handX, handY, armW*.62f, paint)
        }
        drawArm(true)
        drawArm(false)

        // Head with subtle spherical shading
        paint.shader = RadialGradient(cx-headR*.35f, headY-headR*.35f, headR*1.25f,
            intArrayOf(shade(skin, 28), skin, shade(skin, -35)),
            floatArrayOf(0f,.55f,1f), Shader.TileMode.CLAMP)
        canvas.drawCircle(cx, headY, headR, paint)
        paint.shader = null

        // Hair cap and simple side fade
        paint.color = hairColor()
        canvas.drawArc(cx-headR*1.02f, headY-headR*1.02f, cx+headR*1.02f, headY+headR*.75f, 180f, 180f, true, paint)
        if (previewHair == "Короткая" || previewHair == "Тёмная") {
            paint.color = shade(hairColor(), -18)
            canvas.drawRoundRect(cx-headR*.88f, headY-headR*.55f, cx-headR*.58f, headY+headR*.1f, 5f, 5f, paint)
            canvas.drawRoundRect(cx+headR*.58f, headY-headR*.55f, cx+headR*.88f, headY+headR*.1f, 5f, 5f, paint)
        }

        // Face details and jersey number
        paint.color = Color.rgb(45, 35, 30)
        canvas.drawCircle(cx-headR*.32f, headY-headR*.02f, 1.7f, paint)
        canvas.drawCircle(cx+headR*.32f, headY-headR*.02f, 1.7f, paint)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 2f
        canvas.drawArc(cx-headR*.25f, headY+headR*.12f, cx+headR*.25f, headY+headR*.42f, 15f, 150f, false, paint)
        paint.style = Paint.Style.FILL

        paint.color = Color.WHITE
        paint.textAlign = Paint.Align.CENTER
        paint.typeface = Typeface.DEFAULT_BOLD
        paint.textSize = (figureH*.095f).coerceIn(22f, 31f)
        canvas.drawText(player.number.toString(), cx, torsoTop+figureH*.19f, paint)

        paint.color = Color.argb(220,255,255,255)
        paint.textSize = 13f
        canvas.drawText("$previewHeight см  •  $previewWeight кг", cx, h-13f, paint)
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
