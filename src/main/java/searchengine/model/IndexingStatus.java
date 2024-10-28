package searchengine.model;

public enum IndexingStatus {
    INDEXING, INDEXED, FAILED;

    public String toString(){
        return switch (this) {
            case INDEXING -> "INDEXING";
            case INDEXED -> "INDEXED";
            case FAILED -> "FAILED";
        };
    }
}
