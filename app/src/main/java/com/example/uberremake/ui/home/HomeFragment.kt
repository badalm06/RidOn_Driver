package com.example.uberremake.ui.home

import android.Manifest
import android.R.attr.duration
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.app.AlertDialog
import com.google.maps.android.PolyUtil
import android.content.Context
import android.os.Handler
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.media.audiofx.BassBoost.Settings
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Looper
import android.text.style.RelativeSizeSpan
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.LinearInterpolator
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresPermission
import androidx.cardview.widget.CardView
import androidx.compose.ui.node.RootForTest
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.example.uberremake.R
import com.example.uberremake.Common
import com.example.uberremake.DriverHomeActivity
import com.example.uberremake.Model.DriverRequestReceived
import com.example.uberremake.Model.NotificationHelper.sendTripCompletedNotification
import com.example.uberremake.Model.TripNotificationManager
import com.example.uberremake.Remote.IGoogleAPI
import com.example.uberremake.Remote.RetrofitClient
import com.example.uberremake.databinding.FragmentHomeBinding
import com.firebase.geofire.GeoFire
import com.firebase.geofire.GeoLocation
import com.firebase.geofire.GeoQuery
import com.firebase.geofire.util.GeoUtils.distance
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.JointType
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.Polyline
import com.google.android.gms.maps.model.PolylineOptions
import com.google.android.gms.maps.model.SquareCap
import com.google.android.material.chip.Chip
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.MutableData
import com.google.firebase.database.Transaction
import com.google.firebase.database.ValueEventListener
import com.karumi.dexter.Dexter
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionDeniedResponse
import com.karumi.dexter.listener.PermissionGrantedResponse
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.single.PermissionListener
import io.reactivex.rxjava3.disposables.CompositeDisposable   // RxJava 3
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Scheduler
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.schedulers.Schedulers
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.json.JSONObject
import java.io.IOException
import java.sql.Time
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.compareTo
import kotlin.math.log

class HomeFragment : Fragment(), OnMapReadyCallback {

    // Views
    private lateinit var chip_decline: Chip
    private lateinit var layout_accept: CardView
    private lateinit var circularProgressBar: ProgressBar
    private var timerDisposable: Disposable? = null
    private lateinit var txt_fare: TextView
    private lateinit var txt_fare1: TextView
    private lateinit var txt_destination: TextView
    private lateinit var txt_estimate_time: TextView
    private lateinit var txt_estimate_distance: TextView
    private lateinit var txt_trip_time: TextView
    private lateinit var txt_trip_distance: TextView
    private var pickupMarker: Marker? = null
    private var driverMarker: Marker? = null
    private var routeAnimator: ValueAnimator? = null
    private var routePolyline: Polyline? = null

    private lateinit var root_layout: FrameLayout
    private var isRideStarted = false
    private var isRideInProgress = false

    // Rider Info Layout
    private lateinit var riderInfoLayout: CardView
    private lateinit var txtEta: TextView
    private lateinit var txtDistance: TextView
    private lateinit var imgRider: ImageView
    private lateinit var txtRiderName: TextView
    private lateinit var txtRiderRating: TextView
    private lateinit var imgCallRider: ImageView
    private lateinit var btnStartUber: Button
    private lateinit var btnStartRide: Button
    private lateinit var btnCompleteRide: Button
    private lateinit var activeTripId: String

    // Accept or Decline
    private var isRequestHandled = false

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private lateinit var mMap: GoogleMap

    private lateinit var mapFragment: SupportMapFragment

    //Routes
    private val compositeDisposable = CompositeDisposable()
    private lateinit var iGoogleAPI: IGoogleAPI
    private var blackPolyline: Polyline?= null
    private var greyPolyline: Polyline?= null
    private var polylineOptions: PolylineOptions?= null
    private var blackPolylineOptions: PolylineOptions?= null
    private var polylineList: ArrayList<LatLng?>? = null
    internal var originMarker: Marker?= null
    internal var destinationMarker: Marker?= null

    //Location
    private lateinit var locationRequest: LocationRequest
    private lateinit var locationCallback: LocationCallback
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient

    // Online System
    private lateinit var onlineRef: DatabaseReference
    private var currentUserRef: DatabaseReference? = null
    private lateinit var driversLocationRef: DatabaseReference
    private lateinit var geoFire: GeoFire
    private lateinit var riderOriginLatLng: LatLng
    private var destinationLatLng: LatLng? = null



    private val onlineValueEventListener = object : ValueEventListener {
        override fun onDataChange(p0: DataSnapshot) {
            if(p0.exists() && currentUserRef != null)
                currentUserRef!!.onDisconnect().removeValue()
        }

        override fun onCancelled(p0: DatabaseError) {
            Snackbar.make(mapFragment.requireView(),p0.message, Snackbar.LENGTH_LONG).show()
        }

    }

    private fun isLocationEnabled(): Boolean {
        val locationManager = requireContext().getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }

    private fun showLocationEnableDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Location Required")
            .setMessage("Please enable location services to use this app.")
            .setCancelable(false)
            .setPositiveButton("Enable") { _, _ ->
                startActivity(Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS))
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    override fun onStart() {
        super.onStart()
        Log.d("EVENTBUS", "Fragment onStart called")
        if(!EventBus.getDefault().isRegistered(this))
            EventBus.getDefault().register(this)
    }

