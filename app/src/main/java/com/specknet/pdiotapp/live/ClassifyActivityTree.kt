package com.specknet.pdiotapp.live

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.util.Log
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet
import com.specknet.pdiotapp.*
import com.specknet.pdiotapp.utils.Constants
import com.specknet.pdiotapp.utils.RESpeckLiveData
import com.specknet.pdiotapp.utils.ThingyLiveData
import org.jetbrains.annotations.NotNull
import kotlin.collections.ArrayList
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel


class ClassifyActivityTree : ActivitySuperclass() {

    override var currentContentView = R.layout.activity_classification_tree

    // global graph variables
    lateinit var dataSet_res_accel_x: LineDataSet
    lateinit var dataSet_res_accel_y: LineDataSet
    lateinit var dataSet_res_accel_z: LineDataSet

    lateinit var dataSet_thingy_accel_x: LineDataSet
    lateinit var dataSet_thingy_accel_y: LineDataSet
    lateinit var dataSet_thingy_accel_z: LineDataSet

    var time = 0f
    lateinit var allRespeckData: LineData

    lateinit var allThingyData: LineData

    lateinit var respeckChart: LineChart
    lateinit var thingyChart: LineChart

    // global broadcast receiver so we can unregister it
    lateinit var respeckLiveUpdateReceiver: BroadcastReceiver
    lateinit var thingyLiveUpdateReceiver: BroadcastReceiver
    lateinit var combinedLiveUpdateReceiver: BroadcastReceiver
    lateinit var looperRespeck: Looper
    lateinit var looperThingy: Looper
    lateinit var looperCombined: Looper

    val filterTestRespeck = IntentFilter(Constants.ACTION_RESPECK_LIVE_BROADCAST)
    val filterTestThingy = IntentFilter(Constants.ACTION_THINGY_BROADCAST)

    var inputValueRespeck = Array(1) {
        Array(50) {
            FloatArray(6)
        }
    }
    var outputValueRespeck = Array(1) {
        FloatArray(13)
    }

    var outputValueRespeckChild = Array(1) {
        FloatArray(13)
    }

    var inputValueThingy = Array(1) {
        Array(50) {
            FloatArray(6)
        }
    }
    var outputValueThingy = Array(1) {
        FloatArray(13)
    }

    var outputValueThingyChild = Array(1) {
        FloatArray(13)
    }
    var bufferCount = 0
    var confidenceFreshhold = 0.85
    var movementBias = 0.20 as Float

    lateinit var tflite: Interpreter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setupCharts()
        var thingyTextView: TextView = findViewById(R.id.thingy_classification)
        var respeckTextView: TextView = findViewById(R.id.respeck_classification)
        var combinedTextView: TextView = findViewById(R.id.combined_classification)

