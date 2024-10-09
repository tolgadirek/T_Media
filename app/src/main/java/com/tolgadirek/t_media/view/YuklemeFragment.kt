package com.tolgadirek.t_media.view

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.navigation.Navigation
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.ktx.storage
import com.squareup.picasso.Picasso
import com.tolgadirek.t_media.databinding.FragmentYuklemeBinding
import com.tolgadirek.t_media.model.ProfilFotografi
import java.util.UUID

class YuklemeFragment : Fragment() {

    private var _binding: FragmentYuklemeBinding? = null
    private val binding get() = _binding!!

    private lateinit var db : FirebaseFirestore
    private lateinit var storage : FirebaseStorage
    private lateinit var auth : FirebaseAuth

    private lateinit var permissionLauncher : ActivityResultLauncher<String>
    private lateinit var activityResultLauncher : ActivityResultLauncher<Intent>
    private var secilenGorsel : Uri? = null
    private var secilenBitmap : Bitmap? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        db = Firebase.firestore
        storage = Firebase.storage
        auth = Firebase.auth

        registerLaunchers()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentYuklemeBinding.inflate(inflater, container, false)
        val view = binding.root
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.fotografSecImageView.setOnClickListener { fotografSec(it) }
        binding.paylasButton.setOnClickListener { paylas(it) }
    }

    private fun paylas(view: View) {
        val uuid = UUID.randomUUID()
        val gorselAdi = "${uuid}.jpg"
        val reference = storage.reference

        db.collection("ProfilFotografi").whereEqualTo("email", auth.currentUser?.email).get().addOnSuccessListener { documents ->
            for (document in documents) {
                val profilFotografi = document.toObject(ProfilFotografi::class.java)
                val ppUrl = profilFotografi.downloadUrl
                if (secilenGorsel != null) {
                    val gorselReferansi = reference.child("postFotograflari").child(gorselAdi)
                    gorselReferansi.putFile(secilenGorsel!!).addOnSuccessListener { Task ->
                        gorselReferansi.downloadUrl.addOnSuccessListener { uri ->
                            val gonderiUrl = uri.toString()
                            kullaniciAdiAl { kullaniciAdi ->
                                val postMap = hashMapOf<String, Any>()
                                postMap.put("ppUrl", ppUrl)
                                postMap.put("kullaniciAdi", kullaniciAdi.toString())
                                postMap.put("gonderiUrl", gonderiUrl)
                                postMap.put("yorum", binding.yorumYazEditText.text.toString())
                                postMap.put("tarih", Timestamp.now())

                                db.collection("Posts").add(postMap).addOnSuccessListener { documentReferance ->
                                    // Veri database e yüklenmiş oluyor...
                                    val action = YuklemeFragmentDirections.actionYuklemeFragmentToFeedFragment()
                                    Navigation.findNavController(view).navigate(action)
                                }.addOnFailureListener { exception ->
                                    Toast.makeText(requireContext(), exception.localizedMessage, Toast.LENGTH_LONG).show()
                                }
                            }

                        }
                    }.addOnFailureListener { exception ->
                        Toast.makeText(requireContext(), exception.localizedMessage, Toast.LENGTH_LONG).show() }
                } else {
                    kullaniciAdiAl { kullaniciAdi ->
                        val postMap = hashMapOf<String, Any>()
                        postMap.put("ppUrl", ppUrl)
                        postMap.put("kullaniciAdi", kullaniciAdi.toString())
                        postMap.put("yorum", binding.yorumYazEditText.text.toString())
                        postMap.put("tarih", Timestamp.now())

                        db.collection("Posts").add(postMap).addOnSuccessListener { documentReferance ->
                            // Veri database e yüklenmiş oluyor...
                            val action = YuklemeFragmentDirections.actionYuklemeFragmentToFeedFragment()
                            Navigation.findNavController(view).navigate(action)
                        }.addOnFailureListener { exception ->
                            Toast.makeText(requireContext(), exception.localizedMessage, Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }
        }
    }

    private fun kullaniciAdiAl(callback: (String?) -> Unit) {
        val kullaniciId = auth.currentUser?.uid
        if (kullaniciId != null) {
            db.collection("kullanicilar").document(kullaniciId).get().addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        val kullaniciAdi = document.getString("kullanici")
                        callback(kullaniciAdi) // Kullanıcı adını döndür
                    }
            }.addOnFailureListener { e ->
                Toast.makeText(requireContext(), e.localizedMessage, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun fotografSec(view: View) {
        if (Build.VERSION.SDK_INT >= 33) {
            if(ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED){
                //Burada izin baştan var mı yok mu androide kontrol ettiriyoruz.
                //İzin verilmemiş, izin istemeliyiz.
                if (ActivityCompat.shouldShowRequestPermissionRationale(requireActivity(), Manifest.permission.READ_MEDIA_IMAGES)){
                    //Snackbar göstermemiz lazım, kullanıcıdan neden izin istediğimizi bir kez daha söyleyerek izin istemememiz lazım.
                    Snackbar.make(view, "Galeriye Gitmek İçin İzin Vermeniz Gerekiyor", Snackbar.LENGTH_INDEFINITE).setAction( // İndefinite dememizin sebebi kullanıcı bir şey seçene kadar kapanmasın.
                        "İzin Ver", View.OnClickListener {
                            // İzin isteyeceğiz.
                            permissionLauncher.launch(Manifest.permission.READ_MEDIA_IMAGES)
                        }).show()
                } else {
                    // İzin isteyeceğiz
                    permissionLauncher.launch(Manifest.permission.READ_MEDIA_IMAGES)
                }
            } else {
                // İzin verillmiş, galeriye gidebilirim.
                val intentToGallery = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                activityResultLauncher.launch(intentToGallery)
            }
        } else {
            if(ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){
                //Burada izin baştan var mı yok mu androide kontrol ettiriyoruz.
                //İzin verilmemiş, izin istemeliyiz.
                if (ActivityCompat.shouldShowRequestPermissionRationale(requireActivity(), Manifest.permission.READ_EXTERNAL_STORAGE)){
                    //Snackbar göstermemiz lazım, kullanıcıdan neden izin istediğimizi bir kez daha söyleyerek izin istemememiz lazım.
                    Snackbar.make(view, "Galeriye ulaşıp görsel seçmemiz lazım!", Snackbar.LENGTH_INDEFINITE).setAction( // İndefinite dememizin sebebi kullanıcı bir şey seçene kadar kapanmasın.
                        "İzin Ver", View.OnClickListener {
                            // İzin isteyeceğiz.
                            permissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
                        }).show()
                } else {
                    // İzin isteyeceğiz
                    permissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
                }
            } else {
                // İzin verillmiş, galeriye gidebilirim.
                val intentToGallery = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                activityResultLauncher.launch(intentToGallery)
            }
        }
    }

    private fun registerLaunchers(){
        //Galeriye Gitme kodu
        activityResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()){ result ->
            if ( result.resultCode == AppCompatActivity.RESULT_OK){
                val intentFromResult = result.data
                if (intentFromResult != null){
                    secilenGorsel = intentFromResult.data
                    try {
                        if (Build.VERSION.SDK_INT >= 28) {
                            val source = ImageDecoder.createSource(requireActivity().contentResolver, secilenGorsel!!)
                            secilenBitmap = ImageDecoder.decodeBitmap(source)
                            binding.fotografSecImageView.setImageBitmap(secilenBitmap)
                        } else{
                            secilenBitmap = MediaStore.Images.Media.getBitmap(requireActivity().contentResolver, secilenGorsel)
                            binding.fotografSecImageView.setImageBitmap(secilenBitmap)
                        }
                    } catch (e: Exception){ e.printStackTrace() }
                }
            }
        }

        permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()){ result ->
            if (result) {
                //İzin verildi, galeriye gidebiliriz.
                val intentToGallery = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                activityResultLauncher.launch(intentToGallery)
            } else {
                // İzin verilmedi
                Toast.makeText(requireContext(), "İzin Verilmedi!", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}