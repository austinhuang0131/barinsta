package awais.instagrabber.viewmodels;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;

import awais.instagrabber.db.datasources.AccountDataSource;
import awais.instagrabber.db.repositories.AccountRepository;
import awais.instagrabber.repositories.responses.User;
import awais.instagrabber.utils.Constants;
import awais.instagrabber.utils.CookieUtils;
import awais.instagrabber.utils.TextUtils;
import awais.instagrabber.webservices.ServiceCallback;
import awais.instagrabber.webservices.UserService;

import static awais.instagrabber.utils.Utils.settingsHelper;

public class AppStateViewModel extends AndroidViewModel {
    private static final String TAG = AppStateViewModel.class.getSimpleName();

    private final String cookie;
    private final boolean isLoggedIn;

    private User currentUser;
    private AccountRepository accountRepository;
    private UserService userService;

    public AppStateViewModel(@NonNull final Application application) {
        super(application);
        // Log.d(TAG, "AppStateViewModel: constructor");
        cookie = settingsHelper.getString(Constants.COOKIE);
        isLoggedIn = !TextUtils.isEmpty(cookie) && CookieUtils.getUserIdFromCookie(cookie) > 0;
        if (!isLoggedIn) return;
        userService = UserService.getInstance();
        accountRepository = AccountRepository.getInstance(AccountDataSource.getInstance(application));
        fetchProfileDetails();
    }

    public User getCurrentUser() {
        return currentUser;
    }

    private void fetchProfileDetails() {
        final long uid = CookieUtils.getUserIdFromCookie(cookie);
        userService.getUserInfo(uid, new ServiceCallback<User>() {
            @Override
            public void onSuccess(final User user) {
                currentUser = user;
            }

            @Override
            public void onFailure(final Throwable t) {}
        });
    }
}
