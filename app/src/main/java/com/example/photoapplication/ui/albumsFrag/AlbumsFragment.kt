package com.example.photoapplication.ui.albumsFrag

import android.app.ProgressDialog
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.photoapplication.R
import com.example.photoapplication.data.OfflineDB.DatabaseRepository
import com.example.photoapplication.network.ApiService
import com.example.photoapplication.repository.AlbumRepository
import kotlinx.android.synthetic.main.album_fragment.*


class AlbumsFragment : Fragment() {

    private var userId: String? = "-1"
    private lateinit var viewModel: AlbumsViewModel
    private lateinit var factory: AlbumsViewModelFactory
    private lateinit var databaseRepository: DatabaseRepository

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        userId = arguments!!.getString("id")
        return inflater.inflate(R.layout.album_fragment, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        val progressDialog = ProgressDialog(context)
        progressDialog.setMessage("Loading Your Information...")
        progressDialog.setCancelable(false)
        progressDialog.show()

        val apiService = ApiService()
        val repository = AlbumRepository(apiService)
        databaseRepository = context?.let { DatabaseRepository.getInstance(it) }!!
        factory = AlbumsViewModelFactory(repository, databaseRepository)
        viewModel = ViewModelProviders.of(this, factory).get(AlbumsViewModel::class.java)
        if (isNetworkAvailable(context!!)) {
            viewModel.getAlbumsByUserId(userId.toString())
            viewModel.albums.observe(viewLifecycleOwner, Observer { albums ->
                recycler_view_albums.also {
                    it.layoutManager = LinearLayoutManager(requireContext())
                    it.setHasFixedSize(true)
                    it.adapter =
                        AlbumAdapter(
                            albums
                        )
                    viewModel.saveAllAlbumsToDb(albums)
                    progressDialog.dismiss()
                }
            })
        } else {
            Toast.makeText(context, "No internet Available", Toast.LENGTH_SHORT).show()
            viewModel.getAlbumsByUserIdFromDb(userId.toString())
            viewModel.albums.observe(viewLifecycleOwner, Observer { albums ->
                if (albums.isEmpty()) {
                    val activity = recycler_view_albums.context as AppCompatActivity
                    activity.supportFragmentManager.popBackStack()
                    progressDialog.dismiss()
                } else {
                    recycler_view_albums.also {
                        it.layoutManager = LinearLayoutManager(requireContext())
                        it.setHasFixedSize(true)
                        it.adapter =
                            AlbumAdapter(
                                albums
                            )
                        progressDialog.dismiss()
                    }
                }
            })
        }
    }

    private fun isNetworkAvailable(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE)
        return if (connectivityManager is ConnectivityManager) {
            val networkInfo: NetworkInfo? = connectivityManager.activeNetworkInfo
            networkInfo?.isConnected ?: false
        } else false
    }
}