        // set up the broadcast receiver
        respeckLiveUpdateReceiver = object : BroadcastReceiver() {
            @SuppressLint("SetTextI18n")
            override fun onReceive(context: Context, intent: Intent) {

                Log.i("thread", "I am running on thread = " + Thread.currentThread().name)

                val action = intent.action

                if (action == Constants.ACTION_RESPECK_LIVE_BROADCAST) {

                    val liveData =
                        intent.getSerializableExtra(Constants.RESPECK_LIVE_DATA) as RESpeckLiveData
                    Log.d("Live", "onReceive: liveData = " + liveData)

                    // get all relevant intent contents
                    val x = liveData.accelX
                    val y = liveData.accelY
                    val z = liveData.accelZ

                    if (bufferCount >= 25) {
                        Interpreter(loadModelFile("RESPEC_ROOT_OF_TREE_MODEL_5CLASSES.tflite")).use { interpreter ->
                            interpreter.run(inputValueRespeck, outputValueRespeck)}
                        //search "Predicted output" in Run terminal to see the outputs
                        Log.i("Predicted outputValue", outputValueRespeck.contentDeepToString())
                        outputValueRespeck[0][4] -= movementBias  // for the bias against movement
                        val maxIdx = outputValueRespeck[0].indices.maxBy { outputValueRespeck[0][it] } ?: -1
                        Log.i("maxIdx activity identifier", outputValueRespeck.contentDeepToString())
                        if (maxIdx == 0) {
                            respeckTextView.text = "Recogized activity: Lying Down : Running Children models : "
                            Interpreter(loadModelFile("RESPEC_LYING_DOWN_MODEL_4CLASSES.tflite")).use { interpreter ->
                                interpreter.run(inputValueRespeck, outputValueRespeckChild)}
                            val maxIdx_child = outputValueRespeckChild[0].indices.maxBy { outputValueRespeckChild[0][it] } ?: -1
                            if (maxIdx_child == 0) {respeckTextView.text = respeckTextView.text as String + " Left"
                            }
                            else if (maxIdx_child == 1) {respeckTextView.text = respeckTextView.text as String + " On Back"
                            }
                            else if (maxIdx_child == 2) {
                                respeckTextView.text = respeckTextView.text as String + " On Stomach"
                            }
                            else if (maxIdx_child == 3) {
                                respeckTextView.text = respeckTextView.text as String + " Right"
                            }
                        }
                        else if (maxIdx == 1) {
                            respeckTextView.text = "Recogized activity: Sitting : Running Children models"
                            Interpreter(loadModelFile("RESPEC_SITTING_MODEL_4CLASSES.tflite")).use { interpreter ->
                                interpreter.run(inputValueRespeck, outputValueRespeckChild)}
                            val maxIdx_child = outputValueRespeckChild[0].indices.maxBy { outputValueRespeckChild[0][it] } ?: -1
                            if (maxIdx_child == 0) {respeckTextView.text = respeckTextView.text as String + " Desk Work"
                            }
                            else if (maxIdx_child == 1) {respeckTextView.text = respeckTextView.text as String + " Bent Backwards"
                            }
                            else if (maxIdx_child == 2) {
                                respeckTextView.text = respeckTextView.text as String + " Bent Forwards"
                            }
                            else if (maxIdx_child == 3) {
                                respeckTextView.text = respeckTextView.text as String + " Just Sitting"
                            }
                        }
                        else if (maxIdx == 2) {
                            respeckTextView.text = "Recogized activity: Regular Movement: Running Children models"
                            Interpreter(loadModelFile("RESPEC_REGULAR_MOVEMENT_MODEL_3CLASSES.tflite")).use { interpreter ->
                                interpreter.run(inputValueRespeck, outputValueRespeckChild)}
                            val maxIdx_child = outputValueRespeckChild[0].indices.maxBy { outputValueRespeckChild[0][it] } ?: -1
                            if (maxIdx_child == 0) {respeckTextView.text = respeckTextView.text as String + " Running"
                            }
                            else if (maxIdx_child == 1) {respeckTextView.text = respeckTextView.text as String + " Standing"
                            }
                            else if (maxIdx_child == 2) {
                                respeckTextView.text = respeckTextView.text as String + " Walking"
                            }
                        }
                        else if (maxIdx == 3) {
                            respeckTextView.text = "Recogized activity: Stairs: Running Children models"
                            Interpreter(loadModelFile("RESPEC_STAIRS_MODEL_2CLASSES.tflite")).use { interpreter ->
                                interpreter.run(inputValueRespeck, outputValueRespeckChild)}
                            val maxIdx_child = outputValueRespeckChild[0].indices.maxBy { outputValueRespeckChild[0][it] } ?: -1
                            if (maxIdx_child == 0) {respeckTextView.text = respeckTextView.text as String + " Climbing"
                            }
                            else if (maxIdx_child == 1) {respeckTextView.text = respeckTextView.text as String + " Descending"
                            }
                        }
                        else if (maxIdx == 4) {
                            respeckTextView.text = "Recogized activity: Movement"
                        }
                        inputValueRespeck = Array(1) {
                            Array(50) {
                                FloatArray(6)
                            }
                        }
                        bufferCount = 0
                    }

                    time += 1
                    updateGraph("respeck", x, y, z)

                    inputValueRespeck[0][bufferCount][0] = x
                    inputValueRespeck[0][bufferCount][1] = y
                    inputValueRespeck[0][bufferCount][2] = z
                    inputValueRespeck[0][bufferCount][3] = liveData.gyro.x
                    inputValueRespeck[0][bufferCount][4] = liveData.gyro.y
                    inputValueRespeck[0][bufferCount][5] = liveData.gyro.z

                    bufferCount += 1
                }
            }
        }

