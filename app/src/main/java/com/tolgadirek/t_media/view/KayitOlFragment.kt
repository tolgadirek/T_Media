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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.ktx.storage
import com.tolgadirek.t_media.databinding.FragmentKayitOlBinding
import com.tolgadirek.t_media.model.ProfilFotografi
import kotlinx.coroutines.tasks.await
import java.util.UUID

class KayitOlFragment : Fragment() {

    private var _binding: FragmentKayitOlBinding? = null
    private val binding get() = _binding!!
    private lateinit var auth : FirebaseAuth
    private lateinit var db : FirebaseFirestore
    private lateinit var storage : FirebaseStorage

    private lateinit var permissionLauncher : ActivityResultLauncher<String>
    private lateinit var activityResultLauncher : ActivityResultLauncher<Intent>
    private var secilenGorsel : Uri? = null
    private var secilenBitmap : Bitmap? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = Firebase.auth
        db = Firebase.firestore
        storage = Firebase.storage

        registerLaunchers()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentKayitOlBinding.inflate(inflater, container, false)
        val view = binding.root
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.kayitOlButton.setOnClickListener { kayitOl(it) }
        binding.profilFotografi.setOnClickListener { profilFotografi(it) }

    }

    private fun kayitOl(view: View) {
        val email = binding.emailEditText.text.toString()
        val password = binding.passwordEditText.text.toString()
        val kullaniciAdi = binding.kullaniciAdiEditText.text.toString()

        val uuid = UUID.randomUUID()  // Rastgele isim atıyor.
        val gorselAdi = "${uuid}.jpg" // Farklı isimlerde kaydetmek için.
        val reference = storage.reference // Storage'a bağlanıyor.

        if (email.isNotEmpty() && password.isNotEmpty()) {
            auth.createUserWithEmailAndPassword(email, password).addOnSuccessListener { task ->
                // Kullanıcı başarıyla oluşturuldu
                kullaniciAdiKaydet(kullaniciAdi) // Kullanıcı adını kaydet
                if (secilenGorsel != null) {
                    val gorselReferansi = reference.child("profilFotografi").child(gorselAdi) // 'profilFotografi' altında kaydediliyor.

                    gorselReferansi.putFile(secilenGorsel!!).addOnSuccessListener { uploadTask ->
                        // URL alma işlemi
                        gorselReferansi.downloadUrl.addOnSuccessListener { uri ->
                            val downloadUrl = uri.toString()

                            // ProfilFotografi modelini oluştur
                            val ppMap = hashMapOf<String, Any>()
                            ppMap.put("downloadUrl", downloadUrl)
                            ppMap.put("email", email)
                            // Firestore'a veri kaydetme
                            db.collection("ProfilFotografi").add(ppMap).addOnFailureListener { exception ->
                                    Toast.makeText(requireContext(), exception.localizedMessage, Toast.LENGTH_LONG).show()
                                }
                        }
                    }.addOnFailureListener { exception ->
                        Toast.makeText(requireContext(), exception.localizedMessage, Toast.LENGTH_LONG).show()
                    }
                } else {
                    val gorselReferansi = reference.child("pp.jpg")
                    // Fotoğrafın indirme URL'sini al
                    gorselReferansi.downloadUrl.addOnSuccessListener { uri ->
                        val downloadUrl = uri.toString()
                        val ppMap = hashMapOf<String, Any>()
                        ppMap.put("downloadUrl", downloadUrl)
                        ppMap.put("email", email)
                        // Firestore'a veri kaydetme
                        db.collection("ProfilFotografi").add(ppMap).addOnFailureListener { exception ->
                            Toast.makeText(requireContext(), exception.localizedMessage, Toast.LENGTH_LONG).show()
                        }
                    }
                }

                val action = KayitOlFragmentDirections.actionKayitOlFragmentToFeedFragment()
                Navigation.findNavController(view).navigate(action)
            }.addOnFailureListener { exception ->
                Toast.makeText(requireContext(), exception.localizedMessage, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun kullaniciAdiKaydet(kullaniciAdi: String) {
        val kullaniciId = auth.currentUser?.uid ?: return // Null kontrolü

        val kullaniciData = hashMapOf("kullanici" to kullaniciAdi)
        db.collection("kullanicilar").document(kullaniciId).set(kullaniciData)
            .addOnFailureListener { e ->
                // Hata durumu
                Toast.makeText(requireContext(), e.localizedMessage, Toast.LENGTH_LONG).show()
            }
    }

    private fun profilFotografi(view: View) {
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
                            binding.profilFotografi.setImageBitmap(secilenBitmap)
                        } else{
                            secilenBitmap = MediaStore.Images.Media.getBitmap(requireActivity().contentResolver, secilenGorsel)
                            binding.profilFotografi.setImageBitmap(secilenBitmap)
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