package com.example.uberremake.ui.home

import android.Manifest
import android.animation.ValueAnimator
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Resources
import android.graphics.Color
import android.location.Address
import android.location.Geocoder
import android.location.LocationManager
import android.media.audiofx.BassBoost.Settings
import android.os.Build
import android.os.Bundle
import android.os.Looper
import android.text.style.RelativeSizeSpan
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.LinearInterpolator
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
import com.example.uberremake.R
import com.example.uberremake.Common
import com.example.uberremake.DriverHomeActivity
import com.example.uberremake.Model.DriverRequestReceived
import com.example.uberremake.Remote.IGoogleAPI
import com.example.uberremake.Remote.RetrofitClient
import com.example.uberremake.databinding.FragmentHomeBinding
import com.firebase.geofire.GeoFire
import com.firebase.geofire.GeoLocation
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
import kotlin.math.log

class HomeFragment : Fragment(), OnMapReadyCallback {

    // Views
    private lateinit var chip_decline: Chip
    private lateinit var layout_accept: CardView
    private lateinit var circularProgressBar: ProgressBar
    private var timerDisposable: Disposable? = null
    private lateinit var txt_estimate_time: TextView
    private lateinit var txt_estimate_distance: TextView
    private var pickupMarker: Marker? = null
    private var routeAnimator: ValueAnimator? = null





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
        EventBus.getDefault().register(this)
    }





//    override fun onDestroyView() {
//        EventBus.getDefault().unregister(this)
//        super.onDestroyView()
//    }


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
                super.onLocationResult(locationResult)
                val location = locationResult.lastLocation ?: return
                val newPos = LatLng(location.latitude, location.longitude)
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(newPos, 18f))

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
                    params.addRule(RelativeLayout.ALIGN_PARENT_TOP, 0)
                    params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, RelativeLayout.TRUE)
                    params.bottomMargin = 350

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

    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    fun onDriverRequestReceived(event: DriverRequestReceived) {
        Log.d("EVENTBUS", "onDriverRequestReceived called with: $event")
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
                                            // Hide popup and remove marker & route
                                            chip_decline.visibility = View.GONE
                                            layout_accept.visibility = View.GONE
                                            pickupMarker?.remove()
                                            pickupMarker = null
                                            clearRoute()
                                            Log.d("ROUTE_DEBUG", "Removing grey polyline: $greyPolyline")
                                            greyPolyline?.remove()
                                            greyPolyline = null
                                            Log.d("ROUTE_DEBUG", "Removing black polyline: $blackPolyline")
                                            blackPolyline?.remove()
                                            blackPolyline = null

                                        }
                                    }
                                chip_decline.setOnClickListener {
                                    chip_decline.visibility = View.GONE
                                    layout_accept.visibility = View.GONE
                                    timerDisposable?.dispose()
                                    pickupMarker?.remove()
                                    pickupMarker = null
                                    clearRoute()

                                    // REMOVE POLYLINES HERE
                                    Log.d("ROUTE_DEBUG", "Removing grey polyline: $greyPolyline")
                                    greyPolyline?.remove()
                                    greyPolyline = null
                                    Log.d("ROUTE_DEBUG", "Removing black polyline: $blackPolyline")
                                    blackPolyline?.remove()
                                    blackPolyline = null


                                    Toast.makeText(context, "Request Declined", Toast.LENGTH_SHORT).show()
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
