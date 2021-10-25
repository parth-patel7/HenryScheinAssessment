public class ErrorObject {
    int responseCode;
    String errorData;

    public int getResponseCode() {
        return responseCode;
    }

    public void setResponseCode(int responseCode) {
        this.responseCode = responseCode;
    }

    public String getErrorData() {
        return errorData;
    }

    public void setErrorData(String errorData) {
        this.errorData = errorData;
    }

    public ErrorObject(int responseCode, String errorData) {
        this.responseCode = responseCode;
        this.errorData = errorData;
    }
}
