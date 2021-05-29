package me.sureshs.cowinalert.service;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.LineIterator;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class CommonService {

	protected List<String> readFile(String inputFileName){

		List<String> lines = new ArrayList<String>();

		try (LineIterator it = FileUtils.lineIterator(new File(inputFileName), StandardCharsets.UTF_8.name())) {

			while (it.hasNext()) {

				lines.add(it.nextLine().trim());
			}
		} catch (Exception e) {
			log.error("read file failed : {}", inputFileName);
			e.printStackTrace();

			System.exit(1);
		}

		log.info("# lines : {}", lines.size());
		
		return lines;

	}
	
	protected void writeToFile(String outputFileName, List<String> lines) {

		try {
			FileUtils.writeLines(new File(outputFileName), lines);

			log.info("write to file complete : " + outputFileName);
			
		} catch (IOException e) {
			log.error("write to file failed : {}", outputFileName);

			e.printStackTrace();
		}
	}

}
