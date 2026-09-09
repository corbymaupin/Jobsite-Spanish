package com.corbymaupin.jobsitespanish.data

import android.content.Context
import org.json.JSONArray

class TermsRepository(private val context: Context) {

    @Volatile
    private var cached: List<Term>? = null

    fun loadTerms(): List<Term> {
        cached?.let { return it }
        val json = context.assets.open("terms.json").bufferedReader().use { it.readText() }
        val arr = JSONArray(json)
        val out = ArrayList<Term>(arr.length())
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            val en = o.getString("en")
            val es = o.getString("es")
            val id = if (o.has("id")) o.getString("id") else "$en|$es"
            out.add(
                Term(
                    id = id,
                    en = en,
                    es = es,
                    trade = o.getString("trade"),
                    region = o.optString("region", "")
                )
            )
        }
        cached = out
        return out
    }

    fun trades(): List<String> =
        loadTerms().map { it.trade }.distinct()
}
