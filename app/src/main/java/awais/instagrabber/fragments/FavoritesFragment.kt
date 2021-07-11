package awais.instagrabber.fragments

import android.content.DialogInterface
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import awais.instagrabber.R
import awais.instagrabber.adapters.FavoritesAdapter
import awais.instagrabber.databinding.FragmentFavoritesBinding
import awais.instagrabber.db.entities.Favorite
import awais.instagrabber.models.enums.FavoriteType
import awais.instagrabber.utils.extensions.TAG
import awais.instagrabber.viewmodels.FavoritesViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class FavoritesFragment : Fragment() {
    private var shouldRefresh = true

    private lateinit var binding: FragmentFavoritesBinding
    private lateinit var root: RecyclerView
    private lateinit var adapter: FavoritesAdapter

    private val favoritesViewModel: FavoritesViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        if (this::root.isInitialized) {
            shouldRefresh = false
            return root
        }
        binding = FragmentFavoritesBinding.inflate(layoutInflater)
        root = binding.root
        binding.favoriteList.layoutManager = LinearLayoutManager(context)
        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        if (!shouldRefresh) return
        init()
        shouldRefresh = false
    }

    override fun onPause() {
        super.onPause()
        adapter.stateRestorationPolicy = RecyclerView.Adapter.StateRestorationPolicy.PREVENT
    }

    override fun onResume() {
        super.onResume()
        if (!this::adapter.isInitialized) return
        // refresh list every time in onViewStateRestored since it is cheaper than implementing pull down to refresh
        favoritesViewModel.list.observe(viewLifecycleOwner, { list: List<Favorite?>? ->
            adapter.submitList(list) {
                adapter.stateRestorationPolicy = RecyclerView.Adapter.StateRestorationPolicy.ALLOW
            }
        })
    }

    private fun init() {
        adapter = FavoritesAdapter({ model: Favorite ->
            when (model.type) {
                FavoriteType.USER -> {
                    try {
                        val username = model.query ?: return@FavoritesAdapter
                        val actionToProfile = FavoritesFragmentDirections.actionToProfile().apply { this.username = username }
                        findNavController().navigate(actionToProfile)
                    } catch (e: Exception) {
                        Log.e(TAG, "init: ", e)
                    }
                }
                FavoriteType.LOCATION -> {
                    try {
                        val locationId = model.query ?: return@FavoritesAdapter
                        val actionToLocation = FavoritesFragmentDirections.actionToLocation(locationId.toLong())
                        findNavController().navigate(actionToLocation)
                    } catch (e: Exception) {
                        Log.e(TAG, "init: ", e)
                    }
                }
                FavoriteType.HASHTAG -> {
                    try {
                        val hashtag = model.query ?: return@FavoritesAdapter
                        val actionToHashtag = FavoritesFragmentDirections.actionToHashtag(hashtag)
                        findNavController().navigate(actionToHashtag)
                    } catch (e: Exception) {
                        Log.e(TAG, "init: ", e)
                    }
                }
                else -> {
                }
            }
        }, { model: Favorite ->
            // delete
            val context = context ?: return@FavoritesAdapter false
            MaterialAlertDialogBuilder(context)
                .setMessage(getString(R.string.quick_access_confirm_delete, model.query))
                .setPositiveButton(R.string.yes) { d: DialogInterface, _: Int -> favoritesViewModel.delete(model) { d.dismiss() } }
                .setNegativeButton(R.string.no, null)
                .show()
            true
        })
        binding.favoriteList.adapter = adapter
    }
}