import javax.xml.ws.spi.http.HttpExchange;

public class Request {

    private int rank;
    private HttpExchange request;
    private String[] params;

    public Request(HttpExchange request, String[] params, int rank) {
        this.request = request;
        this.params = params;
        this.rank = rank;
    }

    public HttpExchange getRequest() {
        return request;
    }

    public void setRequest(HttpExchange request) {
        this.request = request;
    }

    public String[] getParams() {
        return params;
    }

    public void setParams(String[] params) {
        this.params = params;
    }

    public int getRank() {
        return rank;
    }

    public void setRank(int rank) {
        this.rank = rank;
    }
}
