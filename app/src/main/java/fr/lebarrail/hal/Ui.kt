package fr.lebarrail.hal

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.text.Editable
import android.text.TextWatcher
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import java.text.Normalizer
import java.util.Locale
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsets
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Switch
import android.widget.TextView

// Petits outils d'interface communs aux écrans de Suricate.

const val MATCH = ViewGroup.LayoutParams.MATCH_PARENT
const val WRAP = ViewGroup.LayoutParams.WRAP_CONTENT

object Couleurs {
    val FOND = 0xFF161616.toInt()
    // Ces quatre couleurs se règlent dans Options ▸ Apparence.
    var CARTE = 0xFF242424.toInt()
    var LISERE = 0xFF3C3C3C.toInt()
    var BOUTON = 0xFF2E2620.toInt()
    var ACCENT = 0xFFFF8C1A.toInt()
    // L'orange de la charte (titre-logo, entrées du panneau) ne change pas.
    val ORANGE = 0xFFFF8C1A.toInt()
    val BLANC = 0xFFFFFFFF.toInt()
    val GRIS = 0xFF9E9E9E.toInt()
    val ROUGE = 0xFFE53935.toInt()
    val VERT = 0xFF66BB6A.toInt()
    val BLEU = 0xFF7F9CC4.toInt()
    val PANNEAU = 0xFF1E1E1E.toInt()
    val BANDEAU = 0xFF202020.toInt()
}

fun Context.dp(v: Int): Int = (v * resources.displayMetrics.density + 0.5f).toInt()

// Fond sombre, défilement et marges tenant compte des barres d'Android.
fun Activity.echafaudage(): LinearLayout {
    val defil = ScrollView(this)
    defil.setBackgroundColor(Couleurs.FOND)
    defil.isFillViewport = true
    val contenu = LinearLayout(this)
    contenu.orientation = LinearLayout.VERTICAL
    contenu.setPadding(dp(16), dp(8), dp(16), dp(28))
    defil.addView(contenu, ViewGroup.LayoutParams(MATCH, WRAP))
    defil.setOnApplyWindowInsetsListener { v, ins ->
        val s = ins.getInsets(WindowInsets.Type.systemBars())
        v.setPadding(s.left, s.top, s.right, s.bottom)
        ins
    }
    window.decorView.setBackgroundColor(Couleurs.FOND)
    setContentView(defil)
    return contenu
}

fun Context.texte(
    t: CharSequence,
    taille: Float = 15f,
    couleur: Int = Couleurs.BLANC,
    gras: Boolean = false
): TextView {
    val v = TextView(this)
    v.text = t
    v.textSize = taille
    v.setTextColor(couleur)
    if (gras) v.typeface = Typeface.DEFAULT_BOLD
    v.setLineSpacing(0f, 1.15f)
    val lp = LinearLayout.LayoutParams(MATCH, WRAP)
    lp.topMargin = dp(4)
    v.layoutParams = lp
    return v
}

fun Context.carte(): LinearLayout {
    val c = LinearLayout(this)
    c.orientation = LinearLayout.VERTICAL
    val fond = GradientDrawable()
    fond.cornerRadius = dp(14).toFloat()
    fond.setColor(Couleurs.CARTE)
    fond.setStroke(dp(1), Couleurs.LISERE)
    c.background = fond
    c.setPadding(dp(16), dp(12), dp(16), dp(14))
    c.elevation = dp(3).toFloat()
    val lp = LinearLayout.LayoutParams(MATCH, WRAP)
    lp.topMargin = dp(12)
    c.layoutParams = lp
    return c
}

fun Context.titreCarte(t: String): TextView = texte(t, 17f, Couleurs.ACCENT, true)

private fun Context.fondBouton(): RippleDrawable {
    val forme = GradientDrawable()
    forme.cornerRadius = dp(10).toFloat()
    forme.setColor(Couleurs.BOUTON)
    forme.setStroke(dp(1), Couleurs.ACCENT)
    return RippleDrawable(ColorStateList.valueOf(0x40FFFFFF), forme, null)
}

