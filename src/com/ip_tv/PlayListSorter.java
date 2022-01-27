package com.ip_tv;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

public class PlayListSorter {

	private static final String PARENT_CODE_PARAM = "parent-code=\"4247\"";
	private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("MM.dd");
	private static /* final */ String ADULT_GROUP_NAME_RUS;
	static {
		try {
//			ByteBuffer encode = StandardCharsets.UTF_8.encode( "\u0432\u0437\u0440\u043e\u0441\u043b\u044b\u0435");
//			word = new String(encode.array(), "utf-8").trim(); 
			// see https://www.branah.com/unicode-converter, or https://www.browserling.com/tools/utf16-encode
			ADULT_GROUP_NAME_RUS = new String("\u0432\u0437\u0440\u043e\u0441\u043b\u044b\u0435".getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8);
		} catch (Exception e) {
			e.printStackTrace();
			ADULT_GROUP_NAME_RUS= null;
		}
	}

	public static void main (String[] args) {
		String pathname = args[0];
		File file = new File(pathname);
		String fileType = null;
		int counter = -1;		
		ArrayList<ChannelData> allList = new ArrayList<>();
		String paramsAndName=null;
		String group = null;
		String link=null;
//		try( BufferedReader br = new BufferedReader( new FileReader(file))) {	
		
		try( BufferedReader br = new BufferedReader( new InputStreamReader(new FileInputStream(file), "UTF8"))) {
		    for(String line; (line = br.readLine()) != null; ) {
		    	if (counter < 0) {
		    		fileType = line;		    		
		    	} else {
		    		if (counter == 0) {
		    			paramsAndName = line;
		    		} else if (counter == 1) {
		    			group =  line;
		    		} else if (counter == 2) {
		    			link = line;		    			
		    		}
		    	}
		    	
	    		if ( paramsAndName != null && group != null && link != null ) {	    			
	    			allList.add(new ChannelData(paramsAndName, group, link));
	    			paramsAndName = null;
	    			group = null;
	    			link = null;
	    			counter = 0;
	    		} else {	        
			        counter++;
	    		}		    	
		    }
		    // line is not visible here.
		} catch ( Exception ex ) {
			ex.printStackTrace();
		}
		
		allList.sort( new Comparator<ChannelData>() {
			public int compare(ChannelData o1, ChannelData o2) {
				return o1.m_name.toLowerCase().compareTo(o2.m_name.toLowerCase());
//				return o2.m_name.compareTo(o1.m_name);
			};
		} );
		
		String fileName = file.getParent() +"\\"+ DATE_FORMAT.format(new Date()) +".sorted." + file.getName();
		new File(fileName).delete();
		try {
			PrintWriter pw = new PrintWriter( fileName, "UTF8");
			pw.println(fileType);		
		    for ( ChannelData channel: allList ) {
		        pw.println( channel.m_paramsAndName);
		        pw.println( channel.m_group);
		        pw.println( channel.m_link);
		    }
		    pw.close();
		    System.out.println("Finshed:" + allList.size() + " channels.");		    
		    
		} catch (Exception ex) {
			ex.printStackTrace();
		}		
		
	}	
	

	private static class ChannelData {
		private static final String TIME_SHIFT_4_DAYS = "timeshift=%s";
		public static final String DEF_ARCHIVE_DAYS = "\"4\"";
		//vars
		private final String m_paramsAndName;
		private final String m_group;
		private final String m_link;
		private final String m_name;
		 
		ChannelData(String paramsAndName, String group, String link) {
			m_group = group;// e.g. "#EXTGRP:USA"
			// extract name from "#EXTINF:0 tvg-rec="1",TV 1000"
			String[] paramsAndNameList = paramsAndName.split(",");
			m_name = paramsAndNameList[1].trim();// second is name
			String[] params = paramsAndNameList[0].split(" ");// first are parameters list separated by space character
			List<String> changedParams = new ArrayList<String>( Arrays.asList(params) );
			String days = DEF_ARCHIVE_DAYS;
			for ( String param : changedParams) {
				if (param.startsWith("tvg-rec")) {// tvg-rec="1"
					String[] tvgRec = param.split("=");
					if (tvgRec.length >1) {
						days =tvgRec[1];
					}
				}
			}
			changedParams.add( String.format(TIME_SHIFT_4_DAYS, days) );// add archive parameter
			if ( m_group!= null && m_group.contains(ADULT_GROUP_NAME_RUS)) {
				changedParams.add(PARENT_CODE_PARAM);
			}
			// rebuild parameters with space separator 
			String updatedParams = String.join(" ", changedParams);
			//restore parameters + name
			m_paramsAndName = updatedParams + "," + m_name;
			m_link = link;			
		}
		
		@Override public String toString() { return m_link ;}
	}
}
