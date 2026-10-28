import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;

import java.io.IOException;

public class PageRankMR {
    public static class PRMap extends Mapper<Object, Text, Text, Text>{
        @Override
        protected void map(Object key, Text value, Context context) throws IOException, InterruptedException {
            String line = value.toString();
            String[] parts = line.split("\t");
            String page = parts[0];
            String outLinks = parts[1];
            context.write(new Text(page), new Text("links:"+outLinks));
            String[] links = outLinks.split(",");
            int count = links.length;
            String prStr = context.getConfiguration().get(page);
            double pr = Double.parseDouble(prStr);
            double share = pr / count;
            for(String target : links){
                context.write(new Text(target), new Text("pr:"+share));
            }
        }
    }

    public static class PRReduce extends Reducer<Text,Text,Text,Text>{
        private double d = 0.85;
        @Override
        protected void reduce(Text key, Iterable<Text> values, Context context) throws IOException, InterruptedException {
            String linksStr = "";
            double sum = 0.0;
            for(Text v : values){
                String s = v.toString();
                if(s.startsWith("links:")){
                    linksStr = s.substring(6);
                }else if(s.startsWith("pr:")){
                    sum += Double.parseDouble(s.substring(3));
                }
            }
            double newPr = (1-d) + d * sum;
            context.write(key, new Text(newPr+"\t"+linksStr));
        }
    }

    public static void main(String[] args) throws Exception {
        Configuration conf = new Configuration();
        int iteration = 10;
        String inputPath = "/pagerank/input";
        String outputBase = "/pagerank/output";

        org.apache.hadoop.fs.FileSystem fs = org.apache.hadoop.fs.FileSystem.get(conf);
        Path initInput = new Path(inputPath);
        for(int i=0;i<iteration;i++){
            Path outPath = new Path(outputBase+i);
            if(fs.exists(outPath)){
                fs.delete(outPath,true);
            }
            Job job = Job.getInstance(conf);
            job.setJarByClass(PageRankMR.class);
            job.setMapperClass(PRMap.class);
            job.setReducerClass(PRReduce.class);
            job.setOutputKeyClass(Text.class);
            job.setOutputValueClass(Text.class);

            if(i==0){
                FileInputFormat.addInputPath(job,initInput);
            }else{
                FileInputFormat.addInputPath(job,new Path(outputBase+(i-1)));
            }
            FileOutputFormat.setOutputPath(job,outPath);
            boolean ok = job.waitForCompletion(true);
            if(!ok){
                System.exit(1);
            }

            conf = new Configuration();
            Path resultPath = new Path(outputBase+i+"/part-r-00000");
            org.apache.hadoop.fs.FSDataInputStream in = fs.open(resultPath);
            java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(in));
            String line;
            while((line=br.readLine())!=null){
                String[] arr = line.split("\t");
                String page = arr[0];
                double prVal = Double.parseDouble(arr[1]);
                conf.set(page,String.valueOf(prVal));
            }
            br.close();
            in.close();
        }

        Path finalOut = new Path(outputBase+(iteration-1)+"/part-r-00000");
        org.apache.hadoop.fs.FSDataInputStream fin = fs.open(finalOut);
        java.io.BufferedReader brFin = new java.io.BufferedReader(new java.io.InputStreamReader(fin));
        String line;
        while((line=brFin.readLine())!=null){
            String[] arr = line.split("\t");
            String page = arr[0];
            double pr = Double.parseDouble(arr[1]);
            System.out.printf("(%s,%.10f)%n",page,pr);
        }
        brFin.close();
        fin.close();
    }
}
