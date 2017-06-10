package Manager;

import java.util.List;


public class WorkerHandle {

    private boolean occupied = false;
    private Task task;

    public WorkerHandle(){

    }

    public void setTask(Task t){
        occupied = true;
        task = t;
    }

    public Task getTask(){
        return task;
    }

    public boolean isOccupied(){
        return occupied;
    }
}
