package awais.instagrabber.repositories.responses.saved

class CollectionsListResponse     //        this.numResults = numResults;
(val isMoreAvailable: Boolean,
 val nextMaxId: String,
 val maxId: String,
 val status: String,  //                                   final int numResults,
        //    public int getNumResults() {
        //        return numResults;
        //    }
        //    private final int numResults;
 val items: List<SavedCollection>)