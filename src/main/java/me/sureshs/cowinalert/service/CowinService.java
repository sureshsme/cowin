package me.sureshs.cowinalert.service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import lombok.extern.slf4j.Slf4j;
import me.sureshs.cowinalert.model.Center;
import me.sureshs.cowinalert.model.Response;
import me.sureshs.cowinalert.model.Session;
import me.sureshs.cowinalert.model.result.Result;

@Service
@Slf4j
public class CowinService {

	@Value("${url}")
	private String url;
	
	@Value("${district_id}")
	private List<String> districts;
	
	@Value("${date:#{null}}")
	private String date;

	@Value("${days:#{2}}")
	private int days;
	
	@Value("${threshold:#{1}}")
	private int threshold;

	@Value("${telegram.url}")
	private String telegramUrl;
	
	@Value("${telegram.url.test}")
	private String telegramUrlTest;

	@Value("${telegram.chatId.admin}")
	private String telegramAdmin;
	
	@Value("#{${telegram.subscribers:{:}}}")
	private Map<String, List<String>> subscribers;

	@Value("#{${districts.map}}")
	private Map<String, String> districtsMap;

	public void getResponseForDistrict() {
 
		List<String> dates = getDatesToCheck(this.date, this.days);
		
		Map<String, Set<Result>> resultsMap = getResultMap(subscribers.keySet(), dates);
		
		resultsMap.keySet().forEach(k -> {

			Set<Result> results = resultsMap.get(k);
			
			String summary = "found " + results.size() + " locations @ " + this.districtsMap.get(k) + " from " + dates.get(0)
							+ " to " + dates.get(dates.size() - 1) + " with at least " + this.threshold + " slot(s)";
			log.info(summary);
			
			if (!results.isEmpty()) {
				
				sendMessages(this.telegramUrl, this.subscribers.get(k), summary, results);
				
				results.forEach(r -> log.info(r.toString()));
				
			}

		
		});

		
	}


	private String formatSummary(Set<Result> results, String summary) {
		
		StringBuilder msg = new StringBuilder();
		msg.append(summary).append("\n\n");
		results.forEach(r -> msg.append(r).append("\n"));
		
		String textToSend = msg.toString();
		
		 if (textToSend.length() > 4000)
			 textToSend = textToSend.substring(0, 4000).concat("...");

		return textToSend;
	}
	
	private void sendMessages(String url, List<String> chatIds, String summary, Set<Result> results) {

		String text = formatSummary(results, summary);

		chatIds.forEach(c -> sendTelegramMessage(url, c, text));

	}
	
	
	public void sendTelegramMessage(String url, String chatId, String text) {

		RestTemplate rest = new RestTemplate();

		url += "?".concat("chat_id=").concat(chatId).concat("&text=").concat(text);

		rest.getForObject(url, String.class);

	}

	
	private List<String> getDatesToCheck(String date, int days) {

		List<String> dates = new ArrayList<String>();

		DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern("dd-MM-yyyy");
		LocalDate startDate = date != null? LocalDate.parse(date, dateFormat) : LocalDate.now(); 
		
		for(int i = 0; i < days; i++) {
			dates.add(startDate.plusDays(i).format(dateFormat));
		}

		return dates;
	}
	
	
	private Map<String, Set<Result>> getResultMap(Set<String> districts, List<String> dates) {
		
		Map<String, Set<Result>> resultsMap = new LinkedHashMap<String, Set<Result>>();

		
		for (String district : districts) {

			List<Result> results = new ArrayList<Result>();
			
			for (String date : dates) {

				log.debug("checking for date : {} @ {}", date, district);

				String url = this.url.concat("?").concat("district_id=").concat(district).concat("&date=").concat(date);
				log.debug(url);

				RestTemplate rest = new RestTemplate();

				HttpHeaders headers = new HttpHeaders();
				headers.set("user-agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/90.0.4430.93 Safari/537.36 Edg/90.0.818.51");

				try {

					ResponseEntity<Response> response = rest.exchange(url, HttpMethod.GET, new HttpEntity(headers), Response.class);

					results.addAll(parseResponse(response.getBody()));
					
				} catch (Exception e) {

					sendTelegramMessage(this.telegramUrlTest, this.telegramAdmin, "I have failed you :-( :-(!!");
					
					System.exit(0);
					
				}
			
			}
			
			resultsMap.put(district, new TreeSet<Result>(results));
			
		}
		
		return resultsMap;

	}
	
	
	private List<Result> parseResponse(Response response){
		
		List<Result> results = new ArrayList<Result>();
		
		for(Center center : response.getCenters()) {
	
				for (Session session : center.getSessions()) {
					
					if (session.getAvailable_capacity() >= this.threshold) {
						
						results.add(Result.builder()
								.name(center.getName())
								.fee_type(center.getFee_type())
								.date(session.getDate())
								.available_capacity(session.getAvailable_capacity())
								.vaccine(session.getVaccine())
								
								.build());
					}
				}

		}
		
		return results;
	}
}
