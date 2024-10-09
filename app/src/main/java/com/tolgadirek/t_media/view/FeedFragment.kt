package com.tolgadirek.t_media.view

import android.content.DialogInterface
import android.icu.text.SimpleDateFormat
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.navigation.Navigation
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.tolgadirek.t_media.R
import com.tolgadirek.t_media.adapter.PostAdapter
import com.tolgadirek.t_media.databinding.FragmentFeedBinding
import com.tolgadirek.t_media.model.Post
import java.util.Locale

class FeedFragment : Fragment(), PopupMenu.OnMenuItemClickListener {

    private var _binding: FragmentFeedBinding? = null
    private val binding get() = _binding!!
    private lateinit var auth : FirebaseAuth
    private lateinit var db : FirebaseFirestore

    val postList : ArrayList<Post> = arrayListOf()
    var adapter : PostAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = Firebase.auth
        db = Firebase.firestore

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentFeedBinding.inflate(inflater, container, false)
        val view = binding.root
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.feedMenuButton.setOnClickListener { menuButonTiklandi(it) }
        binding.feedRecyclerView.layoutManager = LinearLayoutManager(requireContext())

        fireStoreVerileriAl()

        adapter = PostAdapter(postList)
        binding.feedRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.feedRecyclerView.adapter = adapter
    }

    private fun fireStoreVerileriAl() {
        db.collection("Posts").orderBy("tarih", Query.Direction.DESCENDING).addSnapshotListener { value, error -> //orderby ile tarihe göre sıraladık.
            if (error != null) {
                Toast.makeText(requireContext(), error.localizedMessage, Toast.LENGTH_LONG).show()
            } else {
                if (value != null) {
                    if (!value.isEmpty) {
                        postList.clear()
                        val documents = value.documents
                        for (document in documents) {
                            val yorum = document.get("yorum") as? String ?: ""
                            val kullaniciAdi = document.get("kullaniciAdi") as String
                            val gonderiUrl = document.get("gonderiUrl") as? String ?: ""
                            val tarih = document.get("tarih") as Timestamp
                            val ppUrl = document.get("ppUrl") as? String ?: ""

                            val tarihString = tarih.toDate().let { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(it) }

                            val post = Post(ppUrl, kullaniciAdi, yorum, gonderiUrl, tarihString)
                            postList.add(post)
                        }
                        adapter?.notifyDataSetChanged() //Adaptörü uyar, bana yeni veri geldi, kendini yenile demek
                        // Bu daha çok chat uygulamalrında lazım olur. Bunda çok gerek yok ama bilelim diye yazdık.
                    }
                }
            }
        }
    }

    private fun menuButonTiklandi(view: View){
        val popup = PopupMenu(requireContext(), binding.feedMenuButton)
        val inflater  = popup.menuInflater
        inflater.inflate(R.menu.feed_popup_menu,popup.menu)
        popup.setOnMenuItemClickListener(this)
        popup.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onMenuItemClick(item: MenuItem): Boolean {
        if (item.itemId == R.id.gonderiOlusturMenu) {
            val action = FeedFragmentDirections.actionFeedFragmentToYuklemeFragment()
            Navigation.findNavController(requireView()).navigate(action)

        } else if (item.itemId == R.id.profilMenu) {
            val action = FeedFragmentDirections.actionFeedFragmentToBilgileriDuzenleFragment()
            Navigation.findNavController(requireView()).navigate(action)

        } else if (item.itemId == R.id.cikisYapMenu) {
            val alert = AlertDialog.Builder(requireContext()) //applicationContext yazdığımızda hata verdi.
            alert.setTitle("Çıkış Yap") //Uyarı başlığı
            alert.setMessage("Çıkış Yapmak İstediğinize Emin misiniz?")
            alert.setPositiveButton("Evet") { dialog, which ->
                auth.signOut()
                val action = FeedFragmentDirections.actionFeedFragmentToKullaniciGirisFragment()
                Navigation.findNavController(requireView()).navigate(action) }
            alert.setNegativeButton("Hayır") { dialog, which -> }
            alert.show()
        }
        return true
    }
}