import com.amazonaws.services.ec2.model.Instance;
import java.util.ArrayList;

public class InstanceEntity {

    private Instance instance;
    private ArrayList<Request> currentRequests = new ArrayList<Request>();
    private int rank;

    public InstanceEntity(Instance instance) {
        this.instance = instance;
    }

    public Instance getInstance() {
        return instance;
    }

    public void setInstance(Instance instance) {
        this.instance = instance;
    }

    public ArrayList<Request> getCurrentRequests() {
        return currentRequests;
    }

    public void addRequest(Request request) {
        if(request.getRank() < 7) {
            rank += 1;
        } else if(request.getRank() < 11) {
            rank += 2;
        } else {
            rank += 3;
        }

        currentRequests.add(request);
    }

    public void removeRequest(Request request) {
        if (request.getRank() < 7) {
            rank -= 1;
        } else if (request.getRank() < 11) {
            rank -= 2;
        } else {
            rank -= 3;
        }

        currentRequests.remove(request);
    }

    public int getRank() {
        return rank;
    }


    // TODO cpu...


}
