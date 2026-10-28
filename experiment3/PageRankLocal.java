import java.io.BufferedReader;
import java.io.FileReader;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;

public class PageRankLocal {
    public static void main(String[] args) throws Exception {
        double d = 0.85;
        int iteration = 10;
        Map<String, List<String>> outLinksMap = new HashMap<>();
        Map<String,Double> prMap = new HashMap<>();

        BufferedReader br = new BufferedReader(new FileReader("/home/king/pagerank-input.txt"));
        String line;
        while((line = br.readLine())!=null){
            String[] parts = line.split("\t");
            String page = parts[0];
            String[] outs = parts[1].split(",");
            List<String> list = new ArrayList<>();
            for(String p : outs){
                list.add(p);
            }
            outLinksMap.put(page,list);
            prMap.put(page,1.0);
        }
        br.close();

        for(int iter = 0; iter < iteration; iter++){
            Map<String,Double> temp = new HashMap<>();
            for(String key : outLinksMap.keySet()){
                temp.put(key,0.0);
            }
            for(Map.Entry<String,List<String>> entry : outLinksMap.entrySet()){
                String page = entry.getKey();
                List<String> targets = entry.getValue();
                double pr = prMap.get(page);
                int size = targets.size();
                double share = pr / size;
                for(String t : targets){
                    temp.put(t, temp.get(t)+share);
                }
            }
            for(String page : temp.keySet()){
                double newPr = (1-d) + d * temp.get(page);
                prMap.put(page,newPr);
            }
        }

        for(Map.Entry<String,Double> e : prMap.entrySet()){
            System.out.printf("(%s,%.10f)%n",e.getKey(),e.getValue());
        }
    }
}