fun Context.bouton(t: String, action: () -> Unit): TextView {
    val b = TextView(this)
    b.text = t
    b.textSize = 15f
    b.setTextColor(Couleurs.BLANC)
    b.gravity = Gravity.CENTER
    b.setPadding(dp(12), dp(11), dp(12), dp(11))
    b.background = fondBouton()
    b.isClickable = true
    b.isFocusable = true
    b.setOnClickListener { action() }
    val lp = LinearLayout.LayoutParams(MATCH, WRAP)
    lp.topMargin = dp(10)
    b.layoutParams = lp
    return b
}

fun Context.petitBouton(t: String, action: () -> Unit): TextView {
    val b = bouton(t, action)
    b.textSize = 14f
    b.setPadding(dp(4), dp(9), dp(4), dp(9))
    // Pas de marge en haut : c'est la ligne qui porte l'écart. Une marge ici,
    // ajoutée au centrage de la ligne, poussait le bouton trop bas et rognait son cadre.
    val lp = LinearLayout.LayoutParams(0, WRAP, 1f)
    lp.topMargin = 0
    lp.leftMargin = dp(3)
    lp.rightMargin = dp(3)
    b.layoutParams = lp
    return b
}

fun Context.rangee(): LinearLayout {
    val r = LinearLayout(this)
    r.orientation = LinearLayout.HORIZONTAL
    r.gravity = Gravity.CENTER_VERTICAL
    // Sans cela, l'alignement sur la ligne de base rogne le bas des boutons.
    r.isBaselineAligned = false
    val lp = LinearLayout.LayoutParams(MATCH, WRAP)
    lp.topMargin = dp(8)
    r.layoutParams = lp
    return r
}

private fun Context.libelle(t: String): TextView {
    val l = TextView(this)
    l.text = t
    l.textSize = 15f
    l.setTextColor(Couleurs.BLANC)
    l.layoutParams = LinearLayout.LayoutParams(0, WRAP, 1f)
    return l
}

// « Réglage : valeur », le nom en blanc et la valeur en couleur.
fun Context.ligneValeur(t: String, valeur: String, couleur: Int): LinearLayout {
    val r = rangee()
    r.addView(libelle(t))
    val v = TextView(this)
    v.text = valeur
    v.textSize = 15f
    v.setTextColor(couleur)
    v.typeface = Typeface.DEFAULT_BOLD
    r.addView(v)
    return r
}

fun Context.ligneInterrupteur(t: String, valeur: Boolean, change: (Boolean) -> Unit): LinearLayout {
    val r = rangee()
    r.addView(libelle(t))
    val s = Switch(this)
    s.isChecked = valeur
    val etats = arrayOf(intArrayOf(android.R.attr.state_checked), intArrayOf())
    s.thumbTintList = ColorStateList(etats, intArrayOf(Couleurs.ACCENT, 0xFFBDBDBD.toInt()))
    val piste = (Couleurs.ACCENT and 0x00FFFFFF) or 0x80000000.toInt()
    s.trackTintList = ColorStateList(etats, intArrayOf(piste, 0x50FFFFFF))
    s.setOnCheckedChangeListener { _, b -> change(b) }
    r.addView(s)
    return r
}

// Ligne de choix : la valeur en orange ouvre la liste des possibilités.
fun Context.ligneChoix(t: String, options: List<String>, index: Int, change: (Int) -> Unit): LinearLayout {
    val r = rangee()
    r.addView(libelle(t))
    val v = TextView(this)
    v.text = options.getOrElse(index) { "" } + "  ▾"
    v.textSize = 15f
    v.setTextColor(Couleurs.ACCENT)
    v.setPadding(dp(8), dp(6), 0, dp(6))
    v.setOnClickListener {
        AlertDialog.Builder(this, android.R.style.Theme_Material_Dialog_Alert)
            .setTitle(t)
            .setSingleChoiceItems(options.toTypedArray(), index) { d, i ->
                d.dismiss()
                change(i)
            }
            .setNegativeButton("Annuler", null)
            .show()
    }
    r.addView(v)
    return r
}

