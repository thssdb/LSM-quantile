import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.apache.commons.math3.distribution.NormalDistribution;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

// based on KLL Sketch in DataSketch. See https://github.com/apache/datasketches-java/tree/master/src/main/java/org/apache/datasketches/kll
// This is an implementation for long data type.
// works only in heap memory. don't need to serialize or deserialize
public class HeapKLLSketchStrictForBound extends KLLSketchForQuantile {


  public HeapKLLSketchStrictForBound(int maxMemoryByte) { // maxN=7000 for PAGE, 1.6e6 for CHUNK
    N = 0;
    calcParameters(maxMemoryByte);
    calcLevelMaxSize(1);
  }

  private void calcParameters(int maxMemoryByte) {
    maxMemoryNum = calcMaxMemoryNum(maxMemoryByte);
    num = new long[maxMemoryNum];
    level0Sorted = false;
    cntLevel = 0;
  }

  @Override
  protected int calcMaxMemoryNum(int maxMemoryByte) {
    return Math.min(1 << 20, maxMemoryByte / 8);
  }

  @Override
  protected void calcLevelMaxSize(int setLevel) { // set cntLevel.  cntLevel won't decrease
    int[] tmpArr = new int[setLevel + 1];
    int maxPos = cntLevel > 0 ? Math.max(maxMemoryNum, levelPos[cntLevel]) : maxMemoryNum;
    for (int i = 0; i < setLevel + 1; i++) tmpArr[i] = i < cntLevel ? levelPos[i] : maxPos;
    levelPos = tmpArr;
    cntLevel = setLevel;
    levelMaxSize = new int[cntLevel];
    int newK = 0;
    for (int addK = 1 << 28; addK > 0; addK >>>= 1) { // find a new K to fit the memory limit.
      int need = 0;
      for (int i = 0; i < cntLevel; i++)
        need += Math.max(1, (int) Math.round(((newK + addK) * Math.pow(2.0 / 3, cntLevel - i - 1))));
      if (need <= maxMemoryNum) newK += addK;
    }
    for (int i = 0; i < cntLevel; i++)
      levelMaxSize[i] = Math.max(1, (int) Math.round((newK * Math.pow(2.0 / 3, cntLevel - i - 1))));
//    show();
  }

  public String toString() {
    final StringBuilder sb = new StringBuilder();
    sb.append(N);
    sb.append(levelMaxSize[cntLevel - 1]);
    sb.append(cntLevel);
    sb.append((levelPos[cntLevel] - levelPos[0]));
    for (int i = 0; i < cntLevel; i++) sb.append(levelPos[i]);
    return sb.toString();
  }


  public void showLevelMaxSize() {
    int numLEN = levelPos[cntLevel] - levelPos[0];
    System.out.println("\t\t//maxMemNum:" + maxMemoryNum + "\t//N:" + N);
    for (int i = 0; i < cntLevel; i++) System.out.print("\t\t" + levelMaxSize[i] + "\t");
    System.out.println();
    System.out.println("-------------------------------------------------------");
  }

  @Override
  public void update(long x) { // signed long
    if (levelPos[0] == 0) compact();
    num[--levelPos[0]] = x;
    N++;
    level0Sorted = false;
    if (levelPos[1] - levelPos[0] > levelMaxSize[0]) compact();
//    boolean flag=false;
//    for(int i=0;i<cntLevel;i++)if(levelPos[i+1]-levelPos[i]>levelMaxSize[i])flag=true;
//    if(flag)compact();
//    System.out.println("\t\t\t"+x);
  }


  private static int mergeSort(long[] a1, int L1, int R1, long[] a2, int L2, int R2, long[] a3, int pos) {
    if (L1 == R1) System.arraycopy(a2, L2, a3, pos, R2 - L2);
    else if (L2 == R2) System.arraycopy(a1, L1, a3, pos, R1 - L1);
    else {
      int p1 = L1, p2 = L2;
      while (p1 < R1 || p2 < R2)
        if (p1 < R1 && (p2 == R2 || a1[p1] < a2[p2])) a3[pos++] = a1[p1++];
        else a3[pos++] = a2[p2++];
    }
    return R1 - L1 + R2 - L2;
  }