    override fun onDestroyView() {
        _binding = null
        geoFire.removeLocation(FirebaseAuth.getInstance().currentUser!!.uid)
        onlineRef.removeEventListener(onlineValueEventListener)

        compositeDisposable.clear()
        if(EventBus.getDefault().hasSubscriberForEvent(DriverHomeActivity::class.java))
            EventBus.getDefault().removeStickyEvent(DriverHomeActivity::class.java)
        EventBus.getDefault().unregister(this)

        super.onDestroyView()

    }


    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val homeViewModel = ViewModelProvider(this).get(HomeViewModel::class.java)
        FirebaseAppCheck.getInstance().installAppCheckProviderFactory(
            PlayIntegrityAppCheckProviderFactory.getInstance()
        )
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        val root: View = binding.root

        if (!isLocationEnabled()) {
            showLocationEnableDialog()
        } else {
            initViews(root)
            init() // Your initialization logic (location updates, map, etc.)

        }
        mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        return root
    }

    private fun initViews(root: View?) {
        chip_decline = root!!.findViewById(R.id.chip_decline) as Chip
        layout_accept = root.findViewById(R.id.layout_accept) as CardView
        circularProgressBar = root.findViewById(R.id.circularProgressBar) as ProgressBar
        txt_estimate_time = root.findViewById(R.id.txt_estimate_time) as TextView
        txt_estimate_distance = root.findViewById(R.id.txt_estimate_distance) as TextView
        txt_trip_time = root.findViewById(R.id.txt_trip_time) as TextView
        txt_fare = root.findViewById(R.id.txt_fare) as TextView
        txt_fare1 = root.findViewById(R.id.txt_fare1) as TextView
        txt_destination = root.findViewById(R.id.txt_destination) as TextView
        txt_trip_distance = root.findViewById(R.id.txt_trip_distance) as TextView
        root_layout = root.findViewById(R.id.root_layout) as FrameLayout

        riderInfoLayout = root.findViewById(R.id.rider_info_layout) as CardView
        txtEta = root.findViewById(R.id.txt_eta) as TextView
        txtDistance = root.findViewById(R.id.txt_distance) as TextView
        imgRider = root.findViewById(R.id.img_rider) as ImageView
        txtRiderName = root.findViewById(R.id.txt_rider_name) as TextView
     //   txtRiderRating = root.findViewById(R.id.txt_rider_rating) as TextView
        imgCallRider = root.findViewById(R.id.img_call_rider) as ImageView
        btnStartUber = root.findViewById(R.id.btn_start_uber) as Button
        btnStartRide = root.findViewById(R.id.btn_start_ride) as Button
        btnCompleteRide = root.findViewById(R.id.btn_complete_ride) as Button



    }

    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    private fun init() {

        iGoogleAPI = RetrofitClient.instance!!.create(IGoogleAPI::class.java)

        onlineRef = FirebaseDatabase.getInstance().getReference().child(".info/connected")



        locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            10000L // interval
        )
            .setMinUpdateIntervalMillis(15000L) // fastest interval
            .setMinUpdateDistanceMeters(50f) // smallest displacement
            .build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                Log.d("DriverLocation", "onLocationResult fired")
                super.onLocationResult(locationResult)
                val location = locationResult.lastLocation ?: return
                val newPos = LatLng(location.latitude, location.longitude)
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(newPos, 18f))
                driverMarker?.position = newPos



                val geoCoder = Geocoder(requireContext(), Locale.getDefault())
                lifecycleScope.launch {
                    try {
                        val addressList: List<Address>? = withContext(Dispatchers.IO) {
                            geoCoder.getFromLocation(
                                locationResult.lastLocation!!.latitude,
                                locationResult.lastLocation!!.longitude,
                                1
                            )
                        }

                        val cityName = addressList?.get(0)?.locality


                        driversLocationRef = FirebaseDatabase.getInstance().getReference(Common.DRIVERS_LOCATION_REFERENCE)
                            .child(cityName.toString())


                        currentUserRef = driversLocationRef.child(
                            FirebaseAuth.getInstance().currentUser!!.uid
                        )
                        currentUserRef?.onDisconnect()?.removeValue()

                        geoFire = GeoFire(driversLocationRef)

                        geoFire.setLocation(
                            FirebaseAuth.getInstance().currentUser!!.uid,
                            GeoLocation(
                                locationResult.lastLocation!!.latitude,
                                locationResult.lastLocation!!.longitude
                            )
                        ) { key: String?, error: DatabaseError? ->
                            if (error != null) {
                                Snackbar.make(mapFragment.requireView(), error.message, Snackbar.LENGTH_LONG).show()
                            }
//                            else {
//                                Snackbar.make(mapFragment.requireView(), "You're Online!", Snackbar.LENGTH_SHORT).show()
//                            }
                        }

                    } catch (e: IOException) {
                        Snackbar.make(mapFragment.requireView(), "Error: ${e.message}", Snackbar.LENGTH_LONG).show()
                    }
                }

            }
        }
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(requireContext())
        fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback,
            Looper.myLooper())
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        // Request Permission
        Dexter.withContext(requireContext())
            .withPermission(Manifest.permission.ACCESS_FINE_LOCATION)
            .withListener(object : PermissionListener{
                @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
                override fun onPermissionGranted(p0: PermissionGrantedResponse?) {
                    // Enable button first
                    mMap.isMyLocationEnabled = true
                    mMap.uiSettings.isMyLocationButtonEnabled = true
                    mMap.setOnMyLocationClickListener {
                        fusedLocationProviderClient.lastLocation
                            .addOnFailureListener { e ->
                                Toast.makeText(context!!,e.message, Toast.LENGTH_SHORT).show()
                            }.addOnSuccessListener { location ->
                                val userLatLng = LatLng(location.latitude, location.longitude)
                                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(userLatLng, 18f))
                            }
                        true
                    }

                    val locationButton = (mapFragment.requireView()
                        .findViewById<View>("1".toInt())
                        .parent!! as View)
                        .findViewById<View>("2".toInt())
                    val params = locationButton.layoutParams as RelativeLayout.LayoutParams

                    params.removeRule(RelativeLayout.ALIGN_PARENT_BOTTOM)
                    params.removeRule(RelativeLayout.ALIGN_PARENT_LEFT)
                    params.removeRule(RelativeLayout.ALIGN_PARENT_RIGHT)
                    params.removeRule(RelativeLayout.ALIGN_PARENT_TOP)

                    params.addRule(RelativeLayout.ALIGN_PARENT_TOP, RelativeLayout.TRUE)
                    params.addRule(RelativeLayout.ALIGN_PARENT_END, RelativeLayout.TRUE)
                    params.setMargins(0, 50, 50, 0) // top, right, bottom, left
                }

                override fun onPermissionDenied(p0: PermissionDeniedResponse?) {
                    Toast.makeText(context!!, "Permission "+p0!!.permissionName+"was denied", Toast.LENGTH_SHORT).show()
                }

                override fun onPermissionRationaleShouldBeShown(
                    p0: PermissionRequest?,
                    p1: PermissionToken?
                ) {

                }

            }).check()

        mMap.uiSettings.isZoomControlsEnabled = true

        try {
            val success = googleMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(requireContext(),
                R.raw.uber_maps_style))
            if(!success) {
                Log.e("EDMT_ERROR", "Style parsing error")
            }
        }catch (e: Resources.NotFoundException) {
            Log.e("EDMT_ERROR", e.message.toString())
        }
        Snackbar.make(mapFragment.requireView(), "You're Online!", Snackbar.LENGTH_SHORT).show()
    }

    fun resizeBitmap(context: Context, drawableRes: Int, width: Int, height: Int): Bitmap {
        val imageBitmap = BitmapFactory.decodeResource(context.resources, drawableRes)
        return Bitmap.createScaledBitmap(imageBitmap, width, height, false)
    }

    @SuppressLint("CheckResult")
    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    fun onDriverRequestReceived(event: DriverRequestReceived) {
        Log.d("EVENTBUS", "onDriverRequestReceived called with: $event")
        isRequestHandled = false
        mMap.isMyLocationEnabled = true


        //   Toast.makeText(context, "Popup Event Received!", Toast.LENGTH_SHORT).show()

        fusedLocationProviderClient.lastLocation
            .addOnFailureListener { e ->
                Snackbar.make(requireView(), e.message ?: "Location error", Snackbar.LENGTH_LONG).show()
            }
            .addOnSuccessListener { location ->
                if (location == null) {
                    Snackbar.make(requireView(), "Location not available", Snackbar.LENGTH_LONG).show()
                    return@addOnSuccessListener
                }

                compositeDisposable.add(
                    iGoogleAPI.getDirections(
                        "driving",
                        "less_driving",
                        "${location.latitude},${location.longitude}",
                        event.pickupLocation,
                        getString(R.string.maps_directions_api_key)
                    )
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe({ returnResult ->
                            Log.d("ROUTE_DEBUG", "Directions API result: $returnResult") // 1
                            try {
                                val jsonObject = JSONObject(returnResult)
                                val jsonArray = jsonObject.getJSONArray("routes")
                                Log.d("ROUTE_DEBUG", "Number of routes: ${jsonArray.length()}") // 2
                                for (i in 0 until jsonArray.length()) {
                                    val route = jsonArray.getJSONObject(i)
                                    val poly = route.getJSONObject("overview_polyline")
                                    val polyline = poly.getString("points")
                                    Log.d("ROUTE_DEBUG", "Decoded polyline: $polyline") // 3
                                    polylineList = Common.decodePoly(polyline)
                                    Log.d("ROUTE_DEBUG", "Polyline list size: ${polylineList?.size}") // 4
                                }




                                // Remove old polylines before drawing new ones
                                greyPolyline?.remove()
                                greyPolyline = null
                                blackPolyline?.remove()
                                blackPolyline = null

                                // Prepare PolylineOptions with latest polylineList
                                polylineOptions = PolylineOptions()
                                    .color(Color.GRAY)
                                    .width(12f)
                                    .startCap(SquareCap())
                                    .jointType(JointType.ROUND)
                                    .addAll(polylineList!!)

                                blackPolylineOptions = PolylineOptions()
                                    .color(Color.BLACK)
                                    .width(5f)
                                    .startCap(SquareCap())
                                    .jointType(JointType.ROUND)
                                    .addAll(polylineList!!)

                                // Only add if enough points
                                if (polylineList!!.size > 1) {
                                    greyPolyline = mMap.addPolyline(polylineOptions!!)
                                    blackPolyline = mMap.addPolyline(blackPolylineOptions!!)
                                    Log.d("ROUTE_DEBUG", "Polyline drawn with ${polylineList!!.size} points.")
                                } else {
                                    Log.w("ROUTE_DEBUG", "Polyline too short to display a route.")
                                }



                                // Animator
                                routeAnimator = ValueAnimator.ofInt(0, 100).apply {
                                    duration = 1100
                                    repeatCount = ValueAnimator.INFINITE
                                    interpolator = LinearInterpolator()
                                    addUpdateListener { value ->
                                        // Defensive check: only update if polyline is not null
                                        blackPolyline?.let { poly ->
                                            val points = greyPolyline?.points ?: return@let
                                            val percentValue = value.animatedValue.toString().toInt()
                                            val size = points.size
                                            val newpoints = (size * (percentValue / 100.0f)).toInt()
                                            val p = points.subList(0, newpoints)
                                            poly.points = p
                                        }
                                    }
                                    start()
                                }

                                val origin = LatLng(location.latitude, location.longitude)

                                val destination = LatLng(
                                    event.pickupLocation.split(",")[0].toDouble(),
                                    event.pickupLocation.split(",")[1].toDouble()
                                )
                                riderOriginLatLng = LatLng(
                                    event.pickupLocation.split(",")[0].toDouble(),
                                    event.pickupLocation.split(",")[1].toDouble()
                                )

                                Log.d("ROUTE_DEBUG", "Origin: $origin, Destination: $destination") // 7

                                val latLngBound = LatLngBounds.Builder()
                                    .include(origin)
                                    .include(destination)
                                    .build()

                                // Add car icon for origin
                                val objects = jsonArray.getJSONObject(0)
                                val legs = objects.getJSONArray("legs")
                                val legsObject = legs.getJSONObject(0)

                                val duration = legsObject.getJSONObject("duration").getString("text")
                                val distance = legsObject.getJSONObject("distance").getString("text")


                                txt_estimate_time.text = duration
                                txt_estimate_distance.text = distance



                                // Add marker at driver's current location (origin)
                                mMap.isMyLocationEnabled = true

                                Log.d("ROUTE_DEBUG", "Marker added at: $origin") // 8


                                pickupMarker = mMap.addMarker(
                                    MarkerOptions()
                                        .position(destination)
                                        .icon(BitmapDescriptorFactory.defaultMarker())
                                        .title("Pickup Location")
                                )

                                Log.d("ROUTE_DEBUG", "Marker added at: $destination") // 8

                                mMap.moveCamera(CameraUpdateFactory.newLatLngBounds(latLngBound, 160))
                                mMap.moveCamera(CameraUpdateFactory.zoomTo(mMap.cameraPosition!!.zoom - 1))
                                Log.d("ROUTE_DEBUG", "Camera moved to bounds") // 9


                                // Show popup
                                chip_decline.visibility = View.VISIBLE
                                layout_accept.visibility = View.VISIBLE

                                val riderOrigin = event.pickupLocation
                                val riderDestination = event.destinationLocation
                                iGoogleAPI.getDirections(
                                    "driving",
                                    "less_driving",
                                    riderOrigin,
                                    riderDestination,
                                    getString(R.string.maps_directions_api_key)
                                )
                                    .subscribeOn(Schedulers.io())
                                    .observeOn(AndroidSchedulers.mainThread())
                                    .subscribe({ tripResult ->
                                        try {
                                            val tripJson = JSONObject(tripResult)
                                            val tripRoutes = tripJson.getJSONArray("routes")
                                            if (tripRoutes.length() > 0) {
                                                val tripLegs = tripRoutes.getJSONObject(0).getJSONArray("legs")
                                                if (tripLegs.length() > 0) {
                                                    val tripLeg = tripLegs.getJSONObject(0)
                                                    val tripDuration = tripLeg.getJSONObject("duration").getString("text")
                                                    val tripDistance = tripLeg.getJSONObject("distance").getString("text")
                                                    val distanceValue = if (tripDistance.contains("km"))
                                                        tripDistance.replace(" km", "").toDouble()
                                                    else if (tripDistance.contains("m"))
                                                        tripDistance.replace(" m", "").toDouble() / 1000
                                                    else
                                                        0.0

                                                    val price = distanceValue * 10

                                                    val start_address = tripLeg.getString("start_address")
                                                    val end_address = tripLeg.getString("end_address")


                                                    txt_destination.text = end_address

                                                    txt_trip_time.text = "Trip Time: $tripDuration"
                                                    txt_trip_distance.text = "Trip Distance: $tripDistance"
                                                    txt_fare.text = "₹${"%.2f".format(price)}"
                                                    txt_fare1.text = "${"%.2f".format(price)}"

                                                }
                                            }
                                        } catch (e: Exception) {
                                            Log.e("TRIP_DEBUG", "Trip route parsing error: ${e.message}")
                                        }
                                    }, { error ->
                                        Log.e("TRIP_DEBUG", "Trip Directions API error: ${error.message}")
                                    })

                                // Reset progress bar to full
                                circularProgressBar.progress = 100

                                // Dispose any previous timer to avoid multiple timers
                                timerDisposable?.dispose()

                                // Start a 10-second countdown, updating every 100ms
                                timerDisposable = Observable
                                    .interval(100, TimeUnit.MILLISECONDS)
                                    .take(101) // 0 to 100 (for 10 seconds)
                                    .observeOn(AndroidSchedulers.mainThread())
                                    .subscribe { tick ->
                                        val progress = 100 - tick.toInt()
                                        circularProgressBar.progress = progress

                                        // When finished, hide popup
                                        if (tick == 100L) {
                                            if (!isRequestHandled) {
                                                isRequestHandled = true
                                                // 1. Hide popup and clean up UI
                                                chip_decline.visibility = View.GONE
                                                layout_accept.visibility = View.GONE
                                                riderInfoLayout.visibility = View.VISIBLE
                                                pickupMarker?.remove()
                                                pickupMarker = null
                                                clearRoute()
                                                greyPolyline?.remove()
                                                greyPolyline = null
                                                blackPolyline?.remove()
                                                blackPolyline = null

                                                Log.d("RideDebug", "Auto-accepting ride: ${event.tripId}")
                                                acceptRide(event)
                                            }
                                        }

                                    }
                                chip_decline.setOnClickListener {
                                    if (!isRequestHandled) {
                                        isRequestHandled = true
                                        chip_decline.visibility = View.GONE
                                        layout_accept.visibility = View.GONE
                                        timerDisposable?.dispose()
                                        pickupMarker?.remove()
                                        pickupMarker = null
                                        clearRoute()
                                        greyPolyline?.remove()
                                        greyPolyline = null
                                        blackPolyline?.remove()
                                        blackPolyline = null
                                        declineRide(event)
                                    }
                                }


                            } catch (e: Exception) {
                                Log.e("ROUTE_DEBUG", "Exception in route parsing: ${e.message}") // 10
                            }
                        }, { error ->
                            Log.e("ROUTE_DEBUG", "Directions API error: ${error.message}") // 11
                            Toast.makeText(context, error.message ?: "API error", Toast.LENGTH_SHORT).show()                        })
                )
            }
    }


    private fun clearRoute() {
        // Stop the animator
        routeAnimator?.cancel()
        routeAnimator = null

        // Remove polylines
        greyPolyline?.remove()
        greyPolyline = null
        blackPolyline?.remove()
        blackPolyline = null

    }

    fun updateDriverLocation(tripId: String, lat: Double, lng: Double) {
        Log.d("DriverLocation", "Attempting to update: $tripId, $lat, $lng")
        val tripLocationRef = FirebaseDatabase.getInstance().getReference("Trips").child(tripId).child("driverLocation")
        val locationMap = mapOf("lat" to lat, "lng" to lng)
        tripLocationRef.setValue(locationMap)
            .addOnSuccessListener { Log.d("DriverLocation", "Location updated!") }
            .addOnFailureListener { e -> Log.e("DriverLocation", "Failed to update: ${e.message}") }
    }


    private fun acceptRide(event: DriverRequestReceived) {
        val tripId = event.tripId
        val rideRef = FirebaseDatabase.getInstance().getReference("Trips").child(tripId)


        Log.d("AcceptRide", "Attempting to accept ride with transaction")

        rideRef.runTransaction(object : Transaction.Handler {
            override fun doTransaction(currentData: MutableData): Transaction.Result {
                val tripMap = currentData.value as? Map<*, *>
                val status = tripMap?.get("status") as? String

                Log.d("AcceptRide", "Transaction started. Current status: $status")

                if (status == null || status == "requested" || status == "declined") {
                    Log.d("AcceptRide", "Ride is eligible for acceptance (status: $status). Accepting now.")
                    currentData.child("status").value = "accepted"
                    currentData.child("driverId").value = FirebaseAuth.getInstance().currentUser!!.uid

                    return Transaction.success(currentData)
                } else {
                    Log.d("AcceptRide", "Ride is NOT eligible for acceptance (status: $status). Aborting transaction.")
                    return Transaction.abort()
                }
            }


            @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
            override fun onComplete(error: DatabaseError?, committed: Boolean, currentData: DataSnapshot?) {
                if (error != null) {
                    Log.e("AcceptRide", "Transaction error: ${error.message}")
                    Toast.makeText(context, "Accept failed: ${error.message}", Toast.LENGTH_SHORT).show()
                    return
                }
                if (!committed) {
                    Log.d("AcceptRide", "Ride already accepted by another driver.")
                    Toast.makeText(context, "Ride already accepted by another driver.", Toast.LENGTH_SHORT).show()
                    riderInfoLayout.visibility = View.GONE
                    return
                }

                // Transaction committed: fetch trip and rider info for UI updates
                activeTripId = tripId
                Log.d("AcceptRide", "activeTripId set: $activeTripId")
                // After: activeTripId = tripId
                startTripLocationUpdates()


                // Immediately push current location to trip node
                fusedLocationProviderClient.lastLocation.addOnSuccessListener { location ->
                    if (location != null) {
                        updateDriverLocation(activeTripId, location.latitude, location.longitude)
                        Log.d("DriverLocation", "Manually pushed driver location after accepting ride")
                    } else {
                        Log.d("DriverLocation", "No last known location available to push")
                    }
                }

                Log.d("AcceptRide", "Trip node updated successfully")
                rideRef.addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        Log.d("AcceptRide", "Fetched trip data from Firebase")
                        if (snapshot.exists()) {
                            val pickupLocation = snapshot.child("origin").value.toString()
                            val riderId = snapshot.child("rider").value.toString()


                            Log.d("AcceptRide", "pickupLocation: $pickupLocation, riderId: $riderId")

                            // Fetch rider's phone number from rider_users node
                            FirebaseDatabase.getInstance().getReference("rider_users")
                                .child(riderId)
                                .addListenerForSingleValueEvent(object : ValueEventListener {
                                    override fun onDataChange(riderSnapshot: DataSnapshot) {
                                        Log.d("AcceptRide", "Fetched rider data from Firebase")
                                        if (riderSnapshot.exists()) {
                                            val riderName = riderSnapshot.child("name").value.toString() // Add this line
                                            val riderPhoneNumber = riderSnapshot.child("phone").value.toString()
                                            val riderPhotoUrl = riderSnapshot.child("profileImageUrl").value?.toString()

                                            txtRiderName.text = riderName


                                            if (!riderPhotoUrl.isNullOrEmpty()) {
                                                Glide.with(requireContext())
                                                    .load(riderPhotoUrl)
                                                    .placeholder(R.drawable.baseline_account_circle_24) // Shown while loading
                                                    .error(R.drawable.baseline_account_circle_24)       // Shown if loading fails
                                                    .into(imgRider)
                                            } else {
                                                imgRider.setImageResource(R.drawable.baseline_account_circle_24)
                                            }

                                            Log.d("AcceptRide", "riderPhoneNumber: $riderPhoneNumber")

                                            // Set click listeners with fetched data
                                            btnStartUber.setOnClickListener {
                                                val coordinates = pickupLocation.split(",")
                                                if (coordinates.size == 2) {
                                                    val pickupLatLng = LatLng(coordinates[0].toDouble(), coordinates[1].toDouble())

                                                    if (checkLocationPermission()) {
                                                        val myLocation = mMap.myLocation
                                                        if (myLocation != null) {
                                                            val driverLatLng = LatLng(
                                                                myLocation.latitude,
                                                                myLocation.longitude
                                                            )

                                                            // Remove old markers if needed
                                                            mMap.clear()

                                                            // Driver marker (car icon)
                                                            driverMarker = mMap.addMarker(
                                                                MarkerOptions()
                                                                    .position(driverLatLng)
                                                                    .icon(
                                                                        BitmapDescriptorFactory.fromBitmap(
                                                                            resizeBitmap(
                                                                                requireContext(),
                                                                                R.drawable.car,
                                                                                35,
                                                                                80
                                                                            )
                                                                        )
                                                                    )
                                                                    .title("You")
                                                            )

                                                            // Rider marker (red marker)
                                                            mMap.addMarker(
                                                                MarkerOptions()
                                                                    .position(pickupLatLng)
                                                                    .icon(
                                                                        BitmapDescriptorFactory.defaultMarker(
                                                                            BitmapDescriptorFactory.HUE_RED
                                                                        )
                                                                    )
                                                                    .title("Pickup")
                                                            )

                                                            drawRoute(driverLatLng, riderOriginLatLng!!)


                                                            // Draw route as you already do
                                                            isRideStarted = true
                                                            isRideInProgress = false
                                                            startTripLocationUpdates()

                                                            //       drawRoute(driverLatLng, pickupLatLng)

                                                        }
                                                    }
                                                }
                                            }

                                            btnStartRide.setOnClickListener {
                                                isRideInProgress = true
                                                val destinationLocation = snapshot.child("destination").value?.toString()

                                                if (destinationLocation != null) {
                                                    val destinationLtgLng = destinationLocation.split(",")
                                                    if (destinationLtgLng.size == 2) {
                                                        val destinationLatLng = LatLng(
                                                            destinationLtgLng[0].toDouble(),
                                                            destinationLtgLng[1].toDouble()
                                                        )
                                                        val driverLatLng = driverMarker?.position ?: return@setOnClickListener

                                                        mMap.clear()
                                                        // Use destinationLatLng (not destinationLtgLng) for all map functions

                                                        drawRoute(
                                                           driverLatLng,
                                                            destinationLatLng
                                                        )

                                                        startTripLocationUpdates()


                                                        if (destinationMarker == null) {
                                                            destinationMarker = mMap.addMarker(
                                                                MarkerOptions()
                                                                    .position(destinationLatLng)
                                                                    .icon(
                                                                        BitmapDescriptorFactory.defaultMarker(
                                                                            BitmapDescriptorFactory.HUE_YELLOW
                                                                        )
                                                                    )
                                                            )
                                                        }
                                                        btnStartRide.visibility = View.GONE
                                                        btnCompleteRide.visibility = View.VISIBLE

                                                        FirebaseDatabase.getInstance().getReference("Trips")
                                                            .child(tripId)
                                                            .child("status")
                                                            .setValue("rideStarted")
                                                            .addOnSuccessListener {
                                                                Log.d("TripStatus", "Trip status updated to rideStarted")
                                                            }
                                                            .addOnFailureListener { e ->
                                                                Log.e("TripStatus", "Failed to update trip status: ${e.message}")
                                                            }
                                                        fetchRouteAndUpdateDriverUI(
                                                           driverLatLng,
                                                            destinationLatLng
                                                        )

                                                    }
                                                }
                                            }

                                            btnCompleteRide.setOnClickListener {
                                                mMap.clear()
                                                destinationMarker?.remove()
                                                destinationMarker = null
                                                btnCompleteRide.visibility = View.GONE
                                                btnStartUber.visibility = View.VISIBLE
                                                riderInfoLayout.visibility = View.GONE
                                                Toast.makeText(context, "You have completed your Trip", Toast.LENGTH_LONG).show()

                                                FirebaseDatabase.getInstance().getReference("Trips")
                                                    .child(tripId)
                                                    .child("status")
                                                    .setValue("completed")
                                                    .addOnSuccessListener {
                                                        Log.d("TripStatus", "Trip status updated to completed")

                                                        // Fetch the rider's FCM token
                                                        FirebaseDatabase.getInstance().getReference("Token")
                                                            .child(riderId)
                                                            .child("token")
                                                            .get()
                                                            .addOnSuccessListener { snapshot ->
                                                                val riderToken = snapshot.getValue(String::class.java)
                                                                if (riderToken != null) {
                                                                    sendTripCompletedNotification(riderToken)
                                                                } else {
                                                                    Log.e("NotificationHelper", "Rider token not found")
                                                                }
                                                            }
                                                            .addOnFailureListener { e ->
                                                                Log.e("NotificationHelper", "Failed to fetch rider token: ${e.message}")
                                                            }
                                                    }
                                                    .addOnFailureListener { e ->
                                                        Log.e("TripStatus", "Failed to update trip status: ${e.message}")
                                                    }
                                            }




                                            imgCallRider.setOnClickListener {
                                                Log.d("AcceptRide", "imgCallRider clicked, opening dialer")
                                                if (riderPhoneNumber.isNotEmpty()) {
                                                    val intent = Intent(Intent.ACTION_DIAL)
                                                    intent.data =
                                                        Uri.parse("tel:$riderPhoneNumber")
                                                    startActivity(intent)
                                                }
                                            }

                                            // Show rider info layout
                                            riderInfoLayout.visibility = View.VISIBLE
                                            Log.d("AcceptRide", "Rider info layout is now visible")
                                        } else {
                                            Log.e("AcceptRide", "Rider snapshot does not exist")
                                        }
                                    }

                                    override fun onCancelled(error: DatabaseError) {
                                        Log.e("AcceptRide", "Failed to fetch rider: ${error.message}")
                                    }
                                })
                        } else {
                            Log.e("AcceptRide", "Trip snapshot does not exist")
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        Log.e("AcceptRide", "Failed to fetch trip: ${error.message}")
                    }
                })
            }
        })
    }

    private fun checkLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }


    private fun declineRide(event: DriverRequestReceived) {
        Log.d("RideUpdate", "declineRide called for key: ${event.tripId}")
        val tripId = event.tripId

        val rideRef = FirebaseDatabase.getInstance().getReference("Trips").child(tripId)
        val updates = mapOf(
            "driverId" to FirebaseAuth.getInstance().currentUser!!.uid,
            "status" to "declined"
        )
        rideRef.updateChildren(updates)
            .addOnSuccessListener {
                Log.d("TripUpdate", "Ride declined and updated in Firebase")
                Toast.makeText(context, "Ride Declined!", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { error ->
                Log.e("TripUpdate", "Failed to decline ride: ${error.message}")
                Toast.makeText(context, "Failed to decline ride: ${error.message}", Toast.LENGTH_SHORT).show()
            }
    }


    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    private fun startTripLocationUpdates() {
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                val location = locationResult.lastLocation ?: return
                val driverCurrentLatLng = LatLng(location.latitude, location.longitude)
                val driverLatLng = driverMarker?.position ?: return

                updateDriverLocation(activeTripId, location.latitude, location.longitude)

                riderOriginLatLng?.let { pickupLatLng ->
                    val distanceToPickup = FloatArray(1)
                    Location.distanceBetween(
                        driverCurrentLatLng.latitude, driverCurrentLatLng.longitude,
                        pickupLatLng.latitude, pickupLatLng.longitude,
                        distanceToPickup
                    )
                    // Update UI based on distance
                    if (distanceToPickup[0] <= 50) {
                        btnStartUber.visibility = View.GONE
                        btnStartRide.visibility = View.VISIBLE
                    } else {
                        btnStartUber.visibility = View.VISIBLE
                        btnStartRide.visibility = View.GONE
                    }
                    // Let TripNotificationManager handle the notification
                    if (::activeTripId.isInitialized) {
                        val pickupLocation = latLngToLocation(pickupLatLng)
                        TripNotificationManager.checkDriverArrivedAndNotify(
                            location.latitude,
                            location.longitude,
                            pickupLocation,
                            activeTripId
                        )
                    }
                }

                if (destinationLatLng != null) {
                    updateTripTimeAndDistance(driverCurrentLatLng, destinationLatLng!!)
                }

                if (isRideStarted && !isRideInProgress && riderOriginLatLng != null) {
                    drawRoute(driverCurrentLatLng, riderOriginLatLng!!)
                    updateTripTimeAndDistance(driverCurrentLatLng, riderOriginLatLng!!)
                }

                if (isRideInProgress && destinationLatLng != null) {
                    drawRoute(driverLatLng, destinationLatLng!!)
                    updateTripTimeAndDistance(driverCurrentLatLng, destinationLatLng!!)
                }

                fetchRouteAndUpdateDriverUI(
                    LatLng(location.latitude, location.longitude),
                    riderOriginLatLng
                )
            }
        }
        fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
    }

    // Helper function to convert LatLng to Location
    private fun latLngToLocation(latLng: LatLng): Location {
        val location = Location("")
        location.latitude = latLng.latitude
        location.longitude = latLng.longitude
        return location
    }



    private fun updateTripTimeAndDistance(origin: LatLng, destination: LatLng) {
        val originString = "${origin.latitude},${origin.longitude}"
        val destinationString = "${destination.latitude},${destination.longitude}"

        compositeDisposable.add(
            iGoogleAPI.getDirections(
                "driving",
                "less_driving",
                originString,
                destinationString,
                getString(R.string.maps_directions_api_key)
            )
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ tripResult ->
                    try {
                        val tripJson = JSONObject(tripResult)
                        val tripRoutes = tripJson.getJSONArray("routes")
                        if (tripRoutes.length() > 0) {
                            val tripLegs = tripRoutes.getJSONObject(0).getJSONArray("legs")
                            if (tripLegs.length() > 0) {
                                val tripLeg = tripLegs.getJSONObject(0)
                                val tripDuration = tripLeg.getJSONObject("duration").getString("text")
                                val tripDistance = tripLeg.getJSONObject("distance").getString("text")
                                txt_trip_time.text = "Trip Time: $tripDuration"
                                txt_trip_distance.text = "Trip Distance: $tripDistance"
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("TRIP_DEBUG", "Trip route parsing error: ${e.message}")
                    }
                }, { error ->
                    Log.e("TRIP_DEBUG", "Trip Directions API error: ${error.message}")
                })
        )
    }


    private fun fetchRouteAndUpdateDriverUI(origin: LatLng, destination: LatLng) {
        val originString = "${origin.latitude},${origin.longitude}"
        val destinationString = "${destination.latitude},${destination.longitude}"


        iGoogleAPI.getDirections(
            "driving",
            "less_driving",
            originString,
            destinationString,
            getString(R.string.maps_directions_api_key)
        )
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ response ->
                val jsonObject = JSONObject(response)
                val routes = jsonObject.getJSONArray("routes")
                if (routes.length() > 0) {
                    val legs = routes.getJSONObject(0).getJSONArray("legs")
                    if (legs.length() > 0) {
                        val leg = legs.getJSONObject(0)
                        val duration = leg.getJSONObject("duration").getString("text")
                        val distance = leg.getJSONObject("distance").getString("text")

                        txtEta.text = duration
                        txtDistance.text = distance
                    }
                }
            }, { throwable ->
                Log.e("Route", "Error fetching route: ${throwable.message}")
            })
    }




    private fun drawRoute(origin: LatLng, destination: LatLng) {
        val originString = "${origin.latitude},${origin.longitude}"
        val destinationString = "${destination.latitude},${destination.longitude}"


        compositeDisposable.add(
            iGoogleAPI.getDirections(
                "driving",
                "less_driving",
                originString,
                destinationString,
                getString(R.string.maps_directions_api_key)
            )
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ response ->
                    val jsonObject = JSONObject(response)
                    val routes = jsonObject.getJSONArray("routes")
                    if (routes.length() > 0) {
                        val overviewPolyline = routes.getJSONObject(0)
                            .getJSONObject("overview_polyline")
                            .getString("points")
                        val polylineList = PolyUtil.decode(overviewPolyline) // This is List<LatLng>
                        animatePolyline(polylineList)
                    }else {
                        Log.e("Route", "No routes found in response")
                    }
                }, { throwable ->
                    Log.e("Route", "Error fetching route: ${throwable.message}")
                })
        )
    }

    private fun animatePolyline(polylineList: List<LatLng>) {
        // Always remove the previous polyline before adding a new one
        routePolyline?.remove()
        routePolyline = mMap.addPolyline(
            PolylineOptions()
                .color(Color.BLACK)
                .width(8f)
                .addAll(polylineList)
        )
    }

    override fun onResume() {
        super.onResume()
        if (::onlineRef.isInitialized) {
            registerOnlineSystem()
        }
    }

    private fun registerOnlineSystem() {
        onlineRef.addValueEventListener(onlineValueEventListener)
    }

}
