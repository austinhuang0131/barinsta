package awais.instagrabber.dialogs

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import awais.instagrabber.R
import awais.instagrabber.utils.*
import awais.instagrabber.utils.extensions.TAG
import awais.instagrabber.webservices.GraphQLRepository
import awais.instagrabber.webservices.MediaRepository
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*

class PostLoadingDialogFragment : DialogFragment() {
    private var isLoggedIn: Boolean = false

    private val mediaRepository: MediaRepository by lazy { MediaRepository.getInstance() }
    private val graphQLRepository: GraphQLRepository by lazy { GraphQLRepository.getInstance() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val cookie = Utils.settingsHelper.getString(Constants.COOKIE)
        var userId: Long = 0
        var csrfToken: String? = null
        if (cookie.isNotBlank()) {
            userId = getUserIdFromCookie(cookie)
            csrfToken = getCsrfTokenFromCookie(cookie)
        }
        if (cookie.isBlank() || userId == 0L || csrfToken.isNullOrBlank()) {
            isLoggedIn = false
            return
        }
        isLoggedIn = true
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return MaterialAlertDialogBuilder(requireContext())
            .setCancelable(false)
            .setView(R.layout.dialog_opening_post)
            .create()
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        val arguments = PostLoadingDialogFragmentArgs.fromBundle(arguments ?: return)
        val shortCode = arguments.shortCode
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val media = if (isLoggedIn) mediaRepository.fetch(TextUtils.shortcodeToId(shortCode)) else graphQLRepository.fetchPost(shortCode)
                withContext(Dispatchers.Main) {
                    if (media == null) {
                        Toast.makeText(context, R.string.post_not_found, Toast.LENGTH_SHORT).show()
                        return@withContext
                    }
                    try {
                        findNavController().navigate(PostLoadingDialogFragmentDirections.actionToPost(media, 0))
                    } catch (e: Exception) {
                        Log.e(TAG, "showPostView: ", e)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "showPostView: ", e)
            } finally {
                withContext(Dispatchers.Main) {
                    dismiss()
                }
            }
        }
    }
}