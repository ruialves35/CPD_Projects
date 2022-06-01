package common;

public enum MessageTypes {
    REQUEST("REQ"),
    REPLY("REP"),
    JOIN("join"),
    LEAVE("leave"),
    GET("get"),
    PUT("put"),
    DELETE("delete"),
    SAVE_FILE("saveFile"),
    GET_AND_DELETE("getAndDelete"),
    GET_FILES("getFiles"),
    ERROR("error"),
    OK("ok"),
    ELECTION_REQUEST("electionRequest");

    private String code;

    MessageTypes(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