        // register receiver on another thread
        val handlerThreadRespeck = HandlerThread("bgThreadRespeckLive")
        handlerThreadRespeck.start()
        looperRespeck = handlerThreadRespeck.looper
        val handlerRespeck = Handler(looperRespeck)
        this.registerReceiver(respeckLiveUpdateReceiver, filterTestRespeck, null, handlerRespeck)

        // set up the broadcast receiver
        thingyLiveUpdateReceiver = object : BroadcastReceiver() {
            @SuppressLint("SetTextI18n")
            override fun onReceive(context: Context, intent: Intent) {

                Log.i("thread", "I am running on thread = " + Thread.currentThread().name)

                val action = intent.action

                if (action == Constants.ACTION_THINGY_BROADCAST) {

                    val liveData =
                        intent.getSerializableExtra(Constants.THINGY_LIVE_DATA) as ThingyLiveData
                    Log.d("Live", "onReceive: liveData = " + liveData)

                    // get all relevant intent contents
                    val x = liveData.accelX
                    val y = liveData.accelY
                    val z = liveData.accelZ

                    if (bufferCount >= 25) {
                        Interpreter(loadModelFile("THINGY_ROOT_OF_TREE_MODEL_5CLASSES.tflite")).use { interpreter ->
                            interpreter.run(inputValueThingy, outputValueThingy)}
                        //search "Predicted output" in Run terminal to see the outputs
                        Log.i("Predicted outputValue", outputValueThingy.contentDeepToString())
                        outputValueRespeck[0][4] -= movementBias  // for the bias against movement
                        val maxIdx = outputValueThingy[0].indices.maxBy { outputValueThingy[0][it] } ?: -1
                        Log.i("maxIdx activity identifier", outputValueThingy.contentDeepToString())
                        if (maxIdx == 0) {
                            thingyTextView.text = "Recogized activity: Lying Down : Running Children models : "
                            Interpreter(loadModelFile("THINGY_LYING_DOWN_MODEL_4CLASSES.tflite")).use { interpreter ->
                                interpreter.run(inputValueThingy, outputValueThingyChild)}
                            val maxIdx_child = outputValueThingyChild[0].indices.maxBy { outputValueThingyChild[0][it] } ?: -1
                            if (maxIdx_child == 0) {thingyTextView.text = thingyTextView.text as String + " Left"
                            }
                            else if (maxIdx_child == 1) {thingyTextView.text = thingyTextView.text as String + " On Back"
                            }
                            else if (maxIdx_child == 2) {
                                thingyTextView.text = thingyTextView.text as String + " On Stomach"
                            }
                            else if (maxIdx_child == 3) {
                                thingyTextView.text = thingyTextView.text as String + " Right"
                            }
                        }
                        else if (maxIdx == 1) {
                            thingyTextView.text = "Recogized activity: Sitting : Running Children models"
                            Interpreter(loadModelFile("THINGY_SITTING_MODEL_4CLASSES.tflite")).use { interpreter ->
                                interpreter.run(inputValueThingy, outputValueThingyChild)}
                            val maxIdx_child = outputValueThingyChild[0].indices.maxBy { outputValueThingyChild[0][it] } ?: -1
                            if (maxIdx_child == 0) {thingyTextView.text = thingyTextView.text as String + " Desk Work"
                            }
                            else if (maxIdx_child == 1) {thingyTextView.text = thingyTextView.text as String + " Bent Backwards"
                            }
                            else if (maxIdx_child == 2) {
                                thingyTextView.text = thingyTextView.text as String + " Bent Forwards"
                            }
                            else if (maxIdx_child == 3) {
                                thingyTextView.text = thingyTextView.text as String + " Just Sitting"
                            }
                        }
                        else if (maxIdx == 2) {
                            thingyTextView.text = "Recogized activity: Regular Movement: Running Children models"
                            Interpreter(loadModelFile("THINGY_REGULAR_MOVEMENT_MODEL_3CLASSES.tflite")).use { interpreter ->
                                interpreter.run(inputValueThingy, outputValueThingyChild)}
                            val maxIdx_child = outputValueThingyChild[0].indices.maxBy { outputValueThingyChild[0][it] } ?: -1
                            if (maxIdx_child == 0) {thingyTextView.text = thingyTextView.text as String + " Running"
                            }
                            else if (maxIdx_child == 1) {thingyTextView.text = thingyTextView.text as String + " Standing"
                            }
                            else if (maxIdx_child == 2) {
                                thingyTextView.text = thingyTextView.text as String + " Walking"
                            }
                        }
                        else if (maxIdx == 3) {
                            thingyTextView.text = "Recogized activity: Stairs: Running Children models"
                            Interpreter(loadModelFile("THINGY_STAIRS_MODEL_2CLASSES.tflite")).use { interpreter ->
                                interpreter.run(inputValueThingy, outputValueThingyChild)}
                            val maxIdx_child = outputValueThingyChild[0].indices.maxBy { outputValueThingyChild[0][it] } ?: -1
                            if (maxIdx_child == 0) {thingyTextView.text = thingyTextView.text as String + " Climbing"
                            }
                            else if (maxIdx_child == 1) {thingyTextView.text = thingyTextView.text as String + " Descending"
                            }
                        }
                        else if (maxIdx == 4) {
                            thingyTextView.text = "Recogized activity: Movement"
                        }
                        inputValueThingy = Array(1) {
                            Array(50) {
                                FloatArray(6)
                            }
                        }
                        bufferCount = 0
                    }

                    time += 1
                    updateGraph("thingy", x, y, z)

                    inputValueThingy[0][bufferCount][0] = x
                    inputValueThingy[0][bufferCount][1] = y
                    inputValueThingy[0][bufferCount][2] = z
                    inputValueThingy[0][bufferCount][3] = liveData.gyro.x
                    inputValueThingy[0][bufferCount][4] = liveData.gyro.y
                    inputValueThingy[0][bufferCount][5] = liveData.gyro.z

                    bufferCount += 1
                }
            }
        }

