package com.yatra.yatraapp.ui.nav_fragments

import android.app.DatePickerDialog
import android.app.ProgressDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.view.*
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.TimePicker
import androidx.core.view.GestureDetectorCompat
import androidx.fragment.app.Fragment
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.yatra.yatraapp.databinding.FragmentStudentHomeBinding
import com.yatra.yatraapp.model.YatraRequest
import com.yatra.yatraapp.ui.SubmitSuccessActivity
import com.yatra.yatraapp.utils.getRandomUID
import com.yatra.yatraapp.utils.showShortToast
import java.util.*

class StudentHomeFragment : Fragment() {
    private var binding: FragmentStudentHomeBinding? = null

    private var dialog: ProgressDialog? = null

    private lateinit var departureLocation: String
    private var departureLatitude: Double = 0.0
    private var departureLongitude: Double = 0.0

    private lateinit var arrivalLocation: String
    private var arrivalLatitude: Double = 0.0
    private var arrivalLongitude: Double = 0.0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        childFragmentManager.setFragmentResultListener(
            "departure", this
        ) { _, result ->
            departureLocation = result.getString("departure_location")!!
            departureLatitude = result.getDouble("departure_lat")
            departureLongitude = result.getDouble("departure_long")
            binding?.etDepartureLocation?.setText(departureLocation)
        }
        childFragmentManager.setFragmentResultListener(
            "arrival", this
        ) { _, result ->
            arrivalLocation = result.getString("arrival_location")!!
            arrivalLatitude = result.getDouble("arrival_lat")
            arrivalLongitude = result.getDouble("arrival_long")
            binding?.etArrivalLocation?.setText(arrivalLocation)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentStudentHomeBinding.inflate(inflater, container, false)
        return binding!!.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val people = mutableListOf(
            "Select Number of People",
            "1", "2", "3", "4",
            "5", "6", "7", "8",
            "9", "10"
        )
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, people)
        binding?.spnrPeopleCount?.adapter = adapter

        binding?.etDepatureDate?.setOnConsistentClickListener {
            with(Calendar.getInstance()) {
                DatePickerDialog(
                    requireActivity(),
                    { _, year: Int, month: Int, day: Int ->
                        val date = "$day/${month + 1}/$year"
                        binding?.etDepatureDate?.setText(date)
                    },
                    this[Calendar.YEAR], this[Calendar.MONTH], this[Calendar.DAY_OF_MONTH]
                ).also {
                    it.datePicker.minDate = System.currentTimeMillis()
                }.show()
            }
        }

        binding?.etArrivalDate?.setOnConsistentClickListener {
            with(Calendar.getInstance()) {
                DatePickerDialog(
                    requireActivity(),
                    { _, year: Int, month: Int, day: Int ->
                        val date = "$day/${month + 1}/$year"
                        binding?.etArrivalDate?.setText(date)
                    },
                    this[Calendar.YEAR], this[Calendar.MONTH], this[Calendar.DAY_OF_MONTH]
                ).also {
                    it.datePicker.minDate = System.currentTimeMillis()
                }.show()
            }
        }

        binding?.etDepatureTime?.setOnConsistentClickListener {
            with(Calendar.getInstance()) {
                TimePickerDialog(
                    context,
                    { _: TimePicker?, hourOfDay: Int, minute: Int ->
                        val time = "$hourOfDay:$minute"
                        binding?.etDepatureTime?.setText(time)
                    }, this[Calendar.HOUR_OF_DAY], this[Calendar.MINUTE], false
                ).show()
            }
        }

        binding?.etArrivalTime?.setOnConsistentClickListener {
            with(Calendar.getInstance()) {
                TimePickerDialog(
                    context,
                    { _: TimePicker?, hourOfDay: Int, minute: Int ->
                        val time = "$hourOfDay:$minute"
                        binding?.etArrivalTime?.setText(time)
                    }, this[Calendar.HOUR_OF_DAY], this[Calendar.MINUTE], false
                ).show()
            }
        }

        binding?.etDepartureLocation?.setOnConsistentClickListener {
            SelectLocationFragment.newInstance(true).show(childFragmentManager, "location")
        }

        binding?.etArrivalLocation?.setOnConsistentClickListener {
            SelectLocationFragment.newInstance(false).show(childFragmentManager, "location")
        }

