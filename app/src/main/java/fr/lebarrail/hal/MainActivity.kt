package fr.lebarrail.hal

import android.app.Activity
import android.content.ContentValues
import android.os.Bundle
import android.provider.MediaStore
import android.text.TextUtils
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import java.util.Locale

// Fenêtre unique de Hal : la liste des applications du téléphone, avec pour
// chacune sa consommation de batterie et son temps de processeur.
class MainActivity : Activity() {

    private lateinit var contenu: LinearLayout
    private var resultat: Moteur.Resultat? = null
    private var enCours = false
    private var parBatterie = true

    val version: String
        get() = try {
            packageManager.getPackageInfo(packageName, 0).versionName ?: "?"
        } catch (e: Exception) {
            "?"
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        contenu = echafaudage()
        if (Moteur.autorise(this)) mesurer() else construire()
    }

    private fun mesurer() {
        if (enCours) return
        enCours = true
        construire()
        Thread {
            val r = Moteur.mesurer(this)
            runOnUiThread {
                resultat = r
                enCours = false
                construire()
            }
        }.start()
    }

    // ---------------------------------------------------------------- écran

    private fun construire() {
        contenu.removeAllViews()

        // Bandeau : titre-logo à gauche, À propos à droite.
        val bandeau = rangee()
        bandeau.addView(TitreLogo(this, "Hal", 0.75f))
        bandeau.addView(View(this).apply {
            layoutParams = LinearLayout.LayoutParams(0, 0, 1f)
        })
        bandeau.addView(texte("À propos", 14f, Couleurs.ORANGE).apply {
            layoutParams = LinearLayout.LayoutParams(WRAP, WRAP)
            setPadding(dp(10), dp(8), dp(4), dp(8))
            isClickable = true
            setOnClickListener { APropos.montrer(this@MainActivity) }
        })
        contenu.addView(bandeau)

        if (!Moteur.autorise(this)) {
            construireAutorisation()
            return
        }
        construireCommandes()
        construireListe()
    }

    // Carte montrée tant que l'autorisation n'a pas été donnée depuis le PC.
    private fun construireAutorisation() {
        val carte = carte()
        carte.addView(texte("Une autorisation à donner, une seule fois", 16f, Couleurs.ORANGE, true))
        carte.addView(texte(
            "Android cache le détail de la consommation aux applications ordinaires. " +
                "Pour ouvrir son compteur à Hal, branchez le téléphone au PC en USB " +
                "(comme pour Débarcadère) et lancez ces deux commandes :",
            14f, Couleurs.GRIS
        ))
        carte.addView(texte(Moteur.COMMANDES_ADB, 12f, Couleurs.BLANC).apply {
            setPadding(0, dp(6), 0, dp(6))
        })
        carte.addView(texte(
            "C'est à faire une seule fois : l'autorisation survit aux redémarrages " +
                "du téléphone et aux mises à jour de Hal.",
            13f, Couleurs.GRIS
        ))
        carte.addView(bouton("C'est fait, vérifier") {
            if (Moteur.autorise(this)) mesurer() else construire()
        })
        contenu.addView(carte)
    }

    // Carte du haut : période, boutons Actualiser / Tri / Banc d'essai.
    private fun construireCommandes() {
        val carte = carte()
        carte.addView(titreCarte("Depuis la dernière charge complète"))
        val r = resultat
        when {
            enCours -> carte.addView(texte("Mesure en cours…", 14f, Couleurs.GRIS))
            r?.erreur != null -> carte.addView(texte(r.erreur, 14f, Couleurs.ROUGE))
            r != null -> {
                val totalMah = r.lignes.sumOf { it.mah }
                val totalCpu = r.lignes.sumOf { it.cpuMs }
                carte.addView(texte(
                    "Total mesuré : ${fmtMah(totalMah)} de batterie, ${fmtCpu(totalCpu)} de processeur. " +
                        "Le compteur repart de zéro à chaque charge complète.",
                    13f, Couleurs.GRIS
                ))
            }
        }
        val ligne = rangee()
        ligne.addView(petitBouton("Actualiser") { mesurer() })
        ligne.addView(petitBouton(if (parBatterie) "Tri : batterie" else "Tri : processeur") {
            parBatterie = !parBatterie
            construire()
        })
        ligne.addView(petitBouton("Banc d'essai") { exporterBrut() })
        carte.addView(ligne)
        contenu.addView(carte)
    }

