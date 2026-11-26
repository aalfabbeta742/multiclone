package com.multiclone.app

import android.content.Intent
import android.content.pm.ApplicationInfo
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class PickAppActivity : AppCompatActivity() {

    data class AppRow(val pkg: String, val label: String)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pick_app)

        // Jin apps ka clone pehle se hai wo dobara nahi dikhate (ek app ka ek hi clone ban sakta hai)
        val exclude = intent.getStringArrayListExtra("exclude")?.toSet() ?: emptySet()

        val launcherIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val rows = packageManager.queryIntentActivities(launcherIntent, 0)
            .map { it.activityInfo.applicationInfo }
            .filter { (it.flags and ApplicationInfo.FLAG_SYSTEM) == 0 }   // system apps nahi
            .filter { it.packageName != packageName }                      // apni app nahi
            .filter { it.packageName !in exclude }
            .distinctBy { it.packageName }
            .map { AppRow(it.packageName, packageManager.getApplicationLabel(it).toString()) }
            .sortedBy { it.label.lowercase() }

        if (rows.isEmpty()) {
            Toast.makeText(this, "Clone karne ke liye koi app nahi mili", Toast.LENGTH_LONG).show()
        }

        val list = findViewById<RecyclerView>(R.id.appList)
        list.layoutManager = LinearLayoutManager(this)
        list.adapter = object : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
            override fun onCreateViewHolder(p: ViewGroup, t: Int): RecyclerView.ViewHolder {
                val v = LayoutInflater.from(p.context).inflate(R.layout.item_app, p, false)
                return object : RecyclerView.ViewHolder(v) {}
            }

            override fun onBindViewHolder(h: RecyclerView.ViewHolder, i: Int) {
                val row = rows[i]
                h.itemView.findViewById<TextView>(R.id.appName).text = row.label
                h.itemView.findViewById<ImageView>(R.id.appIcon)
                    .setImageDrawable(packageManager.getApplicationIcon(row.pkg))
                h.itemView.setOnClickListener {
                    setResult(RESULT_OK, Intent().putExtra("pkg", row.pkg).putExtra("label", row.label))
                    finish()
                }
            }

            override fun getItemCount() = rows.size
        }

        findViewById<View>(R.id.btnBack).setOnClickListener { finish() }
    }
}