  private void compactOneLevel(int level) { // compact half of data when numToReduce is small

    if (level == cntLevel - 1)
      calcLevelMaxSize(cntLevel + 1);
    int L1 = levelPos[level], R1 = levelPos[level + 1]; // [L,R)
//    System.out.println("T_T\t"+(R1-L1));
    if (level == 0 && !level0Sorted) {
      Arrays.sort(num, L1, R1);
      level0Sorted = true;
    }
    L1 += (R1 - L1) & 1;
    if (L1 == R1) return;
    addRecord(false,num[L1],num[R1-1],level);

    randomlyHalveDownToLeft(L1, R1);

    int mid = (L1 + R1) >>> 1;
    mergeSortWithoutSpace(L1, mid, levelPos[level + 1], levelPos[level + 2]);
    levelPos[level + 1] = mid;
    int newP = levelPos[level + 1] - 1, oldP = L1 - 1;
    for (int i = oldP; i >= levelPos[0]; i--)
      num[newP--] = num[oldP--];

    levelPos[level] = levelPos[level + 1] - (L1 - levelPos[level]);
    int numReduced = (R1 - L1) >>> 1;
    for (int i = level - 1; i >= 0; i--) levelPos[i] += numReduced;
//    if(levelPos[level+1]-levelPos[level]>levelMaxSize[level+1]){
//      compactOneLevel(level+1);
//    }
  }

  @Override
  public void compact() {
    boolean compacted = false;
    for (int i = 0; i < cntLevel; i++)
      if (levelPos[i + 1] - levelPos[i] > levelMaxSize[i]) {
        compactOneLevel(i);
        compacted = true;
      }

    if (!compacted) compactOneLevel(cntLevel - 1);
//    this.showLevelMaxSize();
  }

  public void merge(KLLSketchForQuantile another) {
//    System.out.println("[MERGE]");
//    show();
//    another.show();
    if (another.cntLevel > cntLevel)
      calcLevelMaxSize(another.cntLevel);
    for (int i = 0; i < another.cntLevel; i++) {
      int numToMerge = another.levelPos[i + 1] - another.levelPos[i];
      if (numToMerge == 0) continue;
      int mergingL = another.levelPos[i];
      while (numToMerge > 0) {
//        System.out.println("\t\t"+levelPos[0]);show();showLevelMaxSize();
        if (levelPos[0] == 0) compact();
//        if(levelPos[0]==0){
//          show();
//          showLevelMaxSize();
//        }
        int delta = Math.min(numToMerge, levelPos[0]);
        if (i > 0) { // move to give space for level i
          for (int j = 0; j < i; j++) levelPos[j] -= delta;
          System.arraycopy(num, delta, num, 0, levelPos[i] - delta);
        }
        System.arraycopy(another.num, mergingL, num, levelPos[i] - delta, delta);
        levelPos[i] -= delta;
        numToMerge -= delta;
        mergingL += delta;
      }
    }
    this.N += another.N;
//    System.out.println("[MERGE result]");
//    show();
//    System.out.println();
  }

  public void mergeWithTempSpace(KLLSketchForQuantile another) {
    mergeWithTempSpace(Collections.singletonList(another));
  }

//  public void mergeWithTempSpace(int chunkID,List<KLLSketchForQuantile>otherList){
//
//    for (KLLSketchForQuantile other : otherList) {
//      if (other instanceof KLLSketchDividedForBound) {
//        recordList.add(new compactRecord((KLLSketchDividedForBound) other));
//      }
//    }
//
//  }
//
  public void mergeWithTempSpace(List<KLLSketchForQuantile> otherList) {
//    System.out.println("[MERGE]");
//    show();
//    System.out.println("[mergeWithTempSpace]\t???\t"+num.length+"\t??\t"+cntLevel+"\t??\toldPos0:"+levelPos[0]);
//    System.out.println("[mergeWithTempSpace]\t???\tmaxMemNum:"+maxMemoryNum);
//    another.show();
    int[] oldLevelPos = Arrays.copyOf(levelPos, cntLevel + 1);
    int oldCntLevel = cntLevel;
    int otherNumLen = 0;
    long otherN = 0;
//    System.out.print("\t\t\t\t[mergeWithTempSpace] others:");
    for (KLLSketchForQuantile another : otherList)
      if (another != null) {
//      System.out.print("\t"+another.getN());
        if (another.cntLevel > cntLevel)
          calcLevelMaxSize(another.cntLevel);
        otherNumLen += another.getNumLen();
        otherN += another.getN();
      }
//    System.out.println();
//    System.out.println("[mergeWithTempSpace]\totherNumLen:"+otherNumLen);
    if (getNumLen() + otherNumLen <= maxMemoryNum) {
      int cntPos = oldLevelPos[0] - otherNumLen;
      for (int i = 0; i < cntLevel; i++) {
        levelPos[i] = cntPos;
        if (i < oldCntLevel) {
          System.arraycopy(num, oldLevelPos[i], num, cntPos,
              oldLevelPos[i + 1] - oldLevelPos[i]);
          cntPos += oldLevelPos[i + 1] - oldLevelPos[i];
        }
        for (KLLSketchForQuantile another : otherList)
          if (another != null && i < another.cntLevel) {
            System.arraycopy(another.num, another.levelPos[i], num, cntPos, another.getLevelSize(i));
            cntPos += another.getLevelSize(i);
          }
        Arrays.sort(num, levelPos[i], cntPos);
//        System.out.println("\t\t!!\t"+cntPos);
      }
      levelPos[cntLevel] = cntPos;
      this.N += otherN;
    } else {
      long[] oldNum = num;
      num = new long[getNumLen() + otherNumLen];
//      System.out.println("\t\t\t\ttmp_num:"+num.length+"  old_num:"+levelPos[0]+"..."+levelPos[oldCntLevel]);
      int numLen = 0;
      for (int i = 0; i < cntLevel; i++) {
        levelPos[i] = numLen;
        if (i < oldCntLevel) {
//          System.out.println("\t\t\tlv"+i+"\toldPos:"+oldLevelPos[i]+"\t"+numLen+" this_level_old_len:"+(oldLevelPos[i + 1] - oldLevelPos[i]));
//          System.out.println("\t\t\t"+oldNum[oldLevelPos[i + 1]-1]);
          System.arraycopy(oldNum, oldLevelPos[i], num, numLen,
              oldLevelPos[i + 1] - oldLevelPos[i]);
          numLen += oldLevelPos[i + 1] - oldLevelPos[i];
        }
        for (KLLSketchForQuantile another : otherList)
          if (another != null && i < another.cntLevel) {
            System.arraycopy(another.num, another.levelPos[i], num, numLen, another.getLevelSize(i));
            numLen += another.getLevelSize(i);
          }
        Arrays.sort(num, levelPos[i], numLen);
      }
      levelPos[cntLevel] = numLen;
      this.N += otherN;
//    System.out.println("-------------------------------.............---------");
//      show();System.out.println("\t?\t"+levelPos[0]);
      while (getNumLen() > maxMemoryNum) compact();
//      show();System.out.println("\t?\t"+levelPos[0]);
//    System.out.println("\t\t??\t\t"+Arrays.toString(num));
      int newPos0 = maxMemoryNum - getNumLen();
      System.arraycopy(num, levelPos[0], oldNum, newPos0, getNumLen());
      for (int i = cntLevel; i >= 0; i--) levelPos[i] += newPos0 - levelPos[0];
      num = oldNum;
    }
//    System.out.println("\t\t??\t\t"+Arrays.toString(num));
//    System.out.println("\t\t??\t\t"+Arrays.toString(levelPos));
//    System.out.println("-------------------------------.............---------");
//    System.out.println("[MERGE result]");
//    show();
//    System.out.println();
  }

  static double[][] PR;
  static final int maxTimesForExact = 26;
  ObjectArrayList<compactRecord> recordList = new ObjectArrayList<>();

  //  int compactSummaryLevel;
  private class compactRecord {
    public long L, R;
    public int level;
    public boolean isSummary;

    compactRecord(KLLSketchDividedForBound summary) {
      L = summary.minV;
      R = summary.maxV;
      level = summary.cntLevel;
      isSummary=true;
    }

    compactRecord(boolean isSummary,long minV, long maxV, int level) {
      L = minV;
      R = maxV;
      this.level = level;
      this.isSummary=isSummary;
    }
  }

