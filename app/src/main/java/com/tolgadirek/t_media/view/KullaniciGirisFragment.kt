package com.tolgadirek.t_media.view

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.navigation.Navigation
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.tolgadirek.t_media.databinding.FragmentKullaniciGirisBinding

class KullaniciGirisFragment : Fragment() {

    private var _binding: FragmentKullaniciGirisBinding? = null
    private val binding get() = _binding!!
    private lateinit var auth : FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = Firebase.auth
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentKullaniciGirisBinding.inflate(inflater, container, false)
        val view = binding.root
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.girisYapButton.setOnClickListener { girisYap(it) }
        binding.kayitEkraninaGitButton.setOnClickListener { kayitEkraninaGit(it) }

        val currentUser = auth.currentUser

        if (currentUser != null) {
            val action = KullaniciGirisFragmentDirections.actionKullaniciGirisFragmentToFeedFragment()
            Navigation.findNavController(requireView()).navigate(action)
        }
    }

    private fun girisYap(view : View) {
        val userEmail = binding.editEmailText.text.toString()
        val password = binding.editPasswordText.text.toString()

        if (userEmail.isNotEmpty() && password.isNotEmpty()) {
            auth.signInWithEmailAndPassword(userEmail,password).addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val action = KullaniciGirisFragmentDirections.actionKullaniciGirisFragmentToFeedFragment()
                    Navigation.findNavController(requireView()).navigate(action)
                }
            }.addOnFailureListener { exception ->
                Toast.makeText(requireContext(),exception.localizedMessage,Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun kayitEkraninaGit(view: View) {
        val action = KullaniciGirisFragmentDirections.actionKullaniciGirisFragmentToKayitOlFragment()
        Navigation.findNavController(requireView()).navigate(action)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}