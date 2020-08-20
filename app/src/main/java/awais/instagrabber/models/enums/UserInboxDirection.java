package awais.instagrabber.models.enums;

public enum UserInboxDirection {
    OLDER("older"),
    NEWER("newer");

    private final String value;

    UserInboxDirection(final String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}