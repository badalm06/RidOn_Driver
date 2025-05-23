package com.example.uberremake.ui.home

import android.Manifest
import android.content.res.Resources
import android.location.Address
import android.location.Geocoder
import android.os.Build
import android.os.Bundle
import android.os.Looper
import android.text.style.RelativeSizeSpan
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RelativeLayout
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresPermission
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.uberremake.R
import com.example.uberremake.Common
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
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.snackbar.Snackbar
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.util.Locale
import kotlin.math.log

class HomeFragment : Fragment(), OnMapReadyCallback {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private lateinit var mMap: GoogleMap

    private lateinit var mapFragment: SupportMapFragment


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


    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val homeViewModel = ViewModelProvider(this).get(HomeViewModel::class.java)

        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        val root: View = binding.root

        init()

        mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        return root
    }

    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    private fun init() {

        onlineRef = FirebaseDatabase.getInstance().getReference().child(".info/connected")



        locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            5000L // interval
        )
            .setMinUpdateIntervalMillis(3000L) // fastest interval
            .setMinUpdateDistanceMeters(10f) // smallest displacement
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
                            } else {
                                Snackbar.make(mapFragment.requireView(), "You're Online!", Snackbar.LENGTH_SHORT).show()
                            }
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


        // Add a marker and move the camera
        val customLocation = LatLng(27.4270, 77.6969) // Latitude, Longitude of Mathura
        mMap.addMarker(MarkerOptions().position(customLocation).title("Marker in Mathura"))
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(customLocation, 15f)) // 15f = Zoom level


    }

    override fun onResume() {
        super.onResume()
        registerOnlineSystem()
    }

    private fun registerOnlineSystem() {
        onlineRef.addValueEventListener(onlineValueEventListener)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        geoFire.removeLocation(FirebaseAuth.getInstance().currentUser!!.uid)
        onlineRef.removeEventListener(onlineValueEventListener)
    }
}