        // register receiver on another thread
        val handlerThreadThingy = HandlerThread("bgThreadThingyLive")
        handlerThreadThingy.start()
        looperThingy = handlerThreadThingy.looper
        val handlerThingy = Handler(looperThingy)
        this.registerReceiver(thingyLiveUpdateReceiver, filterTestThingy, null, handlerThingy)

        // set up the broadcast receiver
        combinedLiveUpdateReceiver = object : BroadcastReceiver() {
            @SuppressLint("SetTextI18n")
            override fun onReceive(context: Context, intent: Intent) {

                Log.i("thread", "I am running on thread = " + Thread.currentThread().name)

                val respectConnected =  Constants.ACTION_RESPECK_CONNECTED
                val thingyConnected = Constants.ACTION_THINGY_CONNECTED

                //Not sure about this one chief
                if (respectConnected.isNotEmpty() && thingyConnected.isNotEmpty()) {

                    val ThingyData =
                        intent.getSerializableExtra(Constants.THINGY_LIVE_DATA) as ThingyLiveData
                    Log.d("Live", "onReceive: liveData = " + ThingyData)
                    val RespeckData =
                        intent.getSerializableExtra(Constants.RESPECK_LIVE_DATA) as ThingyLiveData
                    Log.d("Live", "onReceive: liveData = " + RespeckData)


                    if (bufferCount >= 25) {
                        Log.i("Predicted outputValue", outputValueThingy.contentDeepToString())
                        val maxIdxThingy = outputValueThingy[0].indices.maxBy { outputValueThingy[0][it] } ?: -1
                        val maxIdxRespeck = outputValueRespeck[0].indices.maxBy { outputValueThingy[0][it] } ?: -1
                        Log.i("maxIdx activity identifier", outputValueThingy.contentDeepToString())
                        if(outputValueThingy[0][maxIdxThingy] >= confidenceFreshhold && outputValueRespeck[0][maxIdxRespeck] >= confidenceFreshhold){
                            if (maxIdxThingy == 0 && maxIdxRespeck == 0 ) {
                                combinedTextView.text = "Recognized activity: Lying Down : Evaluating Children"
                                val maxIdxThingyChild = outputValueThingy[0].indices.maxBy { outputValueThingy[0][it] } ?: -1
                                val maxIdxRespeckChild = outputValueRespeck[0].indices.maxBy { outputValueThingy[0][it] } ?: -1
                                if (outputValueThingyChild[0][maxIdxThingyChild] >= confidenceFreshhold && outputValueRespeckChild[0][maxIdxRespeckChild] >= confidenceFreshhold) {
                                    if (maxIdxThingyChild == 0 && maxIdxRespeckChild == 0) {
                                        combinedTextView.text =
                                            combinedTextView.text as String + " Left"
                                    } else if (maxIdxThingyChild == 1 && maxIdxRespeckChild == 1) {
                                        combinedTextView.text =
                                            combinedTextView.text as String + " On Back"
                                    } else if (maxIdxThingyChild == 2 && maxIdxRespeckChild == 2) {
                                        combinedTextView.text =
                                            combinedTextView.text as String + " On Stomach"
                                    } else if (maxIdxThingyChild == 3 && maxIdxRespeckChild == 3) {
                                        combinedTextView.text =
                                            combinedTextView.text as String + " Right"
                                    }
                                }
                            }
                            else if (maxIdxThingy == 1 && maxIdxRespeck == 1) {
                                combinedTextView.text = "Recogized activity: Sitting : Evaluating Children"
                                val maxIdxThingyChild = outputValueThingy[0].indices.maxBy { outputValueThingy[0][it] } ?: -1
                                val maxIdxRespeckChild = outputValueRespeck[0].indices.maxBy { outputValueThingy[0][it] } ?: -1
                                if (outputValueThingyChild[0][maxIdxThingyChild] >= confidenceFreshhold && outputValueRespeckChild[0][maxIdxRespeckChild] >= confidenceFreshhold) {
                                    if (maxIdxThingyChild == 0 && maxIdxRespeckChild == 0) {
                                        combinedTextView.text =
                                            combinedTextView.text as String + " Desk Work"
                                    } else if (maxIdxThingyChild == 1 && maxIdxRespeckChild == 1) {
                                        combinedTextView.text =
                                            combinedTextView.text as String + " Bent Backwards"
                                    } else if (maxIdxThingyChild == 2 && maxIdxRespeckChild == 2) {
                                        combinedTextView.text =
                                            combinedTextView.text as String + " Bent Forwards"
                                    } else if (maxIdxThingyChild == 3 && maxIdxRespeckChild == 3) {
                                        combinedTextView.text =
                                            combinedTextView.text as String + " Just Sitting"
                                    }
                                }
                            }
                            else if (maxIdxThingy == 2 && maxIdxRespeck == 2) {
                                combinedTextView.text = "Recogized activity: Regular Movement: Evaluating Children"
                                val maxIdxThingyChild = outputValueThingy[0].indices.maxBy { outputValueThingy[0][it] } ?: -1
                                val maxIdxRespeckChild = outputValueRespeck[0].indices.maxBy { outputValueThingy[0][it] } ?: -1
                                if (outputValueThingyChild[0][maxIdxThingyChild] >= confidenceFreshhold && outputValueRespeckChild[0][maxIdxRespeckChild] >= confidenceFreshhold) {
                                    if (maxIdxThingyChild == 0 && maxIdxRespeckChild == 0) {
                                        combinedTextView.text =
                                            combinedTextView.text as String + " Running"
                                    } else if (maxIdxThingyChild == 1 && maxIdxRespeckChild == 1) {
                                        combinedTextView.text =
                                            combinedTextView.text as String + " Standing"
                                    } else if (maxIdxThingyChild == 2 && maxIdxRespeckChild == 2) {
                                        combinedTextView.text =
                                            combinedTextView.text as String + " Walking"
                                    }
                                }
                            }
                            else if (maxIdxThingy == 3 && maxIdxRespeck == 3) {
                                combinedTextView.text = "Recogized activity: Stairs: Evaluating Children"
                                val maxIdxThingyChild = outputValueThingy[0].indices.maxBy { outputValueThingy[0][it] } ?: -1
                                val maxIdxRespeckChild = outputValueRespeck[0].indices.maxBy { outputValueThingy[0][it] } ?: -1
                                if (outputValueThingyChild[0][maxIdxThingyChild] >= confidenceFreshhold && outputValueRespeckChild[0][maxIdxRespeckChild] >= confidenceFreshhold) {
                                    if (maxIdxThingyChild == 0 && maxIdxRespeckChild == 0) {
                                        combinedTextView.text =
                                            combinedTextView.text as String + " Climbing"
                                    } else if (maxIdxThingyChild == 1 && maxIdxRespeckChild == 1) {
                                        combinedTextView.text =
                                            combinedTextView.text as String + " Descending "
                                    }
                                }

                            }
                            else if (maxIdxThingy == 4 && maxIdxRespeck == 1) {
                                thingyTextView.text = "Recogized activity: Movement"
                            }
                        }else{
                            combinedTextView.text = "Waiting for prediction to be confident above " + confidenceFreshhold as  String
                        }


                        bufferCount = 0
                    }
                    time += 1
                    bufferCount += 1
                }else{
                    combinedTextView.text = "Connect both sensors for a combined prediction"
                }
            }
        }
        // register receiver on another thread
        val handlerThreadCombined = HandlerThread("bgThreadCombinedLive")
        handlerThreadCombined.start()
        looperCombined = handlerThreadCombined.looper
        val handlerCombined = Handler(looperThingy)
        this.registerReceiver(combinedLiveUpdateReceiver, filterTestThingy, null, handlerCombined)
        this.registerReceiver(combinedLiveUpdateReceiver, filterTestRespeck, null, handlerCombined)

    }

    private fun loadModelFile(fileName: String): MappedByteBuffer {
        val fileDescriptor = this.assets.openFd(fileName)
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel: FileChannel = inputStream.channel
        val startOffset = fileDescriptor.startOffset
        val declaredLength = fileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }

    fun setupCharts() {
        respeckChart = findViewById(R.id.respeck_chart)
        thingyChart = findViewById(R.id.thingy_chart)

        // Respeck

        time = 0f
        val entries_res_accel_x = ArrayList<Entry>()
        val entries_res_accel_y = ArrayList<Entry>()
        val entries_res_accel_z = ArrayList<Entry>()

        dataSet_res_accel_x = LineDataSet(entries_res_accel_x, "Accel X")
        dataSet_res_accel_y = LineDataSet(entries_res_accel_y, "Accel Y")
        dataSet_res_accel_z = LineDataSet(entries_res_accel_z, "Accel Z")

        dataSet_res_accel_x.setDrawCircles(false)
        dataSet_res_accel_y.setDrawCircles(false)
        dataSet_res_accel_z.setDrawCircles(false)

        dataSet_res_accel_x.setColor(
            ContextCompat.getColor(
                this,
                R.color.red
            )
        )
        dataSet_res_accel_y.setColor(
            ContextCompat.getColor(
                this,
                R.color.green
            )
        )
        dataSet_res_accel_z.setColor(
            ContextCompat.getColor(
                this,
                R.color.blue
            )
        )

        val dataSetsRes = ArrayList<ILineDataSet>()
        dataSetsRes.add(dataSet_res_accel_x)
        dataSetsRes.add(dataSet_res_accel_y)
        dataSetsRes.add(dataSet_res_accel_z)

        allRespeckData = LineData(dataSetsRes)
        respeckChart.data = allRespeckData
        respeckChart.invalidate()

        // Thingy

        time = 0f
        val entries_thingy_accel_x = ArrayList<Entry>()
        val entries_thingy_accel_y = ArrayList<Entry>()
        val entries_thingy_accel_z = ArrayList<Entry>()

        dataSet_thingy_accel_x = LineDataSet(entries_thingy_accel_x, "Accel X")
        dataSet_thingy_accel_y = LineDataSet(entries_thingy_accel_y, "Accel Y")
        dataSet_thingy_accel_z = LineDataSet(entries_thingy_accel_z, "Accel Z")

        dataSet_thingy_accel_x.setDrawCircles(false)
        dataSet_thingy_accel_y.setDrawCircles(false)
        dataSet_thingy_accel_z.setDrawCircles(false)

        dataSet_thingy_accel_x.setColor(
            ContextCompat.getColor(
                this,
                R.color.red
            )
        )
        dataSet_thingy_accel_y.setColor(
            ContextCompat.getColor(
                this,
                R.color.green
            )
        )
        dataSet_thingy_accel_z.setColor(
            ContextCompat.getColor(
                this,
                R.color.blue
            )
        )

        val dataSetsThingy = ArrayList<ILineDataSet>()
        dataSetsThingy.add(dataSet_thingy_accel_x)
        dataSetsThingy.add(dataSet_thingy_accel_y)
        dataSetsThingy.add(dataSet_thingy_accel_z)

        allThingyData = LineData(dataSetsThingy)
        thingyChart.data = allThingyData
        thingyChart.invalidate()
    }

    fun updateGraph(graph: String, x: Float, y: Float, z: Float) {
        // take the first element from the queue
        // and update the graph with it
        if (graph == "respeck") {
            dataSet_res_accel_x.addEntry(Entry(time, x))
            dataSet_res_accel_y.addEntry(Entry(time, y))
            dataSet_res_accel_z.addEntry(Entry(time, z))

            runOnUiThread {
                allRespeckData.notifyDataChanged()
                respeckChart.notifyDataSetChanged()
                respeckChart.invalidate()
                respeckChart.setVisibleXRangeMaximum(150f)
                respeckChart.moveViewToX(respeckChart.lowestVisibleX + 40)
            }
        } else if (graph == "thingy") {
            dataSet_thingy_accel_x.addEntry(Entry(time, x))
            dataSet_thingy_accel_y.addEntry(Entry(time, y))
            dataSet_thingy_accel_z.addEntry(Entry(time, z))

            runOnUiThread {
                allThingyData.notifyDataChanged()
                thingyChart.notifyDataSetChanged()
                thingyChart.invalidate()
                thingyChart.setVisibleXRangeMaximum(150f)
                thingyChart.moveViewToX(thingyChart.lowestVisibleX + 40)
            }
        }


    }


    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(respeckLiveUpdateReceiver)
        unregisterReceiver(thingyLiveUpdateReceiver)
        looperRespeck.quit()
        looperThingy.quit()
    }
}
