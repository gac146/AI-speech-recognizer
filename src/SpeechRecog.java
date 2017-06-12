import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Scanner;


/*
 * Author: Gustavo Carbone
 * Date: 05/29/2017
 */
public class SpeechRecog {
	
	//size of matrices and given data
	private static final int n = 27; //number of states
	private static final int T = 180000; //string length
	private static final int m = 2; //possible observed values
	
	//Given data
	private static double[][] transition = loadMatrix(n, n, "transitionMatrix.txt");; //n x n matrix
	private static double[][] emission = loadMatrix(n, m, "emissionMatrix.txt");;  //n x m matrix
	private static int[] observations = loadInt(T, "observations.txt");
	private static double[] initialDistr = loadDouble(n, "initialStateDistribution.txt");
	
	//matrices and arrays to be calculated for viterbi algorithm
	private static double[][] lStarMatrix = new double[n][T];
	private static int[][] phi = new int[n][T];
	private static int[] sStar = new int[T];
	
	private static int maxIndex; //keeps track of max index for phi table
	private static final int space = 26;
	
	//alphabet
	private static HashMap<Integer, Character> alphabet = new HashMap<>();

	/**
	 * Main executable
	 * @param args
	 */
	public static void main(String[] args) {
		
		//Performing steps for viterbi algorithm
		mapAlphabet();
		forwardPass();
		viterbiPass();
		String str = reduceString();		
		System.out.println(str);
		writeToFile(sStar);
	}
	
	/**
	 * Writes data to a file
	 * 
	 * @param arr
	 */
	private static void writeToFile(int[] arr) {
		
		//writing to file
		try{
		    PrintWriter writer = new PrintWriter("data.txt", "UTF-8");
		    for(int i : arr) {
		    	writer.println(i);
		    }
		    writer.close();
		} catch (IOException e) {
		   System.out.println("Error writing to file");
		}
	}
	
	
	/**
	 * Eliminates duplicated letters on the sStar
	 * 
	 * @return hidden message as a string
	 */
	private static String reduceString() {
		StringBuilder str = new StringBuilder();
		
		//base case
		str.append(Character.toString(alphabet.get(sStar[0])));
		
		//recursive step
		for(int i=1; i < (sStar.length - 10); i++) {
			if(sStar[i] != sStar[i-1]) {
				if(sStar[i] == space) str.append(Character.toString(alphabet.get(space)));
				else str.append(Character.toString(alphabet.get(sStar[i])));
			}
		}
		
		return str.toString();
	}
	
	/**
	 * Calculates the viterbi pass from phi table
	 */
	private static void viterbiPass() {
		int counter = T-1;
		int argMax = 0;
		
		//base case
		for(int row=0; row < phi.length; row++) {
			int tmp = phi[row][counter];
			if(tmp > argMax) argMax = tmp; 
		}
		sStar[counter] = argMax;
		
		//recursive step		
		while(counter > 0) {
			counter--;
			sStar[counter] = phi[sStar[counter+1]][counter+1];
		}
	}
	
	/**
	 * Calculates the forward pass of the algorithm using dynamic programming techniques
	 */
	private static void forwardPass() {
		int counter = 0;
		int tmpObsrv = observations[counter];
		
		//base case: getting all L*i1
		for(int row=0; row < lStarMatrix.length; row++) {
			double pi = Math.log(initialDistr[row]);
			double bi = Math.log(emission[row][tmpObsrv]);
			lStarMatrix[row][counter] = pi + bi;
		}
		counter++; //currently at T=1
		
		//recursive step
		while(counter < T) {
			tmpObsrv = observations[counter];
			
			for(int row=0; row < lStarMatrix.length; row++) {
				double maxI = findMaxI((counter - 1), row);
				double bi = Math.log(emission[row][tmpObsrv]);
				lStarMatrix[row][counter] = maxI + bi;
				phi[row][counter] = maxIndex;
				maxIndex = 0;
			}
			counter++;
		}
		
	}
	
	/**
	 * returns the max from i to n of (L*it + log(transitionMatrixij)
	 * 
	 * @param t - Time variable
	 * @param j - column j to check on transitionMatrix from row of L*
	 * @return - max value
	 */
	private static double findMaxI(int t, int j) {
		double max = -Double.MAX_VALUE;
		
		for(int i=0; i < n; i++) {
			double tmp = lStarMatrix[i][t] + Math.log(transition[i][j]);
			if(tmp > max){
				max = tmp;
				maxIndex = i;
			}
		}
		
		return max;
	}
	
	/**
	 * Mapping integers to characters from the alphabet [0-25]->[a-z] | 26->space
	 */
	private static void mapAlphabet() {
		for(int i=0; i < n; i++) {
			if(i == space)
				alphabet.put(i, ' ');
			else{
				char tmp = (char)(i + 97);
				alphabet.put(i, tmp);
			}
		}
	}
	
	/**
	 * Loads a file into a int array. It loads line by line, not word by word.
	 * 
	 * @param size - Size of the array that will hold the file
	 * @param file - Name of the file to read from
	 * @return array with line by line file
	 */
	private static double[] loadDouble(int size, String file) {
		double[] arr = new double[size];
		Scanner reader = null;
		int counter = 0;
		
		try {
			reader = new Scanner(new File(file));
		} catch(FileNotFoundException e) {
			System.out.println("File not found");
		}
		
		while(reader.hasNextLine()) {
			arr[counter] = Double.parseDouble(reader.nextLine());
			counter++;
		}
		
		return arr;
	}
	
	/**
	 * Loads matrix from given data
	 * 
	 * @param rows - number of rows in matrix
	 * @param cols - number of columns in matrix
	 * @param file - name of file to load data from
	 * @return - loaded matrix
	 */
	private static int[] loadInt(int size, String file) {
		int[] arr = new int[size];
		
		Scanner col = null;
		int i = 0;
		
		try {
			col = new Scanner(new File(file));			
		} catch( FileNotFoundException a) {
			System.out.println("File not found");
		} 
		
		while(col.hasNextLine()) {
			Scanner row = new Scanner(col.nextLine());
			while(row.hasNext()) {
				arr[i] = Integer.parseInt(row.next());
				i++;
			}
		}
		
		col.close();
		return arr;
	}
	
	/**
	 * Loads matrix from given data
	 * 
	 * @param rows - number of rows in matrix
	 * @param cols - number of columns in matrix
	 * @param file - name of file to load data from
	 * @return - loaded matrix
	 */
	private static double[][] loadMatrix(int rows, int cols, String file) {
		double[][] matrix = new double[rows][cols];
		
		Scanner col = null;
		int i = 0;
		int j = 0;
		
		try {
			col = new Scanner(new File(file));			
		} catch( FileNotFoundException a) {
			System.out.println("File not found");
		} 
		
		while(col.hasNextLine()) {
			Scanner row = new Scanner(col.nextLine());
			while(row.hasNext()) {
				matrix[i][j % cols] = Double.parseDouble(row.next());
				j++;
			}
			i++;
		}
		
		col.close();
		return matrix;
	}

}
