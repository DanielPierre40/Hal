package fr.lebarrail.hal

import android.app.Dialog
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.Window
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout

// Fenêtre « À propos », sur le modèle commun des applis Le Barrail :
// titre blanc et orange, logo du rideau, « Le Barrail », version.
object APropos {

    fun montrer(a: MainActivity) {
        val d = Dialog(a)
        d.requestWindowFeature(Window.FEATURE_NO_TITLE)

        // Marge autour de la fenêtre pour laisser voir l'ombre douce
        val cadre = FrameLayout(a)
        cadre.setPadding(a.dp(14), a.dp(14), a.dp(14), a.dp(14))

        val boite = LinearLayout(a)
        boite.orientation = LinearLayout.VERTICAL
        boite.gravity = Gravity.CENTER_HORIZONTAL
        boite.setPadding(a.dp(24), a.dp(20), a.dp(24), a.dp(18))
        val fond = GradientDrawable()
        fond.cornerRadius = a.dp(18).toFloat()
        fond.setColor(Couleurs.PANNEAU)
        fond.setStroke(a.dp(1), 0xFF808080.toInt())
        boite.background = fond
        boite.elevation = a.dp(10).toFloat()

        val titre = TitreLogo(a, "Hal", 0.9f)
        val lpTitre = LinearLayout.LayoutParams(WRAP, WRAP)
        lpTitre.gravity = Gravity.CENTER_HORIZONTAL
        titre.layoutParams = lpTitre
        boite.addView(titre)

        val logo = ImageView(a)
        logo.setImageResource(R.drawable.logo_rideau)
        logo.adjustViewBounds = true
        val lpLogo = LinearLayout.LayoutParams(a.dp(150), WRAP)
        lpLogo.gravity = Gravity.CENTER_HORIZONTAL
        lpLogo.topMargin = a.dp(10)
        logo.layoutParams = lpLogo
        boite.addView(logo)

        boite.addView(a.texte("Le Barrail", 13f, 0xFF8C8C8C.toInt()).apply {
            gravity = Gravity.CENTER
            setPadding(0, a.dp(10), 0, 0)
        })
        boite.addView(a.texte("version ${a.version}", 12f, 0xFF8C8C8C.toInt()).apply {
            gravity = Gravity.CENTER
            setTypeface(typeface, Typeface.ITALIC)
        })
        boite.addView(a.bouton("Fermer") { d.dismiss() })

        cadre.addView(boite, FrameLayout.LayoutParams(MATCH, WRAP))
        d.setContentView(cadre)
        d.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        d.show()
        d.window?.setLayout((a.resources.displayMetrics.widthPixels * 0.86f).toInt(), WRAP)
    }
}
