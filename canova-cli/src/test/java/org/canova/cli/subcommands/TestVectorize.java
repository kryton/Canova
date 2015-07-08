/*
 *
 *  *
 *  *  * Copyright 2015 Skymind,Inc.
 *  *  *
 *  *  *    Licensed under the Apache License, Version 2.0 (the "License");
 *  *  *    you may not use this file except in compliance with the License.
 *  *  *    You may obtain a copy of the License at
 *  *  *
 *  *  *        http://www.apache.org/licenses/LICENSE-2.0
 *  *  *
 *  *  *    Unless required by applicable law or agreed to in writing, software
 *  *  *    distributed under the License is distributed on an "AS IS" BASIS,
 *  *  *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  *  *    See the License for the specific language governing permissions and
 *  *  *    limitations under the License.
 *  *
 *
 */

package org.canova.cli.subcommands;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.canova.api.conf.Configuration;
import org.canova.api.exceptions.CanovaException;
import org.canova.api.formats.input.InputFormat;
import org.canova.api.formats.output.OutputFormat;
import org.canova.api.io.data.DoubleWritable;
import org.canova.api.records.reader.RecordReader;
import org.canova.api.records.reader.impl.LineRecordReader;
import org.canova.api.records.writer.RecordWriter;
import org.canova.api.records.writer.impl.SVMLightRecordWriter;
import org.canova.api.split.FileSplit;
import org.canova.api.split.InputSplit;
import org.canova.api.util.ArchiveUtils;
import org.canova.api.writable.Writable;
import org.canova.image.mnist.MnistFetcher;
import org.canova.image.recordreader.MNISTRecordReader;
import org.junit.Test;
import org.springframework.core.io.ClassPathResource;

public class TestVectorize {

	private static final String trainingFilesFilename = "images-idx1-ubyte.gz";
	private static final String trainingFilesURL = "http://yann.lecun.com/exdb/mnist/train-images-idx3-ubyte.gz";
	public static final String trainingFilesFilename_unzipped = "images-idx1-ubyte";

	private static final String trainingFileLabelsURL = "http://yann.lecun.com/exdb/mnist/train-labels-idx1-ubyte.gz";
	private static final String trainingFileLabelsFilename = "labels-idx1-ubyte.gz";
	public static final String trainingFileLabelsFilename_unzipped = "labels-idx1-ubyte";
	
	
    static String TEMP_ROOT = "/tmp"; //System.getProperty("user.home");
    static String MNIST_ROOT = TEMP_ROOT + File.separator + "MNIST" + File.separator;   
    
