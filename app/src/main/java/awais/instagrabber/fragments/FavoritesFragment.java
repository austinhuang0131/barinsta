package awais.instagrabber.fragments;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

import awais.instagrabber.R;
import awais.instagrabber.adapters.FavoritesAdapter;
import awais.instagrabber.asyncs.LocationFetcher;
import awais.instagrabber.asyncs.ProfileFetcher;
import awais.instagrabber.databinding.FragmentFavoritesBinding;
import awais.instagrabber.utils.DataBox;
import awais.instagrabber.utils.TextUtils;
import awais.instagrabber.utils.Utils;
import awais.instagrabber.viewmodels.FavoritesViewModel;

public class FavoritesFragment extends Fragment {
    private static final String TAG = "FavoritesFragment";

    private boolean shouldRefresh = true;
    private FragmentFavoritesBinding binding;
    private RecyclerView root;
    private FavoritesViewModel favoritesViewModel;
    private FavoritesAdapter adapter;

    @NonNull
    @Override
    public View onCreateView(@NonNull final LayoutInflater inflater, @Nullable final ViewGroup container, @Nullable final Bundle savedInstanceState) {
        if (root != null) {
            shouldRefresh = false;
            return root;
        }
        binding = FragmentFavoritesBinding.inflate(getLayoutInflater());
        root = binding.getRoot();
        binding.favoriteList.setLayoutManager(new LinearLayoutManager(getContext()));
        return root;
    }

    @Override
    public void onViewCreated(@NonNull final View view, @Nullable final Bundle savedInstanceState) {
        if (!shouldRefresh) return;
        init();
        shouldRefresh = false;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (favoritesViewModel == null || adapter == null) return;
        // refresh list every time in onViewStateRestored since it is cheaper than implementing pull down to refresh
        favoritesViewModel.getList().observe(getViewLifecycleOwner(), adapter::submitList);
        final List<DataBox.FavoriteModel> allFavorites = Utils.dataBox.getAllFavorites();
        favoritesViewModel.getList().postValue(allFavorites);
        fetchMissingInfo(allFavorites);
    }

    private void init() {
        favoritesViewModel = new ViewModelProvider(this).get(FavoritesViewModel.class);
        adapter = new FavoritesAdapter(model -> {
            // navigate
            switch (model.getType()) {
                case USER: {
                    final String username = model.getQuery();
                    // Log.d(TAG, "username: " + username);
                    final NavController navController = NavHostFragment.findNavController(this);
                    final Bundle bundle = new Bundle();
                    bundle.putString("username", "@" + username);
                    navController.navigate(R.id.action_global_profileFragment, bundle);
                    break;
                }
                case LOCATION: {
                    final String locationId = model.getQuery();
                    // Log.d(TAG, "locationId: " + locationId);
                    final NavController navController = NavHostFragment.findNavController(this);
                    final Bundle bundle = new Bundle();
                    bundle.putString("locationId", locationId);
                    navController.navigate(R.id.action_global_locationFragment, bundle);
                    break;
                }
                case HASHTAG: {
                    final String hashtag = model.getQuery();
                    // Log.d(TAG, "hashtag: " + hashtag);
                    final NavController navController = NavHostFragment.findNavController(this);
                    final Bundle bundle = new Bundle();
                    bundle.putString("hashtag", "#" + hashtag);
                    navController.navigate(R.id.action_global_hashTagFragment, bundle);
                    break;
                }
                default:
                    // do nothing
            }
        }, model -> {
            // delete
            final Context context = getContext();
            if (context == null) return false;
            new MaterialAlertDialogBuilder(context)
                    .setMessage(getString(R.string.quick_access_confirm_delete, model.getQuery()))
                    .setPositiveButton(R.string.yes, (d, which) -> {
                        Utils.dataBox.deleteFavorite(model.getQuery(), model.getType());
                        d.dismiss();
                        favoritesViewModel.getList().postValue(Utils.dataBox.getAllFavorites());
                    })
                    .setNegativeButton(R.string.no, null)
                    .show();
            return true;
        });
        binding.favoriteList.setAdapter(adapter);
        // favoritesViewModel.getList().observe(getViewLifecycleOwner(), adapter::submitList);

    }

    private void fetchMissingInfo(final List<DataBox.FavoriteModel> allFavorites) {
        final Runnable runnable = () -> {
            final List<DataBox.FavoriteModel> updatedList = new ArrayList<>(allFavorites);
            // cyclic barrier is to make the async calls synchronous
            final CyclicBarrier cyclicBarrier = new CyclicBarrier(2, () -> {
                // Log.d(TAG, "fetchMissingInfo: barrier action");
                favoritesViewModel.getList().postValue(new ArrayList<>(updatedList));
            });
            try {
                for (final DataBox.FavoriteModel model : allFavorites) {
                    cyclicBarrier.reset();
                    // if the model has missing pic or display name (for user and location), fetch those details
                    switch (model.getType()) {
                        case LOCATION:
                            if (TextUtils.isEmpty(model.getDisplayName())
                                    || TextUtils.isEmpty(model.getPicUrl())) {
                                new LocationFetcher(model.getQuery(), result -> {
                                    try {
                                        if (result == null) return;
                                        final int i = updatedList.indexOf(model);
                                        updatedList.remove(i);
                                        final DataBox.FavoriteModel updated = new DataBox.FavoriteModel(
                                                model.getId(),
                                                model.getQuery(),
                                                model.getType(),
                                                result.getName(),
                                                result.getSdProfilePic(),
                                                model.getDateAdded()
                                        );
                                        Utils.dataBox.addOrUpdateFavorite(updated);
                                        updatedList.add(i, updated);
                                    } finally {
                                        try {
                                            cyclicBarrier.await();
                                        } catch (BrokenBarrierException | InterruptedException e) {
                                            Log.e(TAG, "fetchMissingInfo: ", e);
                                        }
                                    }
                                }).execute();
                                cyclicBarrier.await();
                            }
                            break;
                        case USER:
                            if (TextUtils.isEmpty(model.getDisplayName())
                                    || TextUtils.isEmpty(model.getPicUrl())) {
                                new ProfileFetcher(model.getQuery(), result -> {
                                    try {
                                        if (result == null) return;
                                        final int i = updatedList.indexOf(model);
                                        updatedList.remove(i);
                                        final DataBox.FavoriteModel updated = new DataBox.FavoriteModel(
                                                model.getId(),
                                                model.getQuery(),
                                                model.getType(),
                                                result.getName(),
                                                result.getSdProfilePic(),
                                                model.getDateAdded()
                                        );
                                        Utils.dataBox.addOrUpdateFavorite(updated);
                                        updatedList.add(i, updated);
                                    } finally {
                                        try {
                                            cyclicBarrier.await();
                                        } catch (BrokenBarrierException | InterruptedException e) {
                                            Log.e(TAG, "fetchMissingInfo: ", e);
                                        }
                                    }
                                }).execute();
                                cyclicBarrier.await();
                            }
                            break;
                        case HASHTAG:
                        default:
                            // hashtags don't require displayName or pic
                            // updatedList.add(model);
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "fetchMissingInfo: ", e);
            }
            favoritesViewModel.getList().postValue(updatedList);
        };
        new Thread(runnable).start();
    }
}