    // La liste elle-même : une ligne par application, deux colonnes de valeurs.
    private fun construireListe() {
        val r = resultat ?: return
        if (r.erreur != null || r.lignes.isEmpty()) return

        val carte = carte()
        val entete = rangee()
        entete.addView(texte("Application", 13f, Couleurs.ACCENT, true).apply {
            layoutParams = LinearLayout.LayoutParams(0, WRAP, 1f)
        })
        entete.addView(colonne("Batterie", Couleurs.ACCENT, true))
        entete.addView(colonne("Processeur", Couleurs.ACCENT, true))
        carte.addView(entete)

        val tri = if (parBatterie) r.lignes.sortedByDescending { it.mah }
        else r.lignes.sortedByDescending { it.cpuMs }
        val montrees = tri.take(40)
        for (l in montrees) {
            val ligne = rangee()
            ligne.addView(texte(l.nom, 14f).apply {
                layoutParams = LinearLayout.LayoutParams(0, WRAP, 1f)
                maxLines = 1
                ellipsize = TextUtils.TruncateAt.END
            })
            ligne.addView(colonne(fmtMah(l.mah), Couleurs.BLANC, false))
            ligne.addView(colonne(fmtCpu(l.cpuMs), Couleurs.BLANC, false))
            carte.addView(ligne)
        }
        val reste = tri.size - montrees.size
        if (reste > 0) {
            carte.addView(texte("… et $reste autres, à la part négligeable.", 13f, Couleurs.GRIS))
        }
        contenu.addView(carte)
    }

    private fun colonne(t: String, couleur: Int, gras: Boolean): TextView =
        texte(t, 13f, couleur, gras).apply {
            layoutParams = LinearLayout.LayoutParams(dp(88), WRAP)
            gravity = Gravity.END
            maxLines = 1
        }

    // ---------------------------------------------------------------- outils

    private fun fmtMah(v: Double): String = when {
        v >= 100 -> String.format(Locale.FRANCE, "%.0f mAh", v)
        v >= 1 -> String.format(Locale.FRANCE, "%.1f mAh", v)
        else -> String.format(Locale.FRANCE, "%.2f mAh", v)
    }

    private fun fmtCpu(ms: Long): String {
        val s = ms / 1000
        return when {
            s >= 3600 -> String.format(Locale.FRANCE, "%d h %02d min", s / 3600, (s % 3600) / 60)
            s >= 60 -> String.format(Locale.FRANCE, "%d min %02d s", s / 60, s % 60)
            else -> "$s s"
        }
    }

    // Banc d'essai : dépose le relevé brut d'Android dans Téléchargements,
    // pour pouvoir l'examiner si une valeur semble étrange.
    private fun exporterBrut() {
        val r = resultat ?: return
        try {
            val v = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, "Hal banc d'essai.txt")
                put(MediaStore.Downloads.MIME_TYPE, "text/plain")
            }
            val u = contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, v)
            if (u == null) {
                message("Banc d'essai", "Le téléphone a refusé la création du fichier.")
                return
            }
            contentResolver.openOutputStream(u)?.use { it.write(r.brut.toByteArray()) }
            message("Banc d'essai", "Le relevé brut d'Android est déposé dans Téléchargements, sous « Hal banc d'essai.txt ».")
        } catch (e: Exception) {
            message("Banc d'essai", "Échec : ${e.message}")
        }
    }
}
