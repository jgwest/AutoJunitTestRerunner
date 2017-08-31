/*
   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License. 
*/
package com.autojunittestrerunner;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class ValueDB {

	private static ValueDB instance = new ValueDB();
	
	Map<String, String> map = new HashMap<String, String>();
	
	File f;
	
	private ValueDB() {
		f = new File(System.getProperty("user.home"), ".autojunittestrerunner"+File.separator+"settings.txt");
		
		if(!f.getParentFile().exists()) {
			f.getParentFile().mkdirs();
		}
		
		if(f.exists()) {
			BufferedReader br = null;
			try {
				FileReader fr = new FileReader(f);
				br = new BufferedReader(fr);
				String str = null;
				while(null != (str = br.readLine())) {
					if(str.trim().length() > 0) {
						try {
							int index = str.indexOf("`");
							String before = str.substring(0, index);
							String after = str.substring(index+1);
							map.put(before.toLowerCase().trim(), after);
						} catch(Exception e) {
							e.printStackTrace();
						}
					}
				}
				
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			} finally {
				if(br != null) { try { br.close(); } catch (IOException e) { } }
			}
		}
		
		
	}
	
	public synchronized void putValue(String key, String value) {
		map.put(key.toLowerCase().trim(), value);
		
		FileWriter fw = null;
		try {
			fw = new FileWriter(f);
			for(Map.Entry<String, String> e : map.entrySet()) {
				
				fw.write(e.getKey()+"`"+e.getValue()+"\n");
			}
			
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if(fw != null) { try { fw.close(); } catch (IOException e) { } }
		}
		
	}
	
	public synchronized String getValue(String key) {
		return map.get(key.toLowerCase().trim());
	}
	
	public static ValueDB getInstance() {
		return instance;
	}
	
	

}
