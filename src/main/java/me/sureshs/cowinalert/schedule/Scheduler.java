package me.sureshs.cowinalert.schedule;

import java.util.HashSet;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;
import me.sureshs.cowinalert.model.result.Result;
import me.sureshs.cowinalert.service.CowinService;

@Component
@Slf4j
public class Scheduler {


	@Autowired private CowinService service;
	
	@Scheduled(cron = "${cron}") //every 30th second = "*/30 * * * * *" 
	public void execute() {

		log.info("----------------------------------------------------------------------------------------");
		service.getResponseForDistrict();

	}
	
	
	@Scheduled(cron = "${cron.test}")  //every hour = 0 0 * * * *
	public void test() {

		log.info(":: test :: ");
		service.sendMessage(new HashSet<Result>(), "", true);

	}
	
}
