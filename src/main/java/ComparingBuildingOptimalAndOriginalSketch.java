import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Random;

public class ComparingBuildingOptimalAndOriginalSketch {
    int dataType;
    static int N = 50000000; // CHECK IT
    static int pageN = 1<<12, pageNum=N/pageN;
    public static int TEST_CASE=4; // CHECK IT
    static double[] a;
    static KLLSketchForQuantile[] OptimalArr;
    static HeapLongStrictKLLSketch[] OriginalArr;
    static ArrayList<String> err_result = new ArrayList<>();
    static ArrayList<String> time_result = new ArrayList<>();
    static ArrayList<String> build_time_result = new ArrayList<>();
    boolean show_time = false,show_err=false;
    int RESULT_LINE=0;

    Random random = new Random(233);

    public void prepareA(int dataType) throws IOException {
        if(a==null)a = new double[N];
        this.dataType = dataType;

        if(dataType==0){
            for(int i=0;i<N;i++)a[i]=Math.pow(-1,random.nextInt(2))*Math.pow(10.0,(2*Math.pow(random.nextDouble(),2)-1)*300);
        }
        if(dataType==1){
            BufferedReader reader = new BufferedReader(new FileReader(new File("1_bitcoin.csv")));
            reader.readLine(); // ignore first line.
            String line;
            int cntN= 0;
            while((line=reader.readLine())!=null){
                a[cntN++] = Double.parseDouble(line);
                if(cntN==N)return;
            }
        }
        if(dataType==2){
            BufferedReader reader = new BufferedReader(new FileReader(new File("2_physiological_stress.txt")));
            reader.readLine(); // ignore first line.
            String line;
            int cntN= 0;
            while((line=reader.readLine())!=null){
                a[cntN++] = Double.parseDouble(line);
                if(cntN==N)return;
            }
        }
        if(dataType==3){
            BufferedReader reader = new BufferedReader(new FileReader(new File("4_taxipredition8M.txt")));
            reader.readLine(); // ignore first line.
            String line;
            int cntN= 0;
            while((line=reader.readLine())!=null){
                a[cntN++] = Double.parseDouble(line);
                if(cntN==N)return;
            }
        }
        if(dataType==4){
            BufferedReader reader = new BufferedReader(new FileReader(new File("5_wh.csv")));
            reader.readLine(); // ignore first line.
            String line;
            int cntN= 0;
            while((line=reader.readLine())!=null){
                a[cntN++] = Double.parseDouble(line);
                if(cntN==N)return;
            }
        }
    }
    public void prepareWorker(int maxSeriesByte){
        long ST;
        OptimalArr = new KLLSketchForQuantile[pageNum];
        OriginalArr = new HeapLongStrictKLLSketch[pageNum];

        ST = new Date().getTime();
        int enoughMemByte = pageN*10;
        for(int i=0;i<pageNum;i++) {
            LongKLLSketch worker = new LongKLLSketch(pageN, enoughMemByte, maxSeriesByte);
            for (int j = 0; j<pageN; j++) worker.update(dataToLong(a[i*pageN+j]));
            worker.compactBeforeSerialization();
            OptimalArr[i] = worker;
//            if(i==0)worker.show();
        }
        if(show_time) {
//            System.out.print("\t\t"+(new Date().getTime()-ST));
            build_time_result.set(RESULT_LINE, build_time_result.get(RESULT_LINE).concat("\t\t\t" + (new Date().getTime()-ST)));
        }

        ST = new Date().getTime();
        for(int i=0;i<pageNum;i++) {
            HeapLongStrictKLLSketch original_worker = new HeapLongStrictKLLSketch(maxSeriesByte);
            for (int j = 0; j<pageN; j++) original_worker.update(dataToLong(a[i*pageN+j]));
            OriginalArr[i] = original_worker;
//            if(i==0)original_worker.show();
        }
        if(show_time) {
//            System.out.print("\t" + (new Date().getTime() - ST) + "\n");
            build_time_result.set(RESULT_LINE, build_time_result.get(RESULT_LINE).concat("\t" + (new Date().getTime()-ST)));
        }
    }
    public int getValueActualRank(double[]sortedA,int queryN, double v){ // number of elements <= v
        int L=0,R=queryN-1;
        while(L<R){
            int mid=(L+R+1)>>>1;
            if(v<sortedA[mid])R=mid-1;
            else L=mid;
        }
        return L;
    }


    private long dataToLong(double data)  {
        long result = Double.doubleToLongBits((double) data);
        return data >= 0d ? result : result ^ Long.MAX_VALUE;
    }

    private double longToResult(long result)  {
        result = (result >>> 63) == 0 ? result : result ^ Long.MAX_VALUE;
        return Double.longBitsToDouble(result);
    }

