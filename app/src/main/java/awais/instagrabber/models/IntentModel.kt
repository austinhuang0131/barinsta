package awais.instagrabber.models

import awais.instagrabber.models.enums.IntentModelType

data class IntentModel(val type: IntentModelType, val text: String)