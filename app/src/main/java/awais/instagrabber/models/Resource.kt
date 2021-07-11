package awais.instagrabber.models

import androidx.annotation.StringRes

data class Resource<T>(
    @JvmField val status: Status,
    @JvmField val data: T? = null,
    @JvmField val message: String? = null,
    @JvmField @StringRes val resId: Int = 0,
) {
    enum class Status {
        SUCCESS, ERROR, LOADING
    }

    companion object {
        @JvmStatic
        fun <T> success(data: T): Resource<T> {
            return Resource(Status.SUCCESS, data, null, 0)
        }

        @JvmStatic
        fun <T> error(msg: String?, data: T?): Resource<T?> {
            return Resource(Status.ERROR, data, msg, 0)
        }

        @JvmStatic
        fun <T> error(resId: Int, data: T?): Resource<T?> {
            return Resource(Status.ERROR, data, null, resId)
        }

        @JvmStatic
        fun <T> loading(data: T?): Resource<T?> {
            return Resource(Status.LOADING, data, null, 0)
        }
    }
}