  // return bound of result with possibility >= p
  public void addRecord(boolean isSum,long minV,long maxV,int level){
    recordList.add(new compactRecord(isSum,minV,maxV,level));
  }

  private double prOutOfBound(int level,int topErr,int bound, int lowLevel,NormalDistribution lowDis,int[] timesInLevel){
    if(level<lowLevel){
//      System.out.println("\t\t\t\t\t\ttopErr="+topErr);
      topErr = Math.abs(topErr);
      return 1.0-(lowDis.cumulativeProbability(bound-topErr)-lowDis.cumulativeProbability(-bound-topErr));
    }
    double prSum=0;
    int cntTime = timesInLevel[level];
    for(int x=-cntTime;x<=cntTime;x++){
      int cntErr = x*(1<<level);
//      System.out.println("\t\t\t\t\t\t\t\t\t\tcntErr:"+cntErr+"\t\tlevel="+level+"\t\tx:"+x+"\t\tcntPr="+PR[cntTime][x+cntTime]);
      prSum+=PR[cntTime][x+cntTime]*prOutOfBound(level-1,topErr+cntErr,bound,lowLevel,lowDis,timesInLevel);
    }
    return prSum;
  }
  private boolean checkBound(int bound, double Pr,int lowLevel,NormalDistribution lowDis,int[] timesInLevel){
    return prOutOfBound(cntLevel-2,0,bound,lowLevel,lowDis,timesInLevel) <= 1-Pr;
  }

  public double queryBound(long result,double Pr) {
    int[] timesInLevel = new int[cntLevel-1];
//    System.out.println("\t!!result For Bound:\t"+result);
    for (compactRecord record:recordList){
//      System.out.println("\t\t\t\t\t"+record.L+"\t"+record.R+"\t\tinside:"+(result>=record.L&&result<record.R?true:false));
      if(result>=record.L&&result<record.R) {
        if(record.isSummary)
          for(int level=0;level<record.level;level++)
            timesInLevel[level]++;
        else
          timesInLevel[record.level]++;
      }
    }
//    System.out.print("\t\t\tcompactionTimesInLevel:\t");
//    for(int i=0;i<cntLevel-1;i++)System.out.print("\t"+timesInLevel[i]);
//    System.out.println();
    if(PR==null)preparePR();
    double sig2 = 0.0;
    int lowLevel=0;
    for(int i=0;i<cntLevel-1;i++)if(timesInLevel[i]>=maxTimesForExact){
      sig2+=timesInLevel[i]/2.0;
      lowLevel = i+1;
    }
    NormalDistribution lowDis = new NormalDistribution(0, Math.sqrt(sig2));
//    System.out.println("\t\t\ts:"+lowDis.cumulativeProbability(-0.5));
    int L=1,R=(int)N,mid;
    while(L<R){
      mid = (L+R)/2;
//      System.out.println("\tcheck: "+L+"..."+R+"\tmid="+mid);
      if(checkBound(mid,Pr,lowLevel,lowDis,timesInLevel)) {
//        System.out.println("\t\t\tSUCCESS:"+mid);
        R = mid;
      }
      else L=mid+1;
    }
    return L;
  }
  private void preparePR() {
    int maxTimes = maxTimesForExact;
    PR = new double[maxTimes+1][maxTimes * 2 + 1];
    PR[1][0] = 0.25;
    PR[1][1] = 0.5;
    PR[1][2] = 0.25;
    for (int m = 2; m <= maxTimes; m++) {
      for (int sum = -m; sum <= m; sum++) {
        if (m - 1 + sum - 1 >= 0) PR[m][m + sum] += 0.25 * PR[m - 1][m - 1 + sum - 1];
        if (m - 1 + sum >= 0) PR[m][m + sum] += 0.5 * PR[m - 1][m - 1 + sum];
        PR[m][m + sum] += 0.25 * PR[m - 1][m - 1 + sum + 1];
      }
//            for(int sum=-m;sum<=m;sum++)System.out.print("\t"+pr[m][m+sum]*Math.pow(2.0,m*2));System.out.println();
//            for(int sum=-m;sum<=m;sum++)System.out.print("\t"+pr[m][m+sum]);System.out.println();
    }
  }



}