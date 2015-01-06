package net.stubomatic;

public enum Protocol {
    HTTP("http", false),
    HTTPS("https", true);

    private final String displayName;
    private final boolean isSSL;
    Protocol(String displayName, boolean isSSL) {
        this.displayName = displayName;
        this.isSSL = isSSL;
    }

    @Override
    public String toString() { return displayName; }

    public boolean isSSL() { return isSSL; }
}
