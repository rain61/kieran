package kieran.uitls;



import kieran.model.TaskInfo;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class KieranUtils {


    public static List<LinkedList<TaskInfo>> getTimeLong(Integer index){
        ArrayList<LinkedList<TaskInfo>> objects = new ArrayList<LinkedList<TaskInfo>>();

        for (Integer i = 0; i < index; i++) {
            objects.add(new LinkedList<TaskInfo>());
        }
        return objects;
    }



}
