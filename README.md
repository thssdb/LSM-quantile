# LSM-quantile

Branch in Apache IoTDB: https://github.com/apache/iotdb/tree/research/LSM-quantile

Three open dataset from Kaggle used in the evaluation experiments . https://cloud.tsinghua.edu.cn/d/621db264c8f04af6904e/



Proof : in full.pdf



### In-database test:

​		Time cost in Fig 8, 9.

​		**Part 1 Get IoTDB with quantile:**

​			Clone https://github.com/apache/iotdb/tree/research/LSM-quantile , then compile IoTDB server and client by 

				mvn spotless:apply
				mvn clean package -pl cli -am -Dmaven.test.skip=trueD:
​		**Part 2 Run and Ingest Data Set:**

​			Run server by 

				.\server\target\iotdb-server-0.13.0-SNAPSHOT\sbin\start-server.bat
​				or

```
			.\server\target\iotdb-server-0.13.0-SNAPSHOT\sbin\start-server.sh
```

​			Download the three open datasets from the link above, and ingest them by running SessionSyn.py:

```
		iotdb\client-py\SessionSyn.py is used for inserting the data into IoTDB. 
		By alternating the data file name in the read_csv function (line 61), different dataset can be inserted. 
		What should be noticed is that the name of the storage group should be modified for different datasets to forbid conflict.
```

​		**Part 3 TEST**

​				Test Code is in folder iotdb\client-py. SessionQuery.py, SessionMemory.py and SessionNo.py.

			SessionQuery.py is used for testing the query time of LSM-KLL, of different data sizes. To run the python file, corresponding content in .conf file should be modified.
			SessionMemory.py is used for testing the query time of LSM-KLL, of different query chunk memory. To run the python file, corresponding content in .conf file should be modified. Remember to delete the "where time<=2000000" in query statements if you want to query on the whole time-series with 5E7 data.
			SessionNo.py is used for testing the query time of KLL, T-Digest and Random Sampling, of different data sizes and other settings.

​		**Varying parameters in IoTDB configuration file:**

​				Paramters in iotdb/server/target/iotdb-server-0.13.0-SNAPSHOT/conf/iotdb-engine.properties should be changed. Remember to <u>restart</u> server after modifying conf file.

​				To vary query sketch size (query memory limit), "aggregator_memory_in_kb" in line 26 should be changed.

​				To vary chunk sketch size, "synopsis_size_in_byte" in line 25 should be changed.

​				To vary quantile to query, "quantile" in line 28 should be changed.





### Out-of-database test in paper:

​		Error rate in Fig 8, 9; Time cost and Error rate in Fig 10, 11

​		The test code is in src/main/java.

​		Please download the 3 dataset and store in the root directory of this project.

​		**TEST for Fig 8, 9:**

```
	IntervalEvaluatingMergingKLLSketch.java,  IntervalEvaluatingMergingTDigest.java,	IntervalEvaluatingMergingSampling.java are used to test the query time of LSM-KLL, KLL, t-digest and random sampling.	
	You can change the parameters in code to vary what you want. For example, in line 235,236,237 of IntervalEvaluatingMergingKLLSketch.java, you can change the values to vary query data size, query memory and chunk sketch size.
```

​		**TEST for Fig 9, 10:**

```
	EvaluatingOptimalChunkSketch.java is used for computing the error rate in Fig 9.
ComparingBuildingOptimalAndOriginalSketch.java will show the construction time, query time and error rate of optimal chunk sketch and original chunk sketch.
```

​		Here is a screen shot of out-of-database test. Result of all 4 datasets are shown(4 columns). Only 3 datasets are open.![alt](.\test_sample_screenshot_2.png)
