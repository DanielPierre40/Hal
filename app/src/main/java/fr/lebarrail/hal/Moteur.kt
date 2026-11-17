package fr.lebarrail.hal

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager

// Lit le compteur interne d'Android (batterystats) : pour chaque application,
// sa consommation de batterie en mAh et son temps de processeur, depuis la
// dernière charge complète du téléphone.
object Moteur {

    // Autorisations données une seule fois depuis le PC, téléphone branché en USB.
    const val COMMANDES_ADB =
        "adb shell pm grant fr.lebarrail.hal android.permission.BATTERY_STATS\n" +
            "adb shell pm grant fr.lebarrail.hal android.permission.DUMP"

    class Ligne(val nom: String, val mah: Double, val cpuMs: Long)
    class Resultat(val lignes: List<Ligne>, val erreur: String?, val brut: String)

    fun autorise(ctx: Context): Boolean =
        ctx.checkSelfPermission(Manifest.permission.BATTERY_STATS) == PackageManager.PERMISSION_GRANTED &&
            ctx.checkSelfPermission(Manifest.permission.DUMP) == PackageManager.PERMISSION_GRANTED

    // À lancer hors du fil principal : la lecture prend une à deux secondes.
    fun mesurer(ctx: Context): Resultat {
        val brut = try {
            val p = ProcessBuilder("dumpsys", "batterystats", "--checkin")
                .redirectErrorStream(true)
                .start()
            val s = p.inputStream.bufferedReader().readText()
            p.waitFor()
            s
        } catch (e: Exception) {
            return Resultat(emptyList(), "Lecture impossible : ${e.message}", "")
        }
        if ("Permission Denial" in brut || brut.length < 200) {
            return Resultat(emptyList(), "Le téléphone refuse l'accès à son compteur.", brut)
        }

        // Lignes du relevé : 9,<uid>,l,pwi,uid,<mAh>  et  9,<uid>,l,cpu,<ms util.>,<ms syst.>,…
        val mah = HashMap<Int, Double>()
        val cpu = HashMap<Int, Long>()
        for (l in brut.lineSequence()) {
            val f = l.split(',')
            if (f.size < 6 || f[2] != "l") continue
            val uid = f[1].toIntOrNull() ?: continue
            when (f[3]) {
                "pwi" -> if (f[4] == "uid") {
                    f[5].toDoubleOrNull()?.let { mah[uid] = (mah[uid] ?: 0.0) + it }
                }
                "cpu" -> {
                    val u = f[4].toLongOrNull() ?: 0L
                    val s = f[5].toLongOrNull() ?: 0L
                    cpu[uid] = (cpu[uid] ?: 0L) + u + s
                }
            }
        }

        val pm = ctx.packageManager
        val lignes = (mah.keys + cpu.keys)
            .map { uid -> Ligne(nomPour(pm, uid), mah[uid] ?: 0.0, cpu[uid] ?: 0L) }
            .filter { it.mah >= 0.005 || it.cpuMs >= 1000 }
        if (lignes.isEmpty()) {
            return Resultat(emptyList(), "Relevé lu, mais aucune ligne par application trouvée.", brut)
        }
        return Resultat(lignes, null, brut)
    }

    // Le nom de l'application qui se cache derrière un uid Android.
    private fun nomPour(pm: PackageManager, uid: Int): String {
        when (uid) {
            0 -> return "Noyau Android"
            1000 -> return "Système Android"
            1001 -> return "Téléphonie"
            1013 -> return "Multimédia (système)"
            1027 -> return "NFC (système)"
            1041 -> return "Audio (système)"
            2000 -> return "Interpréteur (shell)"
        }
        return try {
            val paquets = pm.getPackagesForUid(uid)
            if (paquets.isNullOrEmpty()) {
                "uid $uid"
            } else {
                val noms = paquets.mapNotNull { p ->
                    try {
                        pm.getApplicationInfo(p, 0).loadLabel(pm).toString()
                    } catch (e: Exception) {
                        null
                    }
                }
                noms.minByOrNull { it.length } ?: paquets[0]
            }
        } catch (e: Exception) {
            "uid $uid"
        }
    }
}