// Champ de recherche sombre, avec une croix pour l'effacer.
fun Context.champRecherche(texteInitial: String, change: (String) -> Unit): LinearLayout {
    val r = rangee()
    val fond = GradientDrawable()
    fond.cornerRadius = dp(12).toFloat()
    fond.setColor(Couleurs.CARTE)
    fond.setStroke(dp(1), Couleurs.LISERE)
    r.background = fond
    r.setPadding(dp(12), 0, dp(4), 0)

    val champ = EditText(this)
    champ.setText(texteInitial)
    champ.hint = "Rechercher (nom, numéro, date…)"
    champ.setHintTextColor(Couleurs.GRIS)
    champ.setTextColor(Couleurs.BLANC)
    champ.textSize = 15f
    champ.setSingleLine(true)
    champ.background = null
    champ.imeOptions = EditorInfo.IME_ACTION_SEARCH
    champ.layoutParams = LinearLayout.LayoutParams(0, WRAP, 1f)
    r.addView(champ)

    val croix = TextView(this)
    croix.text = "✕"
    croix.textSize = 16f
    croix.setTextColor(Couleurs.GRIS)
    croix.setPadding(dp(12), dp(10), dp(12), dp(10))
    croix.setOnClickListener { champ.setText("") }
    r.addView(croix)

    champ.addTextChangedListener(object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
        override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
        override fun afterTextChanged(s: Editable?) {
            change(s?.toString() ?: "")
        }
    })
    return r
}

// Comparaison sans majuscules ni accents, pour la recherche.
fun sansAccents(t: String): String =
    Normalizer.normalize(t.lowercase(Locale.FRANCE), Normalizer.Form.NFD)
        .replace(Regex("\\p{Mn}+"), "")

// ---------------------------------------------------------------- couleurs

private val PALETTE = intArrayOf(
    0xFF000000.toInt(), 0xFF121212.toInt(), 0xFF161616.toInt(), 0xFF1E1E1E.toInt(), 0xFF242424.toInt(), 0xFF2E2E2E.toInt(),
    0xFF3C3C3C.toInt(), 0xFF555555.toInt(), 0xFF808080.toInt(), 0xFFB0B0B0.toInt(), 0xFFE0E0E0.toInt(), 0xFFFFFFFF.toInt(),
    0xFF2E2620.toInt(), 0xFF3B2A1A.toInt(), 0xFF5D4037.toInt(), 0xFF8D6E63.toInt(), 0xFFC9A26B.toInt(), 0xFFE3C99A.toInt(),
    0xFF4A0E0E.toInt(), 0xFF800020.toInt(), 0xFFB71C1C.toInt(), 0xFFE53935.toInt(), 0xFFE8770A.toInt(), 0xFFFF8C1A.toInt(),
    0xFFFFB74D.toInt(), 0xFFFBC02D.toInt(), 0xFF1B3A2A.toInt(), 0xFF2E7D32.toInt(), 0xFF66BB6A.toInt(), 0xFF26A69A.toInt(),
    0xFF0D2A4A.toInt(), 0xFF00395B.toInt(), 0xFF1565C0.toInt(), 0xFF7F9CC4.toInt(), 0xFF4FC3F7.toInt(), 0xFF311B4A.toInt(),
    0xFF6A1B9A.toInt(), 0xFFBA68C8.toInt(), 0xFFF06292.toInt(), 0xFF880E4F.toInt(), 0xFF37474F.toInt(), 0xFF90A4AE.toInt()
)

fun Context.pastille(couleur: Int): View {
    val v = View(this)
    val forme = GradientDrawable()
    forme.cornerRadius = dp(7).toFloat()
    forme.setColor(couleur)
    forme.setStroke(dp(1), 0xFF9E9E9E.toInt())
    v.background = forme
    return v
}

// Ligne « nom : pastille de couleur » ; la pastille ouvre le nuancier.
fun Context.ligneCouleur(t: String, couleur: Int, change: (Int) -> Unit): LinearLayout {
    val r = rangee()
    r.addView(libelle(t))
    val p = pastille(couleur)
    p.setOnClickListener { choisirCouleur(t, couleur, change) }
    val lp = LinearLayout.LayoutParams(dp(46), dp(30))
    lp.leftMargin = dp(8)
    r.addView(p, lp)
    return r
}

