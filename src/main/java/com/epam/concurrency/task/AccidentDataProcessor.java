package com.epam.concurrency.task;

import com.epam.data.RoadAccident;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

/**
 * Created by Tanmoy on 6/17/2016.
 */
public class AccidentDataProcessor {

    private static final String FILE_PATH_1 = "src/main/resources/DfTRoadSafety_Accidents_2010.csv";
    private static final String FILE_PATH_2 = "src/main/resources/DfTRoadSafety_Accidents_2011.csv";
    private static final String FILE_PATH_3 = "src/main/resources/DfTRoadSafety_Accidents_2012.csv";
    private static final String FILE_PATH_4 = "src/main/resources/DfTRoadSafety_Accidents_2013.csv";

    private static final String OUTPUT_FILE_PATH = "target/DfTRoadSafety_Accidents_consolidated.csv";

    private static final int DATA_PROCESSING_BATCH_SIZE = 10000;

    private AccidentDataReader accidentDataReader = new AccidentDataReader();
    private AccidentDataEnricher accidentDataEnricher = new AccidentDataEnricher();
    private AccidentDataWriter accidentDataWriter = new AccidentDataWriter();

    private List<String> fileQueue = new ArrayList<String>();

    private Logger log = LoggerFactory.getLogger(AccidentDataProcessor.class);

    BlockingQueue<List<RoadAccident>> roadAccidentQ = new ArrayBlockingQueue<>(3);
    BlockingQueue<List<RoadAccidentDetails>> roadAccidentDetailsQ = new ArrayBlockingQueue<>(3);
    private volatile boolean processing = true;

    public void init(){
        fileQueue.add(FILE_PATH_1);
        fileQueue.add(FILE_PATH_2);
        fileQueue.add(FILE_PATH_3);
        fileQueue.add(FILE_PATH_4);

        accidentDataWriter.init(OUTPUT_FILE_PATH);
    }

    public void process(){
        for (String accidentDataFile : fileQueue){
            log.info("Starting to process {} file ", accidentDataFile);
            accidentDataReader.init(DATA_PROCESSING_BATCH_SIZE, accidentDataFile);
            processFile();
        }
    }

    private void processFile(){
        ExecutorService executor = Executors.newFixedThreadPool(3);
        FutureTask<RoadAccident> readerTask = new FutureTask<RoadAccident>(new Runnable() {
            @Override
            public void run() {
                int batchCount = 1;
                while (!accidentDataReader.hasFinished()) {
                    try {
                        List<RoadAccident> roadAccidents = accidentDataReader.getNextBatch();
                        roadAccidentQ.put(roadAccidents);
                        log.info("Read [{}] records in batch [{}]", roadAccidents.size(), batchCount);
                    } catch (InterruptedException e) {
                        processing = false;
                    }
                }
            }
        }, null);
        executor.execute(readerTask);

        FutureTask<RoadAccidentDetails> enrichTask = new FutureTask<RoadAccidentDetails>(new Runnable() {
            @Override
            public void run() {
                try {
                    while (processing) {
                        List<RoadAccidentDetails> roadAccidentDetailsList = accidentDataEnricher.enrichRoadAccidentData(roadAccidentQ.take());
                        roadAccidentDetailsQ.put(roadAccidentDetailsList);
                        log.info("Enriched records");
                    }
                } catch (InterruptedException e) {
                    processing = false;
                }

            }
        }, null);
        executor.execute(enrichTask);

        FutureTask<RoadAccidentDetails> writerTask = new FutureTask<RoadAccidentDetails>(new Runnable() {
            @Override
            public void run() {
                try {
                    while (processing) {
                        accidentDataWriter.writeAccidentData(roadAccidentDetailsQ.take());
                        log.info("Written records");
                    }
                } catch (InterruptedException e) {
                    processing = false;
                }
            }
        }, null);
        executor.execute(writerTask);

        try {
            if(readerTask.get() != null || !processing){
                readerTask.cancel(true);
                enrichTask.cancel(true);
                writerTask.cancel(true);
                executor.shutdownNow();
            }
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }


    public static void main(String[] args) {
        AccidentDataProcessor dataProcessor = new AccidentDataProcessor();
        long start = System.currentTimeMillis();
        dataProcessor.init();
        dataProcessor.process();
        long end = System.currentTimeMillis();
        System.out.println("Process finished in s : " + (end-start)/1000);
    }

}
