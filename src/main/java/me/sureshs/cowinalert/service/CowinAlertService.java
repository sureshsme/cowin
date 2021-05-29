package me.sureshs.cowinalert.service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
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
import me.sureshs.cowinalert.model.input.Input;
import me.sureshs.cowinalert.model.result.Result;

import static java.text.MessageFormat.format;
@Service
@Slf4j
public class CowinAlertService {

	@Value("${date:#{null}}")
	private String date;

	@Value("${interval:#{6}}")
	private int interval;

	@Value("${fileName:data.csv}")
	private String fileName;
	
	@Value("${threshold:#{1}}")
	private int threshold;
	
	@Value("${url}")
	private String url;

	@Value("${telegram.url.test}")
	private String telegramUrlTest;

	@Value("${telegram.chatId.admin}")
	private String telegramAdmin;

	@Value("#{${districts.map}}")
	private Map<String, String> districtsMap;

	@Value("${telegram.url}")
	private String telegramUrl;

	@Autowired
	private CommonService common;
	
	public void execute() {
		 
		List<String> lines = common.readFile(this.fileName);
		
		List<Input> users = serialize(lines);
		
		Set<String> districts = users.stream().map(Input::getDistrict_id).collect(Collectors.toSet());
		List<String> dates = getDatesToCheck(this.date, this.interval);
		
		Map<String, Set<Result>> resultsMap = getResultMap(districts, dates);

		resultsMap.keySet().forEach(k -> {

			Set<Result> results = resultsMap.get(k);
			
			log.info("found {} locations @ {} with at least {} slot(s)", results.size(), this.districtsMap.get(k), this.threshold);
			
			if (!results.isEmpty()) {
				results.forEach(r -> log.info(r.toString()));
			}
		});

		users.forEach(u ->{
			
			Set<Result> resultsFilteredByAgeLimit = filterResultsByAgeLimit(resultsMap.get(u.getDistrict_id()), u.getMin_age_limit());

			Set<Result> resultsFilteredByVaccine = filterResultsByVaccine(resultsFilteredByAgeLimit, u.getVaccine());

			Set<Result> results = filterResultsByDose(resultsFilteredByVaccine, u.getDose());

			if (!results.isEmpty()) {
				
				String summary = u.getName() + ","
						+ "\nfound {0} location @ {1} for :"
						+ "\n - age: {2}"
						+ "\n - {3}"
						+ "\n - dose: {4}";
				
				summary = format(summary, results.size(), this.districtsMap.get(u.getDistrict_id()), u.getMin_age_limit(), u.getVaccine(), u.getDose());
				
				log.info("----------------------------------------------------------------------------------------");
				log.info("sending telegram message :: {}", summary);
				
				sendTelegramMessage(this.telegramUrl, u.getChat_id(), formatSummary(results, summary));
			}
		});
	}


	private Set<Result> filterResultsByAgeLimit(Set<Result> results, Integer min_age_limit) {
		Set<Result> filteredResults = new LinkedHashSet<Result>();
		
		for (Result result : results) {
			if (min_age_limit == result.getMin_age_limit()) filteredResults.add(result);
		}

		
		return filteredResults;
	}

	private Set<Result> filterResultsByVaccine(Set<Result> results, String vaccine) {
		Set<Result> filteredResults = new LinkedHashSet<Result>();

		if (vaccine.equalsIgnoreCase("any vaccine")) return results;
		
		for (Result result : results) {
			if (vaccine.equalsIgnoreCase(result.getVaccine())) filteredResults.add(result);
		}
		
		return filteredResults;
	}

	private Set<Result> filterResultsByDose(Set<Result> results, Integer dose) {
		Set<Result> filteredResults = new LinkedHashSet<Result>();
		
		for (Result result : results) {
			if (dose == 1 && result.getAvailable_capacity_dose1() > 0) filteredResults.add(result);
			if (dose == 2 && result.getAvailable_capacity_dose2() > 0) filteredResults.add(result);
		}
		
		return filteredResults;
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
			
			resultsMap.put(district, new LinkedHashSet<Result>(results));
			
		}
		
		return resultsMap;

	}

	public void sendTelegramMessage(String url, String chatId, String text) {

		RestTemplate rest = new RestTemplate();

		url += "?".concat("chat_id=").concat(chatId).concat("&text=").concat(text);

		rest.getForObject(url, String.class);
		

	}

	private List<Input> serialize(List<String> lines) {
		
		List<Input> users = new ArrayList<Input>();
		
		lines.forEach(l -> {
			String[] line = l.split(",");
			
			if (!line[6].startsWith("Y")) return;
				
			users.add(Input.builder()
					.chat_id(line[0])
					.name(line[1])
					.district_id(line[2])
					.dose(Integer.parseInt(line[3]))
					.min_age_limit(Integer.parseInt(line[4]))
					.vaccine(line[5])
					.build());
			
		});
		
		users.forEach(u -> System.out.println(u));
		
		return users;
	}

	private List<String> getDatesToCheck(String date, int interval) {

		List<String> dates = new ArrayList<String>();

		DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern("dd-MM-yyyy");
		LocalDate startDate = date != null? LocalDate.parse(date, dateFormat) : LocalDate.now(); 
		
		
		dates.add(startDate.format(dateFormat));
		dates.add(startDate.plusDays(interval).format(dateFormat));

		return dates;
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
								.min_age_limit(session.getMin_age_limit())
								.available_capacity_dose1(session.getAvailable_capacity_dose1())
								.available_capacity_dose2(session.getAvailable_capacity_dose2())
								.build());
					}
				}

		}
		
		return results;
	}

	private String formatSummary(Set<Result> results, String summary) {
		
		StringBuilder msg = new StringBuilder();
		msg.append(summary).append("\n\n");
		results.forEach(r -> msg.append("- ").append(r).append("\n"));
		
		String textToSend = msg.toString();
		
		 if (textToSend.length() > 4000)
			 textToSend = textToSend.substring(0, 4000).concat("...");

		return textToSend;
	}


	//@Override
	public void run(String... args) throws Exception {
		log.info("----------------------------------------------------------------------------------------");
		execute();
		
	}
}