// Nuancier : un toucher sur une case choisit la couleur ; on peut aussi taper un code #RRVVBB.
fun Context.choisirCouleur(titre: String, actuelle: Int, change: (Int) -> Unit) {
    val col = LinearLayout(this)
    col.orientation = LinearLayout.VERTICAL
    col.setPadding(dp(18), dp(8), dp(18), dp(4))
    var dialogue: AlertDialog? = null

    var ligne: LinearLayout? = null
    for ((i, c) in PALETTE.withIndex()) {
        if (i % 6 == 0) {
            ligne = LinearLayout(this)
            ligne.orientation = LinearLayout.HORIZONTAL
            col.addView(ligne, LinearLayout.LayoutParams(MATCH, WRAP))
        }
        val p = pastille(c)
        if (c == actuelle) {
            (p.background as GradientDrawable).setStroke(dp(3), Couleurs.ACCENT)
        }
        p.setOnClickListener {
            dialogue?.dismiss()
            change(c)
        }
        val lp = LinearLayout.LayoutParams(0, dp(38), 1f)
        lp.setMargins(dp(3), dp(3), dp(3), dp(3))
        ligne?.addView(p, lp)
    }

    val champ = EditText(this)
    champ.setText(String.format(Locale.ROOT, "#%06X", actuelle and 0xFFFFFF))
    champ.setTextColor(Couleurs.BLANC)
    champ.setSingleLine(true)
    champ.hint = "#RRVVBB"
    col.addView(champ, LinearLayout.LayoutParams(MATCH, WRAP).apply { topMargin = dp(10) })

    val defil = ScrollView(this)
    defil.addView(col)
    dialogue = AlertDialog.Builder(this, android.R.style.Theme_Material_Dialog_Alert)
        .setTitle(titre)
        .setView(defil)
        .setPositiveButton("Valider") { _, _ ->
            val code = champ.text.toString().trim().removePrefix("#")
            val v = if (code.length == 6) code.toIntOrNull(16) else null
            if (v != null) change(0xFF000000.toInt() or v)
        }
        .setNegativeButton("Annuler", null)
        .show()
}

fun Context.message(titre: String, texte: String) {
    AlertDialog.Builder(this, android.R.style.Theme_Material_Dialog_Alert)
        .setTitle(titre)
        .setMessage(texte)
        .setPositiveButton("Fermer", null)
        .show()
}

fun Context.confirmer(question: String, oui: String, action: () -> Unit) {
    AlertDialog.Builder(this, android.R.style.Theme_Material_Dialog_Alert)
        .setMessage(question)
        .setPositiveButton(oui) { _, _ -> action() }
        .setNegativeButton("Annuler", null)
        .show()
}

// Titre-logo Le Barrail : initiale manuscrite blanche, le reste en orange
// qui devient de plus en plus transparent vers le bas.
class TitreLogo(ctx: Context, private val mot: String, echelle: Float = 1f) : View(ctx) {

    private val d = ctx.resources.displayMetrics.density * echelle

    private val pInit = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 48f * d
        typeface = Typeface.create("cursive", Typeface.BOLD)
    }

    private val pReste = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 38f * d
        typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
    }

    private val initiale = mot.take(1)
    private val reste = mot.drop(1)

    override fun onMeasure(largeurDemandee: Int, hauteurDemandee: Int) {
        val l = pInit.measureText(initiale) + pReste.measureText(reste) + 8f * d
        val fm = pInit.fontMetrics
        val h = fm.descent - fm.ascent + 6f * d
        setMeasuredDimension(
            resolveSize(l.toInt(), largeurDemandee),
            resolveSize(h.toInt(), hauteurDemandee)
        )
    }

    override fun onDraw(canvas: Canvas) {
        val base = -pInit.fontMetrics.ascent + 3f * d
        canvas.drawText(initiale, 4f * d, base, pInit)
        val x = 4f * d + pInit.measureText(initiale) + 1f * d
        val haut = base + pReste.fontMetrics.ascent
        val bas = base + pReste.fontMetrics.descent
        pReste.shader = LinearGradient(
            0f, haut, 0f, bas,
            0xFFFF8C1A.toInt(), 0x30FF8C1A,
            Shader.TileMode.CLAMP
        )
        canvas.drawText(reste, x, base, pReste)
    }
}