    static String MNIST_Filename = MNIST_ROOT + MNISTRecordReader.trainingFilesFilename_unzipped;
	
	
	public void checkForMNISTLocally() {
		
    	
    	// 1. check for the MNIST data first!
    	
    	// does it exist?
    	
    	// if not, then let's download it
    	
        System.out.println( "Checking to see if MNIST exists locally: " + MNIST_ROOT );
        
        if(!new File(MNIST_ROOT).exists()) {
        	
        	new File(MNIST_ROOT).mkdir();
        	
        	System.out.println("Downloading and unzipping the MNIST dataset locally to: " + MNIST_ROOT );
            try {
				//new MnistFetcher().downloadAndUntar();
            	downloadAndUntar();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
        } else {
        	
        	System.out.println( "MNIST already exists locally..." );
        	
        }
        
        if ( new File(MNIST_Filename).exists() ) {
        	System.out.println( "The images file exists locally unzipped!" );
        } else {
        	System.out.println( "The images file DOES NOT exist locally unzipped!" );
        	System.out.println("Downloading and unzipping the MNIST dataset locally to: " + MNIST_ROOT );
            try {
				//new MnistFetcher().downloadAndUntar();
            	downloadAndUntar();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
        	
        }		
		
	}
	
	
	

	/**
	 * Added another copy of this method because MNISTFetcher uses the user's system home directory
	 * and we couldnt specify that in the canova conf file
	 * 
	 * @return
	 * @throws IOException
	 */
	public static File downloadAndUntar() throws IOException {
		
		File fileDir;
		
		
//		if(fileDir != null) {
	//		return fileDir;
		//}
		// mac gives unique tmp each run and we want to store this persist
		// this data across restarts
		//File tmpDir = new File(System.getProperty("user.home"));

		File baseDir = new File( MNIST_ROOT );
		if(!(baseDir.isDirectory() || baseDir.mkdir())) {
			throw new IOException("Could not mkdir " + baseDir);
		}


		//log.info("Downloading mnist...");
		// getFromOrigin training records
		File tarFile = new File(baseDir, trainingFilesFilename);

		if(!tarFile.isFile()) {
			FileUtils.copyURLToFile(new URL(trainingFilesURL), tarFile);
		}

	    ArchiveUtils.unzipFileTo(tarFile.getAbsolutePath(),baseDir.getAbsolutePath());




        // getFromOrigin training records
        File labels = new File(baseDir, trainingFileLabelsFilename);

        if(!labels.isFile()) {
            FileUtils.copyURLToFile(new URL(trainingFileLabelsURL), labels);
        }

        ArchiveUtils.unzipFileTo(labels.getAbsolutePath(), baseDir.getAbsolutePath());



        fileDir = baseDir;
		return fileDir;
	}	
	
	public static File download_LFW_AndUntar(String workingBaseDir) throws IOException {
		
		String lfwURL = "http://vis-www.cs.umass.edu/lfw/lfw-bush.tgz";
		String lfwUrlFilename = "lfw-bush.tgz";
		File fileDir;
		
		
//		if(fileDir != null) {
	//		return fileDir;
		//}
		// mac gives unique tmp each run and we want to store this persist
		// this data across restarts
		//File tmpDir = new File(System.getProperty("user.home"));

		File baseDir = new File( workingBaseDir );
		if(!(baseDir.isDirectory() || baseDir.mkdir())) {
			throw new IOException("Could not mkdir " + baseDir);
		}


		//log.info("Downloading mnist...");
		// getFromOrigin training records
		File tarFile = new File(baseDir, lfwUrlFilename);

		if(!tarFile.isFile()) {
			FileUtils.copyURLToFile(new URL( lfwURL ), tarFile);
		}

	    ArchiveUtils.unzipFileTo(tarFile.getAbsolutePath(),baseDir.getAbsolutePath());


        fileDir = baseDir;
		return fileDir;
	}
	
    /**
     * Creates an input format
     *
     * @return
     */
    public static InputFormat createInputFormat(String inputFormat) {

        //System.out.println( "> Loading Input Format: " + (String) this.configProps.get( INPUT_FORMAT ) );

        String clazz = inputFormat; //(String) this.configProps.get(INPUT_FORMAT);
/*
        if (null == clazz) {
            clazz = DEFAULT_INPUT_FORMAT_CLASSNAME;
        }
*/
        try {
            Class<? extends InputFormat> inputFormatClazz = (Class<? extends InputFormat>) Class.forName(clazz);
            return inputFormatClazz.newInstance();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }	
	
	public static int checkNumberOfRecordsInSVMLightOutput(String filenamePath) throws IOException, InterruptedException {
		
		//String[] args = { "-conf", "src/test/resources/text/conf/text_vectorization_conf_unit_test.txt" };		
		//Vectorize vecCommand = new Vectorize( args );
		

        Configuration conf = new Configuration();
//        conf.set( OutputFormat.OUTPUT_PATH, "" );
		
		String inputFormatKey = "input.format";
		String svmLightInputFormat = "org.canova.api.formats.input.impl.SVMLightInputFormat";
		
		conf.set(inputFormatKey, svmLightInputFormat);
		
		//String datasetInputPath = (String) vecCommand.configProps.get("input.directory");
		
		InputFormat inputformat = createInputFormat( svmLightInputFormat );
		
		//RecordReader rr = inputformat.
		
        File inputFile = new File( filenamePath );
        InputSplit split = new FileSplit(inputFile);
        //InputFormat inputFormat = this.createInputFormat();
        
        
        
        System.out.println( "input file: " + filenamePath );

        RecordReader reader = inputformat.createReader(split, conf );
        
        int count = 0;
        while (reader.hasNext()) {
        	
        	count++;
        	Collection<Writable> vector = reader.next();

        	String label = getLabelFromSVMLightVector(vector);
        	//System.out.println( label );
        	
        }
        
        //assertEquals( 4, count );
				
			
        return count;
		
		
	}
	

	public static int countLabelsInSVMLightOutput(String filenamePath) throws IOException, InterruptedException {
		
		List<String> labels  = new ArrayList<>();

        Configuration conf = new Configuration();
		
		String inputFormatKey = "input.format";
		String svmLightInputFormat = "org.canova.api.formats.input.impl.SVMLightInputFormat";
		
		conf.set(inputFormatKey, svmLightInputFormat);
		
		//String datasetInputPath = (String) vecCommand.configProps.get("input.directory");
		
		InputFormat inputformat = createInputFormat( svmLightInputFormat );
		
		//RecordReader rr = inputformat.
		
        File inputFile = new File( filenamePath );
        InputSplit split = new FileSplit(inputFile);
        //InputFormat inputFormat = this.createInputFormat();
        
        
        
        System.out.println( "input file: " + filenamePath );

        RecordReader reader = inputformat.createReader(split, conf );
        
        
        
        //int count = 0;
        while (reader.hasNext()) {
        	
        	//count++;
        	Collection<Writable> vector = reader.next();

        	String labelName = getLabelFromSVMLightVector(vector);
        	//System.out.println( label );
        	
            if(!labels.contains(labelName)) {
                labels.add(labelName);
            }

        	
        }
        
        reader.close();
        return labels.size();
        //assertEquals( 4, count );
				
			
        //return count;
		
		
	}	
	
	public static String getLabelFromSVMLightVector(Collection<Writable> vector) {
		
		//Iterator<Writable> iter = vector.iterator();
		
		String label = (String)(vector.toArray()[ vector.size() - 1 ]).toString(); 
    	
		//while (iter.hasNext()) {
			
		//	String firstLabel = (String) iter.next().toString();
			
		//}
		
		return label;
		
	}
	
	
	public static void setupLFWSampleLocally() throws IOException {
		
		
		String localUnzippedSubdir = "lfw";
		String workingDir = "/tmp/canova/image/"; // + localUnzippedSubdir; 
		
		// does the file exist locally?
		
		download_LFW_AndUntar( workingDir );
		
		// let's only get a few images in 2 labels
		
		
	}
	
	@Test
	public void testLoadConfFile() throws IOException {
				
		String[] args = { "-conf", "src/test/resources/csv/confs/unit_test_conf.txt" };		
		Vectorize vecCommand = new Vectorize( args );
		
		vecCommand.loadConfigFile();
		assertEquals( "/tmp/iris_unit_test_sample.txt", vecCommand.configProps.get("canova.output.directory") );
		
	}
	
	@Test
	public void testExecuteCSVConversionWorkflow() throws Exception {
		
		String[] args = { "-conf", "src/test/resources/csv/confs/unit_test_conf.txt" };		
		Vectorize vecCommand = new Vectorize( args );
		
		vecCommand.execute();
		
		// now check the output
		int count = checkNumberOfRecordsInSVMLightOutput( vecCommand.outputVectorFilename );
		assertEquals(12, count);
	}	
	
	@Test
	public void testExecuteCSVConversionWorkflow_WithShuffle() throws Exception {
		
		String[] args = { "-conf", "src/test/resources/csv/confs/unit_test_csv_conf_w_shuffle.txt" };		
		Vectorize vecCommand = new Vectorize( args );
		
		vecCommand.execute();
		
		// now check the output
		int count = checkNumberOfRecordsInSVMLightOutput( vecCommand.outputVectorFilename );
		assertEquals(12, count);
	}	

	@Test
	public void testExecuteCSVConversionWorkflow_SkipHeader() throws Exception {
		
		String[] args = { "-conf", "src/test/resources/csv/confs/unit_test_csv_conf_skip_header.txt" };		
		Vectorize vecCommand = new Vectorize( args );
		
		vecCommand.execute();
		
		// now check the output
		int count = checkNumberOfRecordsInSVMLightOutput( vecCommand.outputVectorFilename );
		assertEquals(12, count);
	}	
	
	
	@Test
	public void testExecuteImageCustomMNISTInputFormatConversionWorkflow() throws Exception {
		
		checkForMNISTLocally();
		
		String[] args = { "-conf", "src/test/resources/image/conf/mnist/unit_test_conf.txt" };		
		Vectorize vecCommand = new Vectorize( args );
		
		vecCommand.execute();
		
		// now check the output
		
		// 1. how many labels are there?
		
		// 2. how many vectors are in the output?
		
		int count = checkNumberOfRecordsInSVMLightOutput( vecCommand.outputVectorFilename );
		int labelCount = countLabelsInSVMLightOutput( vecCommand.outputVectorFilename );
		
		//System.out.println( "Vectors in file: " + count );
		
		// this seems .... odd?
		assertEquals( 59999, count );
		assertEquals( 10, labelCount );
		
		
	}	
	
	/**
	 * Testing the normal image input format reader
	 * 
	 * should be 530 records in the output
	 * 
	 * 
	 * @throws Exception
	 */
	@Test
	public void testExecuteImageInputFormatConversionWorkflow() throws Exception {
		
//	1. setup the LWF dataset sample to work with
		
		setupLFWSampleLocally();
		
		String[] args = { "-conf", "src/test/resources/image/conf/unit_test_conf.txt" };		
		Vectorize vecCommand = new Vectorize( args );
		
		vecCommand.execute();
		
		// now check the output
		
		// 1. how many labels are there?
		
		// 2. how many vectors are in the output?
		
		int count = checkNumberOfRecordsInSVMLightOutput( vecCommand.outputVectorFilename );
		int labelCount = countLabelsInSVMLightOutput( vecCommand.outputVectorFilename );
		
		System.out.println( "Vectors in file: " + count );
		
		assertEquals( 530, count );
		assertEquals( 1, labelCount );
		
	}	
	
	/**
	 * Testing the normal image input format reader
	 * 
	 * should be 530 records in the output
	 * 
	 * with shuffle!
	 * 
	 * @throws Exception
	 */
	@Test
	public void testExecuteImageInputFormat_WithShuffle_ConversionWorkflow() throws Exception {
		
//	1. setup the LWF dataset sample to work with
		
		setupLFWSampleLocally();
		
		String[] args = { "-conf", "src/test/resources/image/conf/unit_test_w_shuffle_conf.txt" };		
		Vectorize vecCommand = new Vectorize( args );
		
		vecCommand.execute();
		
		// now check the output
		
		// 1. how many labels are there?
		
		// 2. how many vectors are in the output?
		
		int count = checkNumberOfRecordsInSVMLightOutput( vecCommand.outputVectorFilename );
		int labelCount = countLabelsInSVMLightOutput( vecCommand.outputVectorFilename );
		
		System.out.println( "Vectors in file: " + count );
		
		assertEquals( 530, count );
		assertEquals( 1, labelCount );
		
	}	
	
	/**
	 * 
	 * 
	 * 
	 * @throws Exception
	 */
	@Test
	public void testExecuteTextInputFormat_TFIDF_ConversionWorkflow() throws Exception {
		
		String[] args = { "-conf", "src/test/resources/text/DemoTextFiles/conf/text_vectorization_conf_unit_test.txt" };		
		Vectorize vecCommand = new Vectorize( args );
		
		vecCommand.execute();
		
	
		
		// now check the output
		
		// now check the output
		
		// 1. how many labels are there?
		
		// 2. how many vectors are in the output?
		
		int count = checkNumberOfRecordsInSVMLightOutput( vecCommand.outputVectorFilename );
		int labelCount = countLabelsInSVMLightOutput( vecCommand.outputVectorFilename );
		
	//	System.out.println( "Vectors in file: " + count );
		
		// this seems .... odd?
		assertEquals( 4, count );
		assertEquals( 2, labelCount );
		
		// check the vocab?
		
		
	}		

	@Test
	public void testExecuteTextInputFormat_TFIDF_Tweets_ConversionWorkflow() throws Exception {
		
		String[] args = { "-conf", "src/test/resources/text/Tweets/conf/tweet_conf.txt" };		
		Vectorize vecCommand = new Vectorize( args );
		
		vecCommand.execute();
		
		
		
		// 1. how many labels are there?
		
		// 2. how many vectors are in the output?
		
		int count = checkNumberOfRecordsInSVMLightOutput( vecCommand.outputVectorFilename );
		int labelCount = countLabelsInSVMLightOutput( vecCommand.outputVectorFilename );
		
	//	System.out.println( "Vectors in file: " + count );
		
		// this seems .... odd?
		assertEquals( 15, count );
		assertEquals( 3, labelCount );
		
		// check the vocab?
		
		
	}		
	

	@Test
	public void testExecuteTextInputFormat_TFIDF_Tweets_ConversionWorkflow_WithShuffle() throws Exception {
		
		String[] args = { "-conf", "src/test/resources/text/Tweets/conf/tweet_conf_w_shuffle.txt" };		
		Vectorize vecCommand = new Vectorize( args );
		
		vecCommand.execute();
		
		
		
		// 1. how many labels are there?
		
		// 2. how many vectors are in the output?
		
		int count = checkNumberOfRecordsInSVMLightOutput( vecCommand.outputVectorFilename );
		int labelCount = countLabelsInSVMLightOutput( vecCommand.outputVectorFilename );
		
	//	System.out.println( "Vectors in file: " + count );
		
		// this seems .... odd?
		assertEquals( 15, count );
		assertEquals( 3, labelCount );
		
		// check the vocab?
		
		
	}		

	/**
	 * TODO: need some work here on the LFW test data
	 * 
	 * 
	 * @throws Exception
	 */
	@Test
	public void testExecuteAudioInputFormatConversionWorkflow() throws Exception {
		
//	1. setup the LWF dataset sample to work with
		
	//	setupLFWSampleLocally();
		/*
		String[] args = { "-conf", "src/test/resources/audio/conf/unit_test_conf.txt" };		
		Vectorize vecCommand = new Vectorize( args );
		
		vecCommand.execute();
		*/
		// now check the output
		
		// 1. how many labels are there?
		
		// 2. how many vectors are in the output?
		
	}	
	
	
	
}
