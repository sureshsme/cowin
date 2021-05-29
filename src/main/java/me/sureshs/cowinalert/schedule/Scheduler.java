package me.sureshs.cowinalert.schedule;

import java.util.HashSet;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;
import me.sureshs.cowinalert.model.result.Result;
import me.sureshs.cowinalert.service.CowinAlertService;
import me.sureshs.cowinalert.service.CowinService;

@Component
@Slf4j
public class Scheduler {


	@Autowired private CowinAlertService service;
	
	@Value("${telegram.url.test}")
	private String telegramUrlTest;

	@Value("${telegram.chatId.admin}")
	private String telegramAdmin;

	
	@Scheduled(cron = "${cron}") //every 30th second = "*/30 * * * * *" 
	public void execute() {

		log.info("----------------------------------------------------------------------------------------");
		service.execute();

	}
	
	
	@Scheduled(cron = "${cron.test}")  //every hour = 0 0 * * * *
	public void test() {

		log.info(":: test :: ");
		service.sendTelegramMessage(this.telegramUrlTest, this.telegramAdmin, "Hello there!");

	}

	
	@Scheduled(cron = "* 44 16 * * *")  //every hour = 0 0 * * * *
	public void goodMorning() {

		log.info(":: test :: ");
		service.sendTelegramMessage(this.telegramUrlTest, this.telegramAdmin, "Hello there!");

	}

}