        binding?.yesBtnId?.setOnClickListener {
            requireActivity().finish()
        }

        binding?.studentSubmitBtnId?.setOnClickListener { _ ->
            val departureDate = binding?.etDepatureDate?.text.toString()
            val arrivalDate = binding?.etArrivalDate?.text.toString()
            val departureTime = binding?.etDepatureTime?.text.toString()
            val arrivalTime = binding?.etArrivalTime?.text.toString()
            val weight = binding?.etWeight?.text.toString().trim()
            val msg = binding?.etMsg?.text.toString().trim()

            var peopleCount: Int = -1
            try {
                peopleCount = binding?.spnrPeopleCount?.selectedItem.toString().toInt()
            } catch (e: Exception) {
                "Please select number of people".showShortToast(requireContext())
            }

            if (TextUtils.isEmpty(departureDate)) {
                binding?.etDepatureDate?.error = "Select Departure Date"
                return@setOnClickListener
            }
            if (TextUtils.isEmpty(departureTime)) {
                binding?.etDepatureTime?.error = "Select Departure Time"
                return@setOnClickListener
            }
            if (TextUtils.isEmpty(departureLocation)) {
                binding?.etDepartureLocation?.error = "It cannot be left empty"
                return@setOnClickListener
            }
            if (TextUtils.isEmpty(arrivalLocation)) {
                binding?.etArrivalLocation?.error = "It cannot be left empty"
                return@setOnClickListener
            }
            if (TextUtils.isEmpty(arrivalDate)) {
                binding?.etArrivalDate?.error = "Select Arrival Date"
                return@setOnClickListener
            }
            if (TextUtils.isEmpty(arrivalTime)) {
                binding?.etDepatureTime?.error = "Select Arrival Time"
                return@setOnClickListener
            }
            if (TextUtils.isEmpty(msg)) {
                binding?.etMsg?.error = "Write Special Message"
                return@setOnClickListener
            }
            if (TextUtils.isEmpty(weight)) {
                binding?.etWeight?.error = "Input Luggage Weight"
                return@setOnClickListener
            }

            dialog = ProgressDialog(context)
            dialog!!.setMessage("Sending Request")
            dialog!!.show()

            makeYatraRequest(
                YatraRequest(
                    requestId = getRandomUID(),
                    initiatorId = Firebase.auth.currentUser?.uid,
                    arrivalLocation = arrivalLocation,
                    arrivalLatitude = arrivalLatitude,
                    arrivalLongitude = arrivalLongitude,
                    arrivalDate = arrivalDate,
                    arrivalTime = arrivalTime,
                    departureLocation = departureLocation,
                    departureLatitude = departureLatitude,
                    departureLongitude = departureLongitude,
                    departureDate = departureDate,
                    departureTime = departureTime,
                    peopleCount = peopleCount,
                    weight = weight.toInt(),
                    msg = msg
                )
            )
        }
    }

    override fun onDestroyView() {
        binding = null
        super.onDestroyView()
    }

    private fun makeYatraRequest(yatraRequest: YatraRequest) {
        Firebase.firestore.collection("yatra_requests")
            .document(yatraRequest.requestId!!)
            .set(yatraRequest)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    "Request Sent".showShortToast(requireContext())
                    Intent(context, SubmitSuccessActivity::class.java).also { intent ->
                        startActivity(intent)
                    }
                } else {
                    "Error occurred : ${task.exception?.message}".showShortToast(requireContext())
                }

                dialog?.dismiss()
                // clearing data in the views
                binding?.etDepartureLocation?.setText("")
                binding?.etArrivalLocation?.setText("")
                binding?.etDepatureDate?.setText("")
                binding?.etArrivalDate?.setText("")
                binding?.etDepatureTime?.setText("")
                binding?.etArrivalTime?.setText("")
                binding?.etWeight?.setText("")
                binding?.etMsg?.setText("")
            }
    }
}

fun EditText.setOnConsistentClickListener(doOnClick: (View) -> Unit) {
    val gestureDetector =
        GestureDetectorCompat(context, object : GestureDetector.SimpleOnGestureListener() {
            override fun onSingleTapUp(event: MotionEvent?): Boolean {
                doOnClick(this@setOnConsistentClickListener)
                return false
            }
        })

    this.setOnTouchListener { _, motionEvent -> gestureDetector.onTouchEvent(motionEvent) }
}