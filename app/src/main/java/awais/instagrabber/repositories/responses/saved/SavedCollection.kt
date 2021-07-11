package awais.instagrabber.repositories.responses.saved

import awais.instagrabber.repositories.responses.Media
import java.io.Serializable

class SavedCollection(val collectionId: String,
                      val collectionName: String,
                      val collectionType: String,
                      val collectionMediaCount: Int,
                      // coverMedia or coverMediaList: only one is defined
                      val coverMedia: Media,
                      val coverMediaList: List<Media>) : Serializable