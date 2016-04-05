import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class average_degree {
	
	//a function to extract the timestamp from the tweet and convert it to a long int
	//representing the number of seconds since Jan 1, 1970, 00:00:00 GMT
	public static long getTimeStamp(String tweet) throws ParseException{
		//Obtain the timestamp which is surround by "created_at":" and "
		String key_created_at="\"created_at\":\"";
		int index_start=tweet.indexOf(key_created_at)+key_created_at.length();
		int index_end=tweet.indexOf("\"",index_start);
		String time_stamp=tweet.substring(index_start,index_end);
	
		//extract each element from the timestamp which has the format xxx xxx xx xx:xx:xx +xxxx xxxx 
		//e.g. Fri Oct 30 15:29:45 +0000 2015 (separated by spaces) 
		String[] date_elements=time_stamp.split("\\s+");
		String[] time_elements=date_elements[3].split(":");
		//define the pattern for the SimpleDateFormat for subsequent parsing 
		SimpleDateFormat simple_date_format=new SimpleDateFormat("yyyy.MMM.dd.HH.mm.ss");
		String rearranged_time_stamp=date_elements[5]+"."+date_elements[1]+"."+date_elements[2]+"."+time_elements[0]+"."+time_elements[1]+"."+time_elements[2];
		Date time_stamp_in_SDF=simple_date_format.parse(rearranged_time_stamp);
		
		//return a long int representing the number of seconds the timestamp since Jan 1, 1970, 00:00:00 GMT
		long time_stamp_in_sec=time_stamp_in_SDF.getTime()/1000;
		return time_stamp_in_sec;		
	}
	
	//a function to extract all the hashtags from a tweet
	public static ArrayList<String> getHashtags(String tweet){
		ArrayList<String> hashtag_list=new ArrayList<String>();
		String key_hashtags="\"hashtags\":[";
		String key_text="\"text\":\"";
						
		//look for the last occurrence of the key "hashtags" in the tweet  
		int index_start=tweet.lastIndexOf(key_hashtags)+key_hashtags.length();

		//each hashtag is surround by "text":" and "
		int index_text=tweet.indexOf(key_text,index_start);
		int index_close_bracket=tweet.indexOf("]",index_start);
		//the list ends when the text: field is not found or when the close square bracket ] appears before the text: field
		while((index_text!=-1 && index_close_bracket!=-1) && index_text<index_close_bracket){
			int index_hashtag_start=index_text+key_text.length();
			int index_hashtag_end=tweet.indexOf("\"",index_hashtag_start);
			String hashtag=tweet.substring(index_hashtag_start,index_hashtag_end);
			hashtag_list.add(hashtag);
			//start looking for index of the next hashtag and close bracket
			index_start=index_close_bracket+1;
			index_text=tweet.indexOf(key_text,index_start);
			index_close_bracket=tweet.indexOf("]",index_start);
		}
		
		return hashtag_list;
	}
	
	//A function that goes through the list of hashtags from the new tweet entry
	//it will add new links to the hashtag_graph if the link never exists before, else it will update the timestamp of existing links
	public static void addHashtags(HashMap<String,HashMap<String,Long>>hashtag_graph, ArrayList<String> hashtag_list,Long time_stamp){
		//exits when there are less than 2 hashtags from the new tweet entry
		if(hashtag_list.size()<=1) return;
		
		//loop through the list of hashtags from the new tweet entry
		for(String hashtag:hashtag_list){
			//find all other distinct hashtags to be connected to the first hashtag
			for(String hashtag_to_be_connected:hashtag_list){
				if(!hashtag_to_be_connected.equals(hashtag)){
					//if the hashtag and the hashtag_to_be_connected exist in the hashtag graph already, update the timestamp if necessary
					//if the hashtag exist but the link to hashtag_to_be_connected does not exist, add a link from hashtag to hashtag_to_be_connected
					if(hashtag_graph.containsKey(hashtag)){
						if(hashtag_graph.get(hashtag).containsKey(hashtag_to_be_connected)){
							if(time_stamp>hashtag_graph.get(hashtag).get(hashtag_to_be_connected)){
								hashtag_graph.get(hashtag).put(hashtag_to_be_connected, time_stamp);
							}
						}
						else{
							hashtag_graph.get(hashtag).put(hashtag_to_be_connected, time_stamp);
						}
					}
					//if the hashtag does not exist, add a new node for the hashtag and a new link connecting to hashtag_to_be_connected
					else{
						HashMap<String,Long> new_link=new HashMap<String,Long>();
						new_link.put(hashtag_to_be_connected, time_stamp);
						hashtag_graph.put(hashtag,new_link);
					}
				}
			}
		}		
	}
	
	//A function to remove links that is older than 60secs from the maximum timestamp
	public static void removeOldLinks(HashMap<String,HashMap<String,Long>>hashtag_graph,long max_time_stamp){
		//go through each node in the hashtag graph and remove all links that's older than 60secs
		for(String hashtag:hashtag_graph.keySet()){
			Iterator<Map.Entry<String,Long>> link_list =hashtag_graph.get(hashtag).entrySet().iterator();
			while(link_list.hasNext()){
				Map.Entry<String,Long> link=link_list.next();
				//remove the link if it's older than 60secs
				if(link.getValue()<=max_time_stamp-60){
					link_list.remove();
				}				
			}
		}
		//remove all hashtag nodes with no link (or no connection to other hashtag)
		Iterator<Map.Entry<String,HashMap<String,Long>>> node_list=hashtag_graph.entrySet().iterator();
		while(node_list.hasNext()){
			Map.Entry<String, HashMap<String,Long>> node=node_list.next();
			if(node.getValue().isEmpty()){
				node_list.remove();
			}
		}
	}
	
	//A function to calculate the average degree of a vertex in the Twitter hashtag graph
	public static double calculateAvgDegree(HashMap<String,HashMap<String,Long>> hashtag_graph){
		int sum_of_degree=0;
		int num_vertex=0;
		
		//sum the number of links for each vertex
		for(String vertex:hashtag_graph.keySet()){
			sum_of_degree+=hashtag_graph.get(vertex).size();
			num_vertex+=1;
		}
		if(num_vertex==0){ 
			return 0;
		}
		//truncate the average to two decimal places
		double temp=(sum_of_degree*100)/num_vertex;
		double avg_degree=temp/100.0;
		return avg_degree;
	}
	
	public static void main(String[] args) throws IOException, ParseException {
		//Open the input and output files
		File dir=new File("..");
		
		FileReader fr=new FileReader("../tweet_input/tweets.txt");		
		BufferedReader br=new BufferedReader(fr);
		FileWriter fw=new FileWriter("../tweet_output/output.txt");		
		PrintWriter print_line=new PrintWriter(fw);
		
		String line=null;
		long max_time_stamp=-1;
		//the hashtag graph is stored in a HashMap with the key representing each vertex in the graph,
		//and the value being another HashMap with the key representing all other vertices connecting to the first vertex
		//and value being the timestamp when the link was created
		HashMap<String,HashMap<String,Long>> hashtag_graph=new HashMap<String,HashMap<String,Long>>();
		
		//read 1 tweet at a time
		while((line=br.readLine())!=null){
			//read only tweets with the field created_at (i.e. ignore the rate limiting messages)
			if(line.indexOf("\"created_at\":")!=-1){
				//obtain the timestamp and the list of hashtags for the tweet
				long time_stamp=getTimeStamp(line);
				ArrayList<String> hashtag_list=getHashtags(line);
				
				//only update hashtag graph if the new tweet is not older than 60 secs from maximum timestamp
				if(time_stamp>max_time_stamp-60){
					addHashtags(hashtag_graph,hashtag_list,time_stamp);
					//update the maximum timestamp if necessary, and remove older links
					if(time_stamp>max_time_stamp){
						max_time_stamp=time_stamp;
						removeOldLinks(hashtag_graph,max_time_stamp);
					}
				}
				//calculate and print the average degree of the hashtag graph
				double avg_degree=calculateAvgDegree(hashtag_graph);
				print_line.printf("%.2f"+"%n", avg_degree);
			}
		}
		br.close();
		print_line.close();

	}

}
