package com.multiclone.app

import android.app.admin.DevicePolicyManager
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Intent
import android.content.pm.LauncherApps
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.PopupWindow
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.util.UUID

class MainActivity : AppCompatActivity() {

    private lateinit var clones: MutableList<CloneItem>
    private lateinit var adapter: CloneAdapter
    private lateinit var emptyView: TextView

    private val pickApp = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { r ->
        val data = r.data ?: return@registerForActivityResult
        val pkg = data.getStringExtra("pkg") ?: return@registerForActivityResult
        val label = data.getStringExtra("label") ?: pkg
        clones.add(CloneItem(UUID.randomUUID().toString(), pkg, label))
        CloneStore.save(this, clones)
        adapter.notifyItemInserted(clones.size - 1)
        refreshEmpty()
        sendToProfile("install", pkg)   // clone space mein copy install karo
    }

    private val provision = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { r ->
        if (r.resultCode == RESULT_OK) {
            Toast.makeText(this, "Clone space ban rahi hai. Thori der baad + dabayein.", Toast.LENGTH_LONG).show()
        } else {
            Toast.makeText(this, "Clone space ka setup cancel ho gaya", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Ye clone space ki apni copy hai: yahan kuch nahi karna
        if (ProfileSetup.isProfileOwner(this)) {
            Toast.makeText(this, "Asli Multi Clone app ko main screen se kholein", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        // Home screen shortcut se aaye hain to seedha us clone ko kholo
        val shortcutCloneId = intent.getStringExtra("clone_id")
        if (shortcutCloneId != null) {
            CloneStore.load(this).find { it.id == shortcutCloneId }?.let { launchClone(it) }
            finish()
            return
        }

        setContentView(R.layout.activity_main)

        emptyView = findViewById(R.id.emptyView)
        val grid = findViewById<RecyclerView>(R.id.cloneGrid)
        clones = CloneStore.load(this)

        adapter = CloneAdapter(
            clones,
            onClick = { item -> launchClone(item) },
            onLongClick = { item, anchor -> showOptions(item, anchor) }
        )
        grid.layoutManager = GridLayoutManager(this, 3)
        grid.adapter = adapter
        refreshEmpty()

        findViewById<ImageView>(R.id.btnProfile).setOnClickListener { showSpaceStatus() }

        findViewById<ImageView>(R.id.btnMore).setOnClickListener { v ->
            val menu = PopupMenu(this, v, Gravity.END)
            menu.menu.add("Clone space setup")
            menu.menu.add("About")
            menu.setOnMenuItemClickListener {
                when (it.title.toString()) {
                    "Clone space setup" -> showSpaceStatus()
                    else -> showAbout()
                }
                true
            }
            menu.show()
        }

        findViewById<FloatingActionButton>(R.id.fabAdd).setOnClickListener {
            if (!hasCloneSpace()) {
                askSetup()
            } else {
                val exclude = ArrayList(clones.map { it.pkg })
                pickApp.launch(Intent(this, PickAppActivity::class.java).putStringArrayListExtra("exclude", exclude))
            }
        }
    }

    private fun refreshEmpty() {
        emptyView.visibility = if (clones.isEmpty()) View.VISIBLE else View.GONE
    }

    // ---------- Clone space (work profile) ----------
    private fun hasCloneSpace(): Boolean =
        getSystemService(LauncherApps::class.java).profiles.size > 1

    private fun showSpaceStatus() {
        val ready = hasCloneSpace()
        val b = AlertDialog.Builder(this)
            .setTitle("Clone space")
            .setMessage(
                if (ready) "Clone space tayyar hai. + dabakar app chunein."
                else "Clone space abhi nahi bani. Isi mein apps ki alag copy chalti hai."
            )
            .setNegativeButton("Band karein", null)
        if (!ready) b.setPositiveButton("Setup karein") { _, _ -> startSetup() }
        b.show()
    }

    private fun askSetup() {
        AlertDialog.Builder(this)
            .setTitle("Clone space chahiye")
            .setMessage(
                "Apps ki alag copy chalane ke liye Android ki \"work profile\" banani paregi. " +
                    "Ye sirf ek baar hota hai. Aage Android ki screen aayegi, wahan Accept dabayein."
            )
            .setPositiveButton("Setup karein") { _, _ -> startSetup() }
            .setNegativeButton("Baad mein", null)
            .show()
    }

    private fun startSetup() {
        val dpm = getSystemService(DevicePolicyManager::class.java)
        val admin = ComponentName(this, AdminReceiver::class.java)
        val intent = Intent(DevicePolicyManager.ACTION_PROVISION_MANAGED_PROFILE)
            .putExtra(DevicePolicyManager.EXTRA_PROVISIONING_DEVICE_ADMIN_COMPONENT_NAME, admin)
            .putExtra(DevicePolicyManager.EXTRA_PROVISIONING_LEAVE_ALL_SYSTEM_APPS_ENABLED, true)

        if (intent.resolveActivity(packageManager) == null) {
            Toast.makeText(this, "Aap ka phone work profile support nahi karta", Toast.LENGTH_LONG).show()
            return
        }
        if (!dpm.isProvisioningAllowed(DevicePolicyManager.ACTION_PROVISION_MANAGED_PROFILE)) {
            Toast.makeText(
                this,
                "Is phone par clone space nahi ban sakti (company ne band ki hai ya pehle se work profile hai)",
                Toast.LENGTH_LONG
            ).show()
            return
        }
        provision.launch(intent)
    }

    /** Clone space ki copy ko kaam bhejo (cross-profile intent). */
    private fun sendToProfile(op: String, pkg: String) {
        if (!hasCloneSpace()) {
            askSetup()
            return
        }
        val i = Intent(ProfileSetup.actionName(this)).putExtra("op", op).putExtra("pkg", pkg)
        try {
            startActivity(i)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(
                this,
                "Clone space abhi tayyar nahi. Thori der baad try karein ya Clone space setup dobara chalayein.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun launchClone(item: CloneItem) = sendToProfile("launch", item.pkg)

    private fun showAbout() {
        AlertDialog.Builder(this)
            .setTitle("Multi Clone")
            .setMessage("Apps ki alag copy chalane wali app. Koi ads nahi, koi VIP nahi.")
            .setPositiveButton("OK", null)
            .show()
    }

    // ---------- Long-press options menu ----------
    private fun showOptions(item: CloneItem, anchor: View) {
        val content = layoutInflater.inflate(R.layout.popup_clone_menu, null)
        val width = (200 * resources.displayMetrics.density).toInt()
        val popup = PopupWindow(content, width, ViewGroup.LayoutParams.WRAP_CONTENT, true)
        popup.elevation = 16f
        popup.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        fun bind(id: Int, action: () -> Unit) {
            content.findViewById<View>(id).setOnClickListener { popup.dismiss(); action() }
        }
        bind(R.id.optHome) { pinShortcut(item) }
        bind(R.id.optRename) { renameClone(item) }
        bind(R.id.optDelete) { deleteClone(item) }
        bind(R.id.optRepair) { sendToProfile("repair", item.pkg) }

        popup.showAsDropDown(anchor, (anchor.width - width) / 2, -anchor.height / 3)
    }

    private fun renameClone(item: CloneItem) {
        val input = EditText(this).apply {
            setText(item.label)
            setSelection(text.length)
            setSingleLine()
        }
        AlertDialog.Builder(this)
            .setTitle("Edit the name")
            .setView(input)
            .setPositiveButton("Save") { _, _ ->
                val newName = input.text.toString().trim()
                if (newName.isNotEmpty()) {
                    item.label = newName
                    CloneStore.save(this, clones)
                    adapter.notifyItemChanged(clones.indexOf(item))
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteClone(item: CloneItem) {
        AlertDialog.Builder(this)
            .setTitle("Delete App")
            .setMessage(
                "\"${item.label}\" ka clone hata dein?\n" +
                    "Is clone ka sara data (chats waghera) mit jayega. Asli app par koi asar nahi parega."
            )
            .setPositiveButton("Delete") { _, _ ->
                if (hasCloneSpace()) sendToProfile("remove", item.pkg)
                val index = clones.indexOf(item)
                if (index >= 0) {
                    clones.removeAt(index)
                    CloneStore.save(this, clones)
                    adapter.notifyItemRemoved(index)
                    refreshEmpty()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun pinShortcut(item: CloneItem) {
        if (!ShortcutManagerCompat.isRequestPinShortcutSupported(this)) {
            Toast.makeText(this, "Is launcher mein shortcut support nahi hai", Toast.LENGTH_LONG).show()
            return
        }
        val bitmap = try {
            packageManager.getApplicationIcon(item.pkg).toBitmap(128, 128)
        } catch (e: Exception) {
            Toast.makeText(this, "App install nahi hai", Toast.LENGTH_SHORT).show()
            return
        }
        val launch = Intent(this, MainActivity::class.java)
            .setAction(Intent.ACTION_VIEW)
            .putExtra("clone_id", item.id)
        val info = ShortcutInfoCompat.Builder(this, "clone_${item.id}")
            .setShortLabel(item.label)
            .setIcon(IconCompat.createWithBitmap(bitmap))
            .setIntent(launch)
            .build()
        ShortcutManagerCompat.requestPinShortcut(this, info, null)
    }
}
