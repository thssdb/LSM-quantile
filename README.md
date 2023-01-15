# LSM-quantile

Branch in Apache IoTDB: https://github.com/apache/iotdb/tree/research/LSM-quantile

Three open datasets from Kaggle used in the evaluation experiments . 

	
	Bitcoin:	https://userscloud.com/txccw5xusv97
	Thruster:	https://userscloud.com/6x01axzl1vii
	Taxi:		https://userscloud.com/qmd27og0m1ix





### In-database test:

​		Time cost in Fig 10,11,14, 15 and 16.

​		**Part 1	Get IoTDB with quantile:**

​			Clone https://github.com/apache/iotdb/tree/research/LSM-quantile , then compile IoTDB server and client by 

				mvn spotless:apply
				mvn clean package -pl cli -am -Dmaven.test.skip=trueD:
​		**Part 2	Run IoTDB Server :**

​			Due to many parameters in LSM-KLL, we need to check the configuration file iotdb\server\target\iotdb-server-0.13.0-SNAPSHOT\conf\iotdb-engine.properties 

​			The main parameters needed to be confirmed are as follows:

| name in configuration file | corresponding parameter in LSM-KLL                  |
| -------------------------- | --------------------------------------------------- |
| sketch_size_ratio          | sketch size ratio. $\mathit{T}_s$                   |
| enable_synopsis            | Whether to collect summaries ( chunk sketches ) .   |
| enable_SST_sketch          | Whether to compute hierarchical SSTable sketches.   |
| aggregator_memory_in_kb    | The query memory limit, i.e. the query sketch size. |
| quantile                   | The queried quantile.                               |

​			Then we can run server by 

				.\server\target\iotdb-server-0.13.0-SNAPSHOT\sbin\start-server.bat
​				or

```
			.\server\target\iotdb-server-0.13.0-SNAPSHOT\sbin\start-server.sh
```

​			It is required to <u>restart</u> server for any new configuration to take effect.



​		**Part 3	Ingest Data Set**

​				Download the three open datasets from the link above, and ingest them by running SessionSyn.py:

​				To ingest data with sequential timestamps (for Fig. 10...15), it's recommended to execute iotdb\session\src\test\java\org\apache\iotdb\session\InsertCsvDataIT.java

​				To ingest data with log-normal latency for (Fig. 16), it's recommended to execute iotdb\session\src\test\java\org\apache\iotdb\session\InsertUnseqLatencyDataIT.java

​				Please look into the java code to determine the path of dataset and its schema in IoTDB.





​		**Part 4 TEST**

​				LSM-KLL and baselines are different aggregation functions deployed in IoTDB.

​				The aggregate function kll_quantile is LSM-KLL or KLL, depending on whether there are pre-computed sketches in data files.

​				The function ddsketch_quantile and tdigest_quantile are for the other two baselines.

​				One can check all aggregate functions or all timeseries by executing "show functions" or "show timeseries" in iotdb client.



​				A. Test in Python.

​					Test Code is in folder iotdb\client-py. SessionQuery.py, SessionMemory.py and SessionNo.py.

			SessionQuery.py is used for testing the query time of LSM-KLL, of different data sizes. To run the python file, corresponding content in .conf file should be modified.
			SessionMemory.py is used for testing the query time of LSM-KLL, of different query chunk memory. To run the python file, corresponding content in .conf file should be modified. Remember to delete the "where time<=2000000" in query statements if you want to query on the whole time-series with more than 5E7 data.
			SessionNo.py is used for testing the query time of different methods, data sizes and other settings.

​				B. Test in Java.

​					For example, one can modify and run iotdb\session\src\test\java\org\apache\iotdb\session\QuerySSTSketchWithDifferentTs.java to get the time cost result in Fig. 14



### Out-of-database test in paper:

​		Error rate in Fig 10, 11, 14, 16 ; Time cost and Error rate in Fig 12, 13.

​		The test code is in src/main/java.

​		Please download the 3 dataset and store in the root directory of this project.

​		**Error rate for Fig 10, 11, 14:**

```
	IntervalEvaluatingSSTKLL.java,  IntervalEvaluatingTDigest.java,	IntervalEvaluatingDDSketch.java are used to test the query time of LSM-KLL, KLL, t-digest and random sampling.	
	You can change the parameters in code to vary what you want. For example, in line 369...377 of IntervalEvaluatingSSTKLL.java, you can change the values of parameters to vary dataset, query data size, query memory ,chunk sketch size and sketch size ratio. What's more, you can change data set size and number of test cases in line 16,17.
```

​		**TEST for Fig 12, 13:**

```
	EvaluatingOptimalChunkSketch for the error in Fig 12.
	ComparingBuildingOptimalAndOriginalSketch.java for other result.
```

​		**Error rate for Fig 16:**

```
	IntervalEvaluatingLatencySSTKLL.java,  IntervalEvaluatingLatencyTDigest.java,	IntervalEvaluatingLatencyDDSketch.java.
```





