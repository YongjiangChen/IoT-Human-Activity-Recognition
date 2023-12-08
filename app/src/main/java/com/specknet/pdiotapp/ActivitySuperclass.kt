package com.specknet.pdiotapp

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ImageView
import android.widget.ListView
import android.widget.TextView
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.Toolbar
import androidx.drawerlayout.widget.DrawerLayout
import com.bumptech.glide.Glide
import com.specknet.pdiotapp.bluetooth.ConnectingActivity
import com.specknet.pdiotapp.live.ClassifyActivity
import com.specknet.pdiotapp.live.ClassifyActivityTree
import com.specknet.pdiotapp.utils.Utils

abstract class ActivitySuperclass : AppCompatActivity() {

    // menu drawer:
    private lateinit var mPageTitles: Array<String?>
    private lateinit var mPageIcons: Array<Int?>
    private lateinit var mDrawerLayout: DrawerLayout
    private lateinit var mDrawerList: ListView
    private lateinit var mDrawerTitle: CharSequence
    private lateinit var mTitle: CharSequence
    private lateinit var mDrawerToggle: ActionBarDrawerToggle
    private lateinit var toolbar: Toolbar
    private lateinit var darkMode: ImageView

    abstract var currentContentView: Int

    inner class DrawerItemClickListener : AdapterView.OnItemClickListener {
        override fun onItemClick(
            parent: AdapterView<*>?,
            view: View?,
            position: Int,
            id: Long
        ) {
            selectItem(position)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (AppCompatDelegate.getDefaultNightMode() == AppCompatDelegate.MODE_NIGHT_YES) {
            setTheme(R.style.DarkTheme)
        } else {
            setTheme(R.style.AppTheme)
        }

        setContentView(currentContentView)
        setupDrawer()
        Log.w(this.toString(), Utils.currentView.toString())
        selectItem(Utils.currentView)
        var text: TextView = findViewById(R.id.toolbar_text)
        text.text = mPageTitles[Utils.currentView]

        darkMode = findViewById(R.id.dark_mode)
        darkMode.setOnClickListener {
            Log.w("Toolbar", "Darkmode button pressed")
            if (AppCompatDelegate.getDefaultNightMode() == AppCompatDelegate.MODE_NIGHT_YES) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            } else {
                Log.w("Darkmode", "OFF")
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            }
        }
    }

    fun selectItem(position: Int)
    {
        Log.w("Toolbar", "menu item $position clicked")
        when (position)
        {
            0 -> {
                if (this !is MainActivity) {
                    val intent = Intent(this, MainActivity::class.java)
                    startActivity(intent)
                }
            }
            1 -> {
                if (this !is ConnectingActivity) {
                    val intent = Intent(this, ConnectingActivity::class.java)
                    startActivity(intent)
                }
            }
            2 -> {
                if (this !is RecordingActivity) {
                    val intent = Intent(this, RecordingActivity::class.java)
                    startActivity(intent)
                }
            }
            3 -> {
                if (this !is ClassifyActivity) {
                    val intent = Intent(this, ClassifyActivity::class.java)
                    startActivity(intent)
                }
            }
            4 -> {
                if (this !is ClassifyActivityTree) {
                    val intent = Intent(this, ClassifyActivityTree::class.java)
                    startActivity(intent)
                }
            }
            5 -> {
                if (this !is HistoricDataActivity) {
                    val intent = Intent(this, HistoricDataActivity::class.java)
                    startActivity(intent)
                }
            }

        }
        mDrawerList.setItemChecked(position, true)
        mDrawerList.setSelection(position)
        mDrawerLayout.closeDrawer(mDrawerList)
        Utils.currentView = position
        var currentImage: ImageView = findViewById(R.id.current_view_pic)
        mPageIcons[position]?.let { currentImage.setImageResource(it) }
    }

    private fun beginActivity(activity: ActivitySuperclass, position: Int)
    {
        val intent = Intent(this, activity::class.java)
        Glide.with(this).load(mPageIcons[position]).into(findViewById(R.id.current_view_pic))
        startActivity(intent)
    }

    private fun setupDrawer()
    {
        mTitle = title
        mDrawerTitle = title
        mDrawerLayout = findViewById(R.id.drawer_layout)
        mDrawerList = findViewById(R.id.left_drawer)

        // Toolbar:
        toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(true);


        var numPages: Int = 6
        mPageTitles = arrayOfNulls<String>(numPages)
        mPageTitles[0] = "Home"
        mPageTitles[1] = "Connect"
        mPageTitles[2] = "Record Data"
        mPageTitles[3] = "Classify Data Curriculum Learning Model"
        mPageTitles[4] = "Classify Data Tree Model"
        mPageTitles[5] = "View Historic Data"

        mPageIcons = arrayOfNulls<Int>(numPages)
        mPageIcons[0] = R.drawable.home
        mPageIcons[1] = R.drawable.connect
        mPageIcons[2] = R.drawable.record
        mPageIcons[3] = R.drawable.classify
        mPageIcons[4] = R.drawable.classify
        mPageIcons[5] = R.drawable.historic

        val drawerItem = arrayOfNulls<DataModel>(numPages)
        drawerItem[0] = DataModel(R.drawable.home, "Home")
        drawerItem[1] = DataModel(R.drawable.connect, "Connect Sensors")
        drawerItem[2] = DataModel(R.drawable.record, "Record Data")
        drawerItem[3] = DataModel(R.drawable.classify, "Classify Data Curriculum Learning Model")
        drawerItem[4] = DataModel(R.drawable.classify, "Classify Data Tree Model")
        drawerItem[5] = DataModel(R.drawable.historic, "View Historic Data")

        var adapter: DrawerItemCustomAdapter = DrawerItemCustomAdapter(this, R.layout.list_view_item_row, drawerItem)
        mDrawerList.adapter = adapter
        mDrawerList.onItemClickListener = DrawerItemClickListener()
        mDrawerLayout = findViewById(R.id.drawer_layout)

        mDrawerToggle = ActionBarDrawerToggle(
            this,
            mDrawerLayout,
            toolbar,
            R.string.app_name,
            R.string.app_name
        )
        mDrawerToggle.syncState()

        mDrawerLayout.setDrawerListener(mDrawerToggle)

        if (AppCompatDelegate.getDefaultNightMode() == AppCompatDelegate.MODE_NIGHT_YES) {
            mDrawerToggle?.drawerArrowDrawable?.color = resources.getColor(R.color.white)
        } else {
            mDrawerToggle?.drawerArrowDrawable?.color = resources.getColor(R.color.colorAccent)
        }
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        mDrawerToggle.syncState()
    }
}