package me.sureshs.cowinalert.service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
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
	private String district_id;
	
	@Value("${date:#{null}}")
	private String date;

	@Value("${days:#{10}}")
	private int days;
	
	@Value("${threshold:#{1}}")
	private int threshold;

	@Value("${telegram.url}")
	private String telegramUrl;
	
	@Value("${telegram.chatIds}")
	private List<String> chatIds;
	
	@Value("${telegram.url.test}")
	private String telegramUrlTest;
	
	@Value("${centers.good:#{null}")
	private List<String> goodCenters;
	
	public void getResponseForDistrict() {

		List<String> dates = getDates();
		
		Set<Result> results = new TreeSet<Result>(getResult(dates));
		
		String summary = "found " + results.size() + " locations @ " + this.district_id + " from " + dates.get(0)
				+ " to " + dates.get(dates.size() - 1) + " with at least " + this.threshold + " slot(s)";
		log.info(summary);

		if (!results.isEmpty()) {
			
			sendMessage(results, summary, false);
			
			results.forEach(r -> log.info(r.toString()));
			
		}
		
	}

	public void sendMessage(Set<Result> results, String summary, boolean test) {

		StringBuilder msg = new StringBuilder();
		msg.append(summary).append("\n\n");
		results.forEach(r -> msg.append(r).append("\n"));
		
		String text = msg.toString();
		
		if (text.length() > 4000)
			text = text.substring(0, 4000).concat("...");
		
		RestTemplate rest = new RestTemplate();		
		
		if (!test) {
			for (String chatId : this.chatIds) {

				String url = this.telegramUrl.concat("?").concat("chat_id=").concat(chatId).concat("&text=").concat(text);
				
				rest.getForObject(url, String.class);
				
			}
		} else {
			rest.getForObject(this.telegramUrlTest,String.class);
		}
		
	}
	
	private List<String> getDates() {

		List<String> dates = new ArrayList<String>();

		DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern("dd-MM-yyyy");
		LocalDate startDate = this.date != null? LocalDate.parse(this.date, dateFormat) : LocalDate.now(); 
		
		for(int i = 0; i < this.days; i++) {
			dates.add(startDate.plusDays(i).format(dateFormat));
		}

		return dates;
	}
	
	private List<Result> getResult(List<String> dates) {
		List<Result> results = new ArrayList<Result>();

		for (String date : dates) {

			log.debug("checking for date : {} @ {}", date, this.district_id);

			String url = this.url.concat("?").concat("district_id=").concat(this.district_id).concat("&date=").concat(date);
			log.debug(url);

			RestTemplate rest = new RestTemplate();
			
			HttpHeaders headers = new HttpHeaders();
			//headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));

			headers.set("Content-Type", "text/html; charset=iso-8859-1");
			//headers.set("Accept-Charset", "iso-8859-1");
			headers.set("user-agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/90.0.4430.93 Safari/537.36 Edg/90.0.818.51");
			
			HttpEntity entity = new HttpEntity(headers);

			System.out.println(url);
			ResponseEntity<Response> response = rest.exchange(url, HttpMethod.GET, entity, Response.class);
			//ResponseEntity<Response> response = rest.getForEntity(url, Response.class);

			results.addAll(parseResponse(response.getBody()));
		}
		return results;

	}
	
	private List<Result> parseResponse(Response response){
		
		List<Result> results = new ArrayList<Result>();
		
		for(Center center : response.getCenters()) {
			
			if ((this.goodCenters == null) || (this.goodCenters.contains(center))){
				
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

		}
		
		return results;
	}
}