    public void testMergeError(int L,int R, int maxMemoryByte, int maxSeriesByte) throws IOException {
//        System.out.println("\tdata/mem:"+queryPageNum*pageN*8/maxMemoryByte+
//            "\tpageData/pageKLL:"+pageN*8/maxSeriesByte);
        DecimalFormat fnum = new DecimalFormat("#0.00");
        long original_time=0, optimal_time=0;
        double err_original=0,err_optimal=0;
        int queryN = R-L;
        double[] query_a = new double[queryN];

        for(int T=0;T<TEST_CASE;T++){
            int pageL = (L+pageN-1)/pageN, pageR = R/pageN;
            int posL = pageL*pageN, posR = pageR*pageN;
//            System.out.println("\t\t\t"+posL+"\t"+posR);

            optimal_time-=new Date().getTime();
            int buf_kll_num = 1;
            HeapLongStrictKLLSketch optimal_worker = new HeapLongStrictKLLSketch(maxMemoryByte);
            for(int i=L;i<Math.min(R,posL);i++)
                optimal_worker.update(dataToLong(a[i]));
            for(int i=pageL;i<pageR;i++)
                optimal_worker.mergeWithTempSpace(OptimalArr[i]);
            for(int i=Math.max(L,posR);i<R;i++)
                optimal_worker.update(dataToLong(a[i]));
            optimal_time+=new Date().getTime();



            original_time-=new Date().getTime();
            HeapLongStrictKLLSketch original_worker = new HeapLongStrictKLLSketch(maxMemoryByte);
            for(int i=L;i<Math.min(R,posL);i++)
                original_worker.update(dataToLong(a[i]));
            for(int i=pageL;i<pageR;i++)
                original_worker.mergeWithTempSpace(OriginalArr[i]);
            for(int i=Math.max(L,posR);i<R;i++)
                original_worker.update(dataToLong(a[i]));
            original_time+=new Date().getTime();

            if (R - L >= 0) System.arraycopy(a, L, query_a, 0, R-L);
            Arrays.sort(query_a);

            double q_start=0.01,q_end=0.99,q_add=0.005,q_count = Math.floor((q_end-q_start-1e-10)/q_add)+1;
            for(double q=q_start;q<q_end+1e-10;q+=q_add){
                int query_rank = (int)(q*queryN);

                double optimal_v = longToResult(optimal_worker.findMinValueWithRank(query_rank));
                int optimal_delta_rank = getValueActualRank(query_a,queryN,optimal_v)-query_rank;
                double optimal_relative_err = 1.0*optimal_delta_rank/(queryN);
                err_optimal+=Math.abs(optimal_relative_err)/(q_count*TEST_CASE);

                double original_v = longToResult(original_worker.findMinValueWithRank(query_rank));
                int original_delta_rank = getValueActualRank(query_a,queryN,original_v)-query_rank;
                double original_relative_err = 1.0*original_delta_rank/(queryN);
                err_original+=Math.abs(original_relative_err)/(q_count*TEST_CASE);

//                System.out.println("?\t\toriginal:"+original_v+" delta:"+original_delta_rank+"\t\toptimal:"+optimal_v+" delta:"+optimal_delta_rank);
            }
        }
        if(show_err) {
            System.out.print("\t\t\t" + err_optimal + "\t" + err_original);
        }
        err_result.set(RESULT_LINE,err_result.get(RESULT_LINE).concat("\t\t\t"+err_optimal+"\t"+err_original));
        if(TEST_CASE!=0) {
//            if(show_time) {
//                System.out.print("\t" + optimal_time / TEST_CASE);
//                System.out.print("\t" + original_time / TEST_CASE);
//                System.out.println();
//            }
            time_result.set(RESULT_LINE, time_result.get(RESULT_LINE).concat("\t\t\t" + optimal_time / TEST_CASE+"\t"+original_time / TEST_CASE));
        }
        //        System.out.println("\t\t\tmerge-point"+"\t"+queryN*(err_optimalBuf-err_original)+"\t"+queryN*(err_optimal-err_original));

    }
    public void show_time_result(){
        System.out.println(time_result);
    }
    public static void setTestCase(int tc){TEST_CASE=tc;}




    public static void main(String[] args) throws IOException{
        ComparingBuildingOptimalAndOriginalSketch main;
//        main = new MainForMergeStatErrorKLL();
//        main.prepareA();
//        int tmp_seri = 1<<9,tmp_mem=1<<15;
//        main.prepareKLL(tmp_seri);
////        for(int num=1;num<=8;num++)
////            main.testKLL(num,tmp_mem,tmp_seri);
//        for(int num=1;num<=pageNum;num*=2)
//            main.testKLL(num,tmp_mem,tmp_seri);
//        main.show_time_result();

//        System.out.println("interval query"+"\n");
        for (int dataType = 1; dataType <= /*4*/3; dataType++){ // CHECK IT
            main = new ComparingBuildingOptimalAndOriginalSketch();
            main.prepareA(dataType);
            for(int i : new int[]{/*10000000,20000000,30000000,40000000,*/50000000})
                for(int query_mem : new int[]{/*1024*16,1024*32,1024*64,*/1024*128/*,1024*256*//*,1024*512,1024*1024*/}) {
                    main.show_time = false;
                    for (int chunk_seri : new int[]{128, 256, 512, 1024/*,128,256,512,1024/*,2048,4096,8192*/})
                        main.prepareWorker(chunk_seri); // for time test.
                    main.show_time = true;
                    for (int chunk_seri : new int[]{128, 256, 512, 1024/*,128,256,512,1024/*,2048,4096,8192*/}) {
                        if (dataType == 1) {
                            err_result.add("N:" + i + ", " + "M:" + query_mem + ", " + "|M_c|:" + chunk_seri + "\t");
                            time_result.add("N:" + i + ", " + "M:" + query_mem + ", " + "|M_c|:" + chunk_seri + "\t");
                            build_time_result.add("N:" + i + ", " + "M:" + query_mem + ", " + "|M_c|:" + chunk_seri + "\t");
                        }
                        main.prepareWorker(chunk_seri);
                        main.testMergeError(N - i, N, query_mem, chunk_seri); // CHECK IT
//                        System.out.println("");
                        main.RESULT_LINE++;
//                        main.show_time_result();
                    }
                }
            }
        System.out.println("Comparing Chunk Sketch: optimal & original\nTEST_CASE="+TEST_CASE);
        System.out.println("\nTime cost for construction:");
        for(String s:build_time_result)
            System.out.println(s);
        System.out.println("\nError Rate of Merge Result:");
        for(String s:err_result)
            System.out.println(s);
        System.out.println("\nTime cost For Merging pre-computed sketches:");
        for(String s:time_result)
            System.out.println(s);
    }
}
