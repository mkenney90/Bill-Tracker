package com.example.billtracker

import android.graphics.Color
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import android.widget.TableRow
import android.widget.TextView
import kotlinx.android.synthetic.main.activity_scrolling.*
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.toast
import org.jetbrains.anko.uiThread
import khttp.*
import java.lang.Exception
import java.net.URL
import kotlin.collections.ArrayList

class ScrollingActivity : AppCompatActivity() {

    private var setPayStatus = false
    private val billsArray = ArrayList<TextView>()
    private val paidArray = IntArray(60)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_scrolling)
        setSupportActionBar(toolbar)

        val max = 14

        for (i in 2 until max) {
            val row = tableBills.getChildAt(i) as TableRow
            val x = (i - 1)
            row.tag = "${x}_bill"
            for (j in 0 until 5) {
                val tvnew = TextView(this)
                tvnew.setBackgroundColor(Color.GRAY)
                tvnew.textSize = 24f
                row.addView(tvnew)
                billsArray.add(tvnew)

                val id = "$x$j".toInt()
                tvnew.id = id

                val params = tvnew.layoutParams as TableRow.LayoutParams
                params.setMargins(3,2,2,2)
                tvnew.layoutParams = params

                tvnew.setOnClickListener{
                    if (setPayStatus) {
                        val paidId = it.id

                        for ((idx, v) in billsArray.withIndex()) {
                            if (v.id == paidId) {
                                if (paidArray[idx] == 1) {
                                    billsArray[idx].setBackgroundColor(getColor(R.color.colorNull))
                                    billsArray[idx].setBackgroundResource(R.drawable.rounded_corners)
                                    paidArray[idx] = 0
                                } else {
                                    billsArray[idx].setBackgroundColor(getColor(R.color.colorPaid))
                                    billsArray[idx].setBackgroundResource(R.drawable.rounded_corners_paid)
                                    paidArray[idx] = 1
                                }
                                break
                            }
                        }

                    }
                }
            }
        }

        getNetworkData()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_scrolling, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.

        when (item.itemId) {
            R.id.action_setstatus -> {
                setPayStatus = true
                toolbar_layout.setBackgroundColor(getColor(R.color.activePrimary))
                toolbar.setBackgroundColor(getColor(R.color.activePrimary))
            }
            R.id.action_finish -> {
                setPayStatus = false
                toolbar_layout.setBackgroundColor(Color.parseColor("#8A55DA"))
                toolbar.setBackgroundColor(Color.parseColor("#8A55DA"))
                writeNetworkData()
            }
            else -> super.onOptionsItemSelected(item)
        }

        return super.onOptionsItemSelected(item)

    }

    private fun getNetworkData() {

        doAsync {
            println("async")

            try {
                val response = URL("http://www.staminadamage.com/billtracker/bills.txt").readText()

                if (response.isEmpty()) {
                    uiThread {
                        toast("Empty response")
                    }
                    return@doAsync
                }
                val lines = response.lines()

                for ((i, l) in lines.withIndex()) {
                    if (l.isNotEmpty()) {
                        val start = l.indexOf("{")
                        val out = l.substring(start + 1, start + 10).replace(",", "")
                        val list = out.map { it.toString().toInt() }.toIntArray()

                        runOnUiThread {
                            for ((j, n) in list.withIndex()) {
                                val cell = (i * 5 + j)
                                if (n == 1) {
                                    billsArray[cell].setBackgroundResource(R.drawable.rounded_corners_paid)
                                    paidArray[cell] = 1
                                } else {
                                    billsArray[cell].setBackgroundResource(R.drawable.rounded_corners)
                                    paidArray[cell] = 0
                                }
                            }
                        }

                    }
                }
                uiThread {
                    toast("Got network data")
                }
            } catch (e : Exception) {
                uiThread {
                    toast(e.localizedMessage as CharSequence)
                }
            }

        }
    }

    private fun writeNetworkData() : String {
        var output = ""

        for (x in 0 until 12) {
            output = "$output${x+1}={"
            for (y in 0 until 5) {
                output = "$output${paidArray[x*5+y]}"
                if (y < 4) {
                    output = "$output,"
                }
            }
            output = "$output}\n"
        }
        val postData = output

        doAsync {
            try {
                val r = sendPostRequest(postData)

                if (r == 200) {
                    uiThread {
                        toast("Success")
                    }
                }
            } catch (e : Exception) {
                uiThread {
                    toast(e.localizedMessage as CharSequence)
                }
            }
        }
        return output
    }

    private fun sendPostRequest(data:String) : Int {

        val r = post(
            url = "http://www.staminadamage.com/billtracker/update.php",
            data = mapOf("data" to data)
        )

        return r.statusCode
    }
}
