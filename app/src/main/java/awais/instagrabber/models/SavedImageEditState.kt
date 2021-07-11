package awais.instagrabber.models

import android.graphics.RectF
import awais.instagrabber.fragments.imageedit.filters.FiltersHelper
import awais.instagrabber.utils.SerializablePair
import java.util.*

data class SavedImageEditState(val sessionId: String, val originalPath: String) {
    var cropImageMatrixValues: FloatArray? = null // 9 values of matrix
    var cropRect: RectF? = null
    var appliedTuningFilters: HashMap<FiltersHelper.FilterType, Map<Int, Any>>? = null
    var appliedFilter: SerializablePair<FiltersHelper.FilterType, Map<Int, Any>>? = null
